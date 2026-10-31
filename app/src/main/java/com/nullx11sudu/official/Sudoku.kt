package com.nullx11sudu.official

/** Stateless solver. Inputs are never modified; an ambiguous scan is not presented as certain. */
object Sudoku {
    sealed class Result {
        data class Unique(val board: IntArray) : Result()
        data object Ambiguous : Result()
        data object Invalid : Result()
    }

    fun solve(input: IntArray): IntArray? = search(input, 1).first

    fun analyze(input: IntArray): Result {
        val (solution, count) = search(input, 2)
        return when (count) {
            0 -> Result.Invalid
            1 -> Result.Unique(solution!!)
            else -> Result.Ambiguous
        }
    }

    private fun search(input: IntArray, limit: Int): Pair<IntArray?, Int> {
        if (input.size != 81 || input.any { it !in 0..9 }) return null to 0
        val board = input.clone()
        val rows = IntArray(9); val cols = IntArray(9); val boxes = IntArray(9)
        fun box(i: Int) = (i / 27) * 3 + (i % 9) / 3
        for (i in board.indices) {
            val v = board[i]
            if (v == 0) continue
            val r = i / 9; val c = i % 9; val b = box(i); val bit = 1 shl v
            if ((rows[r] or cols[c] or boxes[b]) and bit != 0) return null to 0
            rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
        }
        var first: IntArray? = null
        var count = 0
        fun recurse() {
            if (count >= limit) return
            var chosen = -1; var choices = 0; var minimum = 10
            for (i in board.indices) if (board[i] == 0) {
                val mask = 0x3FE and (rows[i / 9] or cols[i % 9] or boxes[box(i)]).inv()
                val possible = Integer.bitCount(mask)
                if (possible == 0) return
                if (possible < minimum) { minimum = possible; chosen = i; choices = mask }
            }
            if (chosen < 0) { if (first == null) first = board.clone(); count++; return }
            val r = chosen / 9; val c = chosen % 9; val b = box(chosen)
            for (v in 1..9) {
                val bit = 1 shl v
                if (choices and bit == 0) continue
                board[chosen] = v; rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
                recurse()
                board[chosen] = 0; rows[r] = rows[r] xor bit; cols[c] = cols[c] xor bit; boxes[b] = boxes[b] xor bit
                if (count >= limit) break
            }
        }
        recurse()
        return first to count
    }
}
