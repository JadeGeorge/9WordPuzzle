/*
 * File:     PuzzleData.kt
 * Created:  2026-04-14
 * Modified: 2026-04-14
 * Author:   Jade
 * Purpose:  Defines the Puzzle data type and the master list of available puzzles.
 * Notes:    To add a new puzzle, append an entry to puzzleList.
 *           The centreLetter must appear in the word — it is required in every valid answer.
 *           All words must be exactly 9 letters.
 */

package com.au.lightlytwisted.nineletters

// Represents a single playable puzzle
data class Puzzle(
    val id: String,
    val word: String,         // The 9-letter word used to populate the grid
    val centreLetter: Char,   // The mandatory letter that must appear in every answer
    val displayName: String   // Shown in the puzzle chooser list
)

// Master list of all available puzzles
val puzzleList = listOf(
    Puzzle("scrambler", "scrambler", 's', "Scrambler"),
    Puzzle("carpenter", "carpenter", 'r', "Carpenter"),
    Puzzle("chocolate", "chocolate", 'c', "Chocolate"),
    Puzzle("adventure", "adventure", 'e', "Adventure"),
    Puzzle("blackbird", "blackbird", 'b', "Blackbird"),
    Puzzle("generator", "generator", 'e', "Generator"),
    Puzzle("australia", "australia", 'a', "Australia"),
    Puzzle("birthdays", "birthdays", 'h', "Birthdays"),
    Puzzle("forgotten", "forgotten", 'o', "Forgotten"),
    Puzzle("databases", "databases", 'a', "Databases"),
)
