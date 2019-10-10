val withoutConst = 1
const val withConst = 3 + 2 + 1

const val first = 1.toByte()
const val second = "1".toByte()
val computeLength = "1".length
const val computeLength2 = "1".length

@Target(AnnotationTarget.FIELD)
annotation class SomeAnnotation(val toCompute: Int)

@SomeAnnotation(1 + 1 + 2)
val withAnnotation = 1

//пока что не поддерживаем
//const val a = if (1 + 1 == 2) 1 else 2