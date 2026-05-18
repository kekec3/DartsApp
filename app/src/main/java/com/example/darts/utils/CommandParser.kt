package com.example.darts.utils

import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import kotlin.math.max

class CommandParser {

    sealed class VoiceCommand {
        data class Throw(val dart: DartThrow) : VoiceCommand()
        object Undo   : VoiceCommand()
        object Submit : VoiceCommand()
        object Unknown : VoiceCommand()
    }

    // Top-level entry: try each candidate until one parses
    fun parse(candidates: List<String>): VoiceCommand {
        for (raw in candidates) {
            val cmd = tryParse(raw.lowercase().trim())
            if (cmd !is VoiceCommand.Unknown) return cmd
        }
        return VoiceCommand.Unknown
    }

    private fun tryParse(text: String): VoiceCommand {
        // ── Control commands ──────────────────────────────────────────────────
        if (fuzzyMatch(text, UNDO_WORDS, threshold = 0.7f))   return VoiceCommand.Undo
        if (fuzzyMatch(text, SUBMIT_WORDS, threshold = 0.7f)) return VoiceCommand.Submit

        // ── Miss ──────────────────────────────────────────────────────────────
        if (fuzzyMatch(text, MISS_WORDS, threshold = 0.7f))
            return VoiceCommand.Throw(DartThrow(0, Multiplier.SINGLE))

        // ── Bullseye variants ─────────────────────────────────────────────────
        if (fuzzyMatch(text, SINGLE_BULL_WORDS, threshold = 0.65f))
            return VoiceCommand.Throw(DartThrow(25, Multiplier.SINGLE))
        if (fuzzyMatch(text, DOUBLE_BULL_WORDS, threshold = 0.65f))
            return VoiceCommand.Throw(DartThrow(25, Multiplier.DOUBLE))

        // ── Multiplier prefix + number ────────────────────────────────────────
        val multiplier = when {
            anyFuzzyPrefix(text, TRIPLE_PREFIXES, threshold = 0.7f) -> Multiplier.TRIPLE
            anyFuzzyPrefix(text, DOUBLE_PREFIXES, threshold = 0.7f) -> Multiplier.DOUBLE
            else                                                     -> Multiplier.SINGLE
        }

        val stripped = removePrefixes(text, threshold = 0.7f)

        val number = parseNumber(stripped)
        if (number != null && number in 1..20) {
            return VoiceCommand.Throw(DartThrow(number, multiplier))
        }

        return VoiceCommand.Unknown
    }

