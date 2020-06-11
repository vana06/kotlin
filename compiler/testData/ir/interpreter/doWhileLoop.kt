// !LANGUAGE: +CompileTimeCalculations

@CompileTimeCalculation
fun factorialDoWhile(num: Int): Int {
    var number = num
    var factorial = 1

    do {
        factorial *= number
        number--
    } while (number > 0)

    return factorial
}

@CompileTimeCalculation
fun firstNotNull(array: Array<Int?>): Int {
    var i = 0

    do {
        val y = array[i++]
    } while (y == null)

    return array[i - 1]!!
}

@CompileTimeCalculation
fun singleExpressionLoop(incrementTo: Int): Int {
    var i = 0
    do i++ while (i < incrementTo)
    return i
}

const val a = factorialDoWhile(6)
const val b = firstNotNull(arrayOf<Int?>(null, null, 1, 2, null))
const val c = singleExpressionLoop(10)