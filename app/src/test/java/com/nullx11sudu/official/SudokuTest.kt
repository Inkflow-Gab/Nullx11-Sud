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
}
