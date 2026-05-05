package com.example.darts.engine

data class Turn(
    val darts: List<DartThrow> = emptyList()
) {
    fun totalScore() : Int {
        return darts.sumOf { it.score() }
    }
}
