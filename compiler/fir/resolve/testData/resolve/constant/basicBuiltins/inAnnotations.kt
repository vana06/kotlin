@Target(AnnotationTarget.FIELD)
annotation class SomeAnnotation(val toCompute: Int)

@SomeAnnotation(1 + 2)
val temp = 1

@SomeAnnotation(1 + temp)
const val temp2 = 2

@SomeAnnotation(1 + temp2)
val temp3 = 3