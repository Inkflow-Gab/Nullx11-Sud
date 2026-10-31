package com.nullx11sudu.official

/** Pure solver: returns null for contradictory or unsolvable inputs. Never changes the caller's board. */
object Sudoku {
    fun solve(input: IntArray): IntArray? {
        if (input.size != 81 || input.any { it !in 0..9 }) return null
        val board = input.clone()
        val rows = IntArray(9); val cols = IntArray(9); val boxes = IntArray(9)
        fun box(i: Int) = (i / 27) * 3 + (i % 9) / 3
        for (i in board.indices) {
            val v = board[i]
            if (v == 0) continue
            val r = i / 9; val c = i % 9; val b = box(i); val bit = 1 shl v
            if ((rows[r] or cols[c] or boxes[b]) and bit != 0) return null
            rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
        }
        fun search(): Boolean {
            var chosen = -1; var choices = 0; var minimum = 10
            for (i in board.indices) if (board[i] == 0) {
                val mask = 0x3FE and (rows[i / 9] or cols[i % 9] or boxes[box(i)]).inv()
                val count = Integer.bitCount(mask)
                if (count == 0) return false
                if (count < minimum) { minimum = count; chosen = i; choices = mask }
            }
            if (chosen < 0) return true
            val r = chosen / 9; val c = chosen % 9; val b = box(chosen)
            for (v in 1..9) {
                val bit = 1 shl v
                if (choices and bit == 0) continue
                board[chosen] = v; rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
                if (search()) return true
                board[chosen] = 0; rows[r] = rows[r] xor bit; cols[c] = cols[c] xor bit; boxes[b] = boxes[b] xor bit
            }
            return false
        }
        return if (search()) board else null
    }
}
