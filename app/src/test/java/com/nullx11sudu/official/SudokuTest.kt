package com.nullx11sudu.official

import org.junit.Assert.*
import org.junit.Test

class SudokuTest {
    @Test fun solvesAndPreservesClues() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079".map { it.digitToInt() }.toIntArray()
        val original = puzzle.clone()
        val solved = Sudoku.solve(puzzle)!!
        assertArrayEquals(original, puzzle)
        assertEquals("534678912672195348198342567859761423426853791713924856961537284287419635345286179", solved.joinToString(""))
    }
    @Test fun rejectsConflictingClues() { assertNull(Sudoku.solve(IntArray(81).also { it[0] = 5; it[1] = 5 })) }
    @Test fun rejectsBadShape() { assertNull(Sudoku.solve(IntArray(80))) }
    @Test fun identifiesAmbiguity() { assertTrue(Sudoku.analyze(IntArray(81)) is Sudoku.Result.Ambiguous) }
    @Test fun identifiesUniqueSolution() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079".map { it.digitToInt() }.toIntArray()
        assertTrue(Sudoku.analyze(puzzle) is Sudoku.Result.Unique)
    }
    @Test fun identifiesInvalidScan() {
        assertTrue(Sudoku.analyze(IntArray(81).also { it[0] = 1; it[9] = 1 }) is Sudoku.Result.Invalid)
    }
    @Test fun hardPuzzleIsSolvedWithoutChangingClues() {
        val encoded = "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......"
        assertEquals(81, encoded.length)
        val clues = encoded.map { if (it == '.') 0 else it.digitToInt() }.toIntArray()
        val result = Sudoku.analyze(clues)
        assertTrue(result is Sudoku.Result.Unique)
        assertValid((result as Sudoku.Result.Unique).board, clues)
    }
    private fun assertValid(solution: IntArray, clues: IntArray) {
        assertEquals(81, solution.size)
        for (i in 0 until 81) if (clues[i] != 0) assertEquals("clue at $i", clues[i], solution[i])
        for (unit in 0..8) {
            val row = (0..8).map { solution[unit * 9 + it] }.toSet()
            val column = (0..8).map { solution[it * 9 + unit] }.toSet()
            val box = (0..8).map { solution[(unit / 3 * 3 + it / 3) * 9 + unit % 3 * 3 + it % 3] }.toSet()
            assertEquals((1..9).toSet(), row)
            assertEquals((1..9).toSet(), column)
            assertEquals((1..9).toSet(), box)
        }
    }
}
