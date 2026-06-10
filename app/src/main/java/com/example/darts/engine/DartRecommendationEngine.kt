package com.example.darts.engine

data class DartTarget(
    val name: String,
    val score: Int,
    val isDouble: Boolean = false,
    val isTriple: Boolean = false,
    val angle: Float = 0f
)

data class CheckoutPath(
    val darts: List<DartTarget>,
    val remaining: Int,
    val finished: Boolean,
    val score: Int
)

object DartRecommendationEngine {

    private val preferredDoubles = mapOf(
        20 to 100,
        16 to 95,
        12 to 90,
        18 to 85,
        8 to 80,
        4 to 75,
        10 to 70,
        2 to 65
    )

    private val allTargets: List<DartTarget> = buildList {

        add(
            DartTarget(
                name = "BULL",
                score = 50,
                angle = 0f
            )
        )

        add(
            DartTarget(
                name = "OUTER_BULL",
                score = 25,
                angle = 0f
            )
        )

        for (i in 1..20) {

            add(
                DartTarget(
                    name = "S$i",
                    score = i,
                    angle = getAngleForSector(i)
                )
            )

            add(
                DartTarget(
                    name = "D$i",
                    score = i * 2,
                    isDouble = true,
                    angle = getAngleForSector(i)
                )
            )

            add(
                DartTarget(
                    name = "T$i",
                    score = i * 3,
                    isTriple = true,
                    angle = getAngleForSector(i)
                )
            )
        }
    }

    /**
     * Main entry point.
     *
     * Returns all possible checkout paths ranked from best to worst.
     */
    fun getCheckoutOptions(remainingScore: Int): List<CheckoutPath> {

        if (remainingScore <= 1) {
            return emptyList()
        }

        if (remainingScore > 180) {
            return listOf(
                CheckoutPath(
                    darts = listOf(
                        DartTarget(
                            name = "T20",
                            score = 60,
                            isTriple = true,
                            angle = getAngleForSector(20)
                        )
                    ),
                    remaining = remainingScore - 60,
                    finished = false,
                    score = 0
                )
            )
        }

        val results = mutableListOf<CheckoutPath>()

        search(
            remaining = remainingScore,
            dartsLeft = 3,
            path = mutableListOf(),
            results = results
        )

        return results
            .map {
                it.copy(score = evaluate(it))
            }
            .sortedByDescending { it.score }
    }

    /**
     * Returns only the highest-ranked checkout path.
     */
    fun getBestCheckout(remainingScore: Int): CheckoutPath? {
        return getCheckoutOptions(remainingScore).firstOrNull()
    }

    private fun search(
        remaining: Int,
        dartsLeft: Int,
        path: MutableList<DartTarget>,
        results: MutableList<CheckoutPath>
    ) {

        if (remaining == 0) {

            val checkout = CheckoutPath(
                darts = path.toList(),
                remaining = 0,
                finished = true,
                score = 0
            )

            results.add(checkout)
            return
        }

        if (remaining < 0) return
        if (dartsLeft == 0) return

        for (target in allTargets) {

            val newRemaining = remaining - target.score

            if (newRemaining < 0)
                continue

            /*
             * Checkout must finish on a double.
             * Bull counts as a double-25.
             */
            if (newRemaining == 0) {

                val legalFinish =
                    target.isDouble ||
                            target.name == "BULL"

                if (!legalFinish)
                    continue
            }

            path.add(target)

            search(
                remaining = newRemaining,
                dartsLeft = dartsLeft - 1,
                path = path,
                results = results
            )

            path.removeAt(path.lastIndex)
        }
    }

    /**
     * Ranking heuristic.
     *
     * Higher score = better recommendation.
     */
    private fun evaluate(path: CheckoutPath): Int {

        if (!path.finished)
            return 0

        var score = 0

        // Prefer fewer darts.
        score += (4 - path.darts.size) * 1000

        val lastDart = path.darts.last()

        when {
            lastDart.name == "BULL" -> {
                score += 98
            }

            lastDart.isDouble -> {

                val doubleNumber =
                    lastDart.score / 2

                score += preferredDoubles[doubleNumber] ?: 20
            }
        }

        // Slight preference for traditional routes.
        score += path.darts.count { it.isTriple } * 5

        return score
    }

    private fun getAngleForSector(sector: Int): Float {

        val clockwiseLayoutSectors = listOf(
            20, 1, 18, 4, 13,
            6, 10, 15, 2, 17,
            3, 19, 7, 16, 8,
            11, 14, 9, 12, 5
        )

        val index = clockwiseLayoutSectors.indexOf(sector)

        if (index == -1)
            return -90f

        return (index * 18f) - 90f
    }
}