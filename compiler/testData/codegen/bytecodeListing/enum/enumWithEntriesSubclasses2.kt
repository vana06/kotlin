enum class Foo(val s: String) {
    A("A"),
    B("B") {
        fun foo() {}
    }
    ,
    C("C") {
        fun foo() {}
    }
}

// method: Foo::<init>
// jvm signature:     (Ljava/lang/String;ILjava/lang/String;LFoo$B;)V
// generic signature: null

// method: Foo::<init>
// jvm signature:     (Ljava/lang/String;ILjava/lang/String;)V
// generic signature: null
