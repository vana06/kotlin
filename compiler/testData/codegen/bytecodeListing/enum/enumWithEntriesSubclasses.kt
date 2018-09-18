enum class Foo {
    A,
    B {
        fun foo() {}
    }
    ,
    C {
        fun foo() {}
    }
}

// method: Foo::<init>
// jvm signature:     (Ljava/lang/String;ILFoo$B;)V
// generic signature: null
