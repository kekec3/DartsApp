package com.example.darts.engine

enum class Multiplier(val mul:Int) {
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3)
}

data class DartThrow(
    val value:Int,
    val multiplier: Multiplier
) {
    fun score(): Int {
        return value * multiplier.mul
    }

    fun isDouble(): Boolean {
        return value == 25 || multiplier == Multiplier.DOUBLE
    }

    fun isTriple(): Boolean {
        return (value == 25 && multiplier == Multiplier.DOUBLE) || multiplier == Multiplier.TRIPLE
    }

    fun isMiss(): Boolean {
        return value == 0
    }

    fun displayString(): String = when {
        isMiss() -> "Miss"
        value == 25 && multiplier == Multiplier.DOUBLE -> "Bull"
        value == 25 -> "Outer"
        multiplier == Multiplier.DOUBLE -> "D$value"
        multiplier == Multiplier.TRIPLE -> "T$value"
        else -> "$value"
    }

    fun shortDisplay(): String = when {
        isMiss() -> "0"
        multiplier == Multiplier.DOUBLE -> "D$value"
        multiplier == Multiplier.TRIPLE -> "T$value"
        else -> "$value"
    }
}
