//                        fun TODO(): Nothing
//                        │
fun <T> genericFoo(): T = TODO()

//        T                  fun <T> genericFoo(): T
//        │                  │           
val <T> T.generic: T get() = genericFoo()
