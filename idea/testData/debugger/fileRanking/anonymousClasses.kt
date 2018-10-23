//FILE: a/a.kt
package a

interface R {
    fun run()
}

fun eval(r: R) {
    r.run()
}

fun foo() {
    eval(object : R {
        override fun run() {
            val a = 1
        }
    })
}

//FILE: b/a.kt
package b

import a.R
import a.eval

fun foo() {
    eval(object : R {
        override fun run() {
            val b = 1
        }
    })
}