/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilderImpl
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.proto.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.builtins.compileTimeAnnotationName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnonymousInit as ProtoAnonymousInit
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructor as ProtoConstructor
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase

class JvmIrLinker(currentModule: ModuleDescriptor?, logger: LoggingContext, builtIns: IrBuiltIns, symbolTable: SymbolTable, override val functionalInteraceFactory: IrAbstractFunctionFactory, private val stubGenerator: DeclarationStubGenerator, private val manglerDesc: JvmManglerDesc) :
    KotlinIrLinker(currentModule, logger, builtIns, symbolTable, emptyList()) {

    var deserializerForCompileTime: KlibModuleDeserializer? = null

    override val fakeOverrideBuilderImpl = FakeOverrideBuilderImpl(symbolTable, IdSignatureSerializer(JvmManglerIr), builtIns)

    private val javaName = Name.identifier("java")

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor.name.asString().startsWith("<dependencies of ")

    // TODO: implement special Java deserializer
    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary?, strategy: DeserializationStrategy): IrModuleDeserializer {
        if (klib != null) {
            assert(moduleDescriptor.getCapability(KlibModuleOrigin.CAPABILITY) != null)
            return JvmModuleDeserializer(moduleDescriptor, klib, strategy)
        }

        return MetadataJVMModuleDeserializer(moduleDescriptor, emptyList())
    }

    fun addDeserializerForCompileTimeDeclarations(moduleDescriptor: ModuleDescriptor, klib: IrLibrary) {
        deserializerForCompileTime = KlibModuleDeserializer(moduleDescriptor, klib).apply { init() }
    }

    private inner class JvmModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary, strategy: DeserializationStrategy) :
        KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, klib, strategy)

    inner class KlibModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary) :
        KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, klib, DeserializationStrategy.ONLY_REFERENCED) {

        override fun createIrDeserializerForFile(
            fileProto: IrFile, fileIndex: Int, moduleDeserializer: IrModuleDeserializer
        ): IrDeserializerForFile {
            return IrDeserializerForCompileTime(fileProto.annotationList, fileProto.actualsList, fileIndex, moduleDeserializer)
        }

        fun declare(irSymbol: IrSymbol) {
            val idSig = irSymbol.signature
            assert(idSig.isPublic)

            val fileDeserializer = scheduleTopLevelSignatureDeserialization(idSig)

            if (idSig != idSig.topLevelSignature()) {
                super.declareIrSymbol(irSymbol)
            } else {
                val symbol = fileDeserializer.fileLocalDeserializationState.deserializedSymbols[idSig]
                if (symbol == null) {
                    fileDeserializer.deserializeDeclaration(idSig)
                }
            }
        }

        fun getBodies(): Map<IdSignature, IrBody> {
            return mutableMapOf<IdSignature, IrBody>().apply {
                fileToDeserializerMap.forEach { putAll((it.value as IrDeserializerForCompileTime).bodies) }
            }
        }

        inner class IrDeserializerForCompileTime(
            annotations: List<ProtoConstructorCall>?, actuals: List<Actual>, fileIndex: Int, moduleDeserializer: IrModuleDeserializer
        ) : KotlinIrLinker.IrDeserializerForFile(annotations, actuals, fileIndex, false, true, false, moduleDeserializer) {
            val bodies = mutableMapOf<IdSignature, IrBody>()

            override fun deserializeIrValueParameter(proto: ProtoValueParameter, index: Int): IrValueParameterImpl {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) {
                    return (symbol.owner as IrValueParameterImpl).apply {
                        if (proto.hasDefaultValue()) defaultValue = IrExpressionBodyImpl(deserializeExpressionBody(proto.defaultValue))
                    }
                }

                return super.deserializeIrValueParameter(proto, index)
            }

            override fun deserializeIrClass(proto: ProtoClass): IrClass {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)
                if (symbol.isBound) {
                    return (symbol.owner as IrClass).apply {
                        usingParent {
                            typeParameters = deserializeTypeParameters(proto.typeParameterList, true)
                            proto.declarationList.forEach { deserializeDeclaration(it) }
                        }
                    }
                }

                return super.deserializeIrClass(proto)
            }

            private fun deserializeFunctionBase(symbol: IrSymbol, proto: ProtoFunctionBase): IrFunction {
                val irFunction = symbol.owner as IrFunction
                try {
                    recordDelegatedSymbol(symbol)
                    val result = symbolTable.withScope(symbol.descriptor) {
                        irFunction.deserializeIrFunctionBase(proto)
                    }
                    result.body?.let { bodies[symbol.signature] = it }
                    result.annotations += deserializeAnnotations(proto.base.annotationList)
                    return result
                } finally {
                    eraseDelegatedSymbol(symbol)
                }
            }

            override fun deserializeIrFunction(proto: ProtoFunction): IrSimpleFunction {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.base.symbol)

                if (symbol.isBound) {
                    return deserializeFunctionBase(symbol, proto.base) as IrSimpleFunction
                }

                return super.deserializeIrFunction(proto)
            }

            override fun deserializeIrConstructor(proto: ProtoConstructor): IrConstructor {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.base.symbol)

                if (symbol.isBound) {
                    return deserializeFunctionBase(symbol, proto.base) as IrConstructor
                }

                return super.deserializeIrConstructor(proto)
            }

            override fun deserializeIrVariable(proto: ProtoVariable): IrVariableImpl {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) return (symbol.owner as IrVariableImpl).apply {
                    if (proto.hasInitializer()) initializer = deserializeExpression(proto.initializer)
                }

                return super.deserializeIrVariable(proto)
            }

            override fun deserializeIrEnumEntry(proto: ProtoEnumEntry): IrEnumEntry {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) return (symbol.owner as IrEnumEntry).apply {
                    if (proto.hasCorrespondingClass()) correspondingClass = deserializeIrClass(proto.correspondingClass)
                    if (proto.hasInitializer()) initializerExpression = IrExpressionBodyImpl(deserializeExpressionBody(proto.initializer))
                }

                return super.deserializeIrEnumEntry(proto)
            }

            override fun deserializeIrAnonymousInit(proto: ProtoAnonymousInit): IrAnonymousInitializerImpl {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) {
                    return symbol.owner.apply { deserializeStatementBody(proto.body) } as IrAnonymousInitializerImpl
                }

                return super.deserializeIrAnonymousInit(proto)
            }

            override fun deserializeIrField(proto: ProtoField): IrField {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) return (symbol.owner as IrField).apply {
                    if (proto.hasInitializer()) initializer = IrExpressionBodyImpl(deserializeExpressionBody(proto.initializer))
                }

                return super.deserializeIrField(proto)
            }

            override fun deserializeIrProperty(proto: ProtoProperty): IrProperty {
                val (symbol, _) = deserializeIrSymbolToDeclare(proto.base.symbol)

                if (symbol.isBound) return (symbol.owner as IrProperty).apply {
                    if (proto.hasGetter()) {
                        getter = deserializeIrFunction(proto.getter).also {
                            it.correspondingPropertySymbol = symbol as IrPropertySymbol
                        }
                    }
                    if (proto.hasSetter()) {
                        setter = deserializeIrFunction(proto.setter).also {
                            it.correspondingPropertySymbol = symbol as IrPropertySymbol
                        }
                    }
                    if (proto.hasBackingField()) {
                        backingField = deserializeIrField(proto.backingField)
                    }
                }

                return super.deserializeIrProperty(proto)
            }
        }
    }

    private fun DeclarationDescriptor.isJavaDescriptor(): Boolean {
        if (this is PackageFragmentDescriptor) {
            return this is LazyJavaPackageFragment || fqName.startsWith(javaName)
        }

        return this is JavaClassDescriptor || this is JavaCallableMemberDescriptor || (containingDeclaration?.isJavaDescriptor() == true)
    }

    private fun DeclarationDescriptor.isCleanDescriptor(): Boolean {
        if (this is PropertyAccessorDescriptor) return correspondingProperty.isCleanDescriptor()
        return this is DeserializedDescriptor
    }

    override fun platformSpecificSymbol(symbol: IrSymbol): Boolean {
        return symbol.descriptor.isJavaDescriptor()
    }

    private fun declareJavaFieldStub(symbol: IrFieldSymbol): IrField {
        return with(stubGenerator) {
            val old = stubGenerator.unboundSymbolGeneration
            try {
                stubGenerator.unboundSymbolGeneration = true
                generateFieldStub(symbol.descriptor)
            } finally {
                stubGenerator.unboundSymbolGeneration = old
            }
        }
    }


    override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer =
        JvmCurrentModuleDeserializer(moduleFragment, dependencies)

    private inner class JvmCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>) :
        CurrentModuleDeserializer(moduleFragment, dependencies) {
        override fun declareIrSymbol(symbol: IrSymbol) {
            val descriptor = symbol.descriptor

            if (descriptor.isJavaDescriptor()) {
                // Wrap java declaration with lazy ir
                if (symbol is IrFieldSymbol) {
                    declareJavaFieldStub(symbol)
                } else {
                    stubGenerator.generateMemberStub(descriptor)
                }
                return
            }

            if (descriptor.isCleanDescriptor()) {
                stubGenerator.generateMemberStub(descriptor)
                return
            }

            super.declareIrSymbol(symbol)
        }
    }

    private inner class MetadataJVMModuleDeserializer(moduleDescriptor: ModuleDescriptor, dependencies: List<IrModuleDeserializer>) :
        IrModuleDeserializer(moduleDescriptor) {

        // TODO: implement proper check whether `idSig` belongs to this module
        override fun contains(idSig: IdSignature): Boolean = true

        private val descriptorFinder = DescriptorByIdSignatureFinder(
            moduleDescriptor, manglerDesc,
            DescriptorByIdSignatureFinder.LookupMode.MODULE_ONLY
        )

        private fun resolveDescriptor(idSig: IdSignature): DeclarationDescriptor {
            return descriptorFinder.findDescriptorBySignature(idSig) ?: error("No descriptor found for $idSig")
        }

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            val descriptor = resolveDescriptor(idSig)

            val declaration = stubGenerator.run {
                when (symbolKind) {
                    BinarySymbolData.SymbolKind.CLASS_SYMBOL -> generateClassStub(descriptor as ClassDescriptor)
                    BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> generatePropertyStub(descriptor as PropertyDescriptor)
                    BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> generateFunctionStub(descriptor as FunctionDescriptor)
                    BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> generateConstructorStub(descriptor as ClassConstructorDescriptor)
                    BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> generateEnumEntryStub(descriptor as ClassDescriptor)
                    BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> generateTypeAliasStub(descriptor as TypeAliasDescriptor)
                    else -> error("Unexpected type $symbolKind for sig $idSig")
                }
            }

            return declaration.symbol
        }

        override fun declareIrSymbol(symbol: IrSymbol) {
            assert(symbol.isPublicApi || symbol.descriptor.isJavaDescriptor())
            if (symbol is IrFieldSymbol) {
                declareJavaFieldStub(symbol)
            } else {
                stubGenerator.generateMemberStub(symbol.descriptor)
                if (symbol.descriptor.annotations.hasAnnotation(compileTimeAnnotationName)) {
                    deserializerForCompileTime?.declare(symbol)
                }
            }
        }

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns, emptyList())
        override val moduleDependencies: Collection<IrModuleDeserializer> = dependencies

    }
}