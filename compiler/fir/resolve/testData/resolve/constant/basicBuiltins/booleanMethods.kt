//unary methods
const val notTrue = true.not()
const val notFalse = false.not()
const val trueAsString = true.toString()
const val FalseAsString = false.toString()

//binary methods
const val and = 1.equals(2 - 1) && true
const val or = (4 % 2).equals(0) || false
const val compareTo1 = and.compareTo(or)
const val compareTo2 = or.compareTo(and)
const val equals = compareTo1.equals(compareTo2)