    /**
     * Levenshtein distance: measures how different two strings are.
     * 0 = identical, 1 = completely different.
     * Threshold 0.7 = allows ~30% character differences (good for accents)
     */
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        val dist = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1f
        return 1f - (dist.toFloat() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = when {
                    s1[i - 1] == s2[j - 1] -> dp[i - 1][j - 1]
                    else -> 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Check if text matches ANY word in the list (fuzzy).
     * Returns true if any word in the list has similarity >= threshold.
     */
    private fun fuzzyMatch(text: String, words: Set<String>, threshold: Float = 0.7f): Boolean {
        return words.any { word ->
            levenshteinSimilarity(text, word) >= threshold ||
                    text.contains(word)  // Also accept exact substring match
        }
    }

    /**
     * Check if text starts with ANY prefix in the list (fuzzy).
     */
    private fun anyFuzzyPrefix(text: String, prefixes: Set<String>, threshold: Float = 0.7f): Boolean {
        return prefixes.any { prefix ->
            text.startsWith(prefix) ||  // Exact match
                    (text.length >= prefix.length &&
                            levenshteinSimilarity(text.substring(0, prefix.length), prefix) >= threshold)
        }
    }

    /**
     * Remove multiplier prefixes from text (fuzzy).
     */
    private fun removePrefixes(text: String, threshold: Float = 0.7f): String {
        var result = text
        val allPrefixes = (TRIPLE_PREFIXES + DOUBLE_PREFIXES + SINGLE_PREFIXES).sortedByDescending { it.length }

        // 1. Try EXACT match first (very fast, highly accurate with the expanded lists)
        for (prefix in allPrefixes) {
            if (result.startsWith(prefix)) {
                return result.removePrefix(prefix).trim()
            }
        }

        // 2. Fallback to Fuzzy matching
        for (prefix in allPrefixes) {
            if (result.length >= prefix.length) {
                val candidate = result.substring(0, prefix.length)
                if (levenshteinSimilarity(candidate, prefix) >= 0.8f) { // Strict 0.8f threshold is good here
                    return result.substring(prefix.length).trim()
                }
            }
        }
        return result
    }

    /**
     * Try to parse a number from text, handling words and digits.
     */
    private fun parseNumber(text: String): Int? {
        // Direct word match
        WORD_TO_NUM[text]?.let { return it }

        // Digit parsing
        text.toIntOrNull()?.let { num ->
            if (num in 1..20) return num
        }

        // Fuzzy word match: find closest number word
        val bestMatch = WORD_TO_NUM.keys.maxByOrNull { word ->
            levenshteinSimilarity(text, word)
        }
        if (bestMatch != null) {
            val similarity = levenshteinSimilarity(text, bestMatch)
            if (similarity >= 0.65f) {  // Allow ~35% difference for accents
                return WORD_TO_NUM[bestMatch]
            }
        }

        return null
    }

    // ── Word lists ────────────────────────────────────────────────────────────
    private val UNDO_WORDS   = setOf(
        "undo", "back", "cancel", "delete", "remove", "oops", "take back",
        "undo that", "take that back"
    )

    private val SUBMIT_WORDS = setOf(
        "submit", "confirm", "done", "next", "finished", "end turn", "that's it",
        "go", "ready", "ok", "check"
    )

    private val MISS_WORDS   = setOf(
        "miss", "missed", "zero", "no score", "out", "blank", "nothing",
        "no points", "zilch"
    )
    private val SINGLE_BULL_WORDS = setOf("outer", "single bull", "twenty five", "twenty-five", "25")
    private val DOUBLE_BULL_WORDS = setOf("bull", "bullseye", "inner", "fifty", "bull's-eye", "50")
    private val TRIPLE_PREFIXES = setOf(
        "triple  ", "treble  ", "trip  ", "t  ", // Added space after 't'
        "tripl  ", "tree-pull  ", "cripple  "
    )
    private val DOUBLE_PREFIXES = setOf(
        "double  ", "dub  ", "d  ",  // Added space after 'd'
        "dabl  ", "dabal  ", "bubble  ", "trouble  "
    )
    private val SINGLE_PREFIXES = setOf(
        "single ", "s " // Added space after 's'
    )


    private val WORD_TO_NUM = mapOf(
        // Standard English
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19, "twenty" to 20,

        // THE ACCENT DICTIONARY
        // The "th" -> "t" / "d" / "f" shift
        "tree" to 3, "free" to 3, "dree" to 3, "dirty" to 3, // engine sometimes hears 'thirty' for 'three'
        "turtin" to 13, "tartin" to 13, "tirting" to 13, "thirting" to 13, "darting" to 13,

        // The "w" -> "v" shift
        "tventy" to 20, "twenty" to 20, "venti" to 20, "twinty" to 20,
        "tvelve" to 12, "valve" to 12, "twelv" to 12, "delve" to 12, "dwell" to 12,

        // Harsh/Rolled "R" and sharp vowels
        "for" to 4, "foar" to 4, "fow" to 4,
        "faiv" to 5, "fife" to 5, "five" to 5,
        "siks" to 6, "seeks" to 6,
        "najn" to 9, "nine-t" to 9,
        "ejt" to 8, "ate" to 8, "ait" to 8,

        // Digit strings (Keep these!)
        "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10,
        "11" to 11,"12" to 12,"13" to 13,"14" to 14,"15" to 15,
        "16" to 16,"17" to 17,"18" to 18,"19" to 19,"20" to 20,

        // 1
        "van" to 1, "von" to 1, "on" to 1, "juan" to 1, "one" to 1,
        // 2
        "to" to 2, "too" to 2, "tu" to 2, "do" to 2, "two" to 2,
        // 3
        "tree" to 3, "free" to 3, "dree" to 3, "three" to 3,
        // 4
        "for" to 4, "fo" to 4, "far" to 4, "four" to 4,
        // 5
        "fajv" to 5, "fiv" to 5, "wife" to 5, "vibe" to 5, "five" to 5,
        // 6
        "seeks" to 6, "siks" to 6, "sex" to 6, "sick" to 6, "six" to 6,
        // 7
        "sevn" to 7, "saven" to 7, "stephen" to 7, "seven" to 7,
        // 8
        "ejt" to 8, "ate" to 8, "it" to 8, "hate" to 8, "hey" to 8, "eight" to 8,
        // 9
        "najn" to 9, "nan" to 9, "nein" to 9, "mine" to 9, "line" to 9, "nine" to 9,
        // 10
        "tan" to 10, "then" to 10, "dan" to 10, "pen" to 10, "den" to 10, "ten" to 10,
    )
}