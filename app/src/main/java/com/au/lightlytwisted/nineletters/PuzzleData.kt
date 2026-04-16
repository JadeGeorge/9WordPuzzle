/*
 * File:     PuzzleData.kt
 * Created:  2026-04-14
 * Modified: 2026-04-16
 * Author:   Jade
 * Purpose:  Defines the Puzzle data type and the master list of available puzzles.
 * Notes:    To add a new puzzle, append an entry to puzzleList.
 *           The centreLetter must appear in the word — it is required in every valid answer.
 *           All words must be exactly 9 letters.
 *           The clue is a short hint describing the 9-letter word, shown when the Clue setting is on.
 */

package com.au.lightlytwisted.nineletters

// Represents a single playable puzzle
data class Puzzle(
    val id: String,
    val word: String,         // The 9-letter word used to populate the grid
    val centreLetter: Char,   // The mandatory letter that must appear in every answer
    val displayName: String,  // Shown in the puzzle chooser list
    val clue: String          // Short hint describing the 9-letter word (shown when Clue setting is on)
)

// Master list of all available puzzles
val puzzleList = listOf(
    Puzzle("scrambler", "scrambler", 's', "Scrambler", "Mixes letters beyond recognition"),
    Puzzle("carpenter", "carpenter", 'r', "Carpenter", "A craftsperson who works with wood"),
    Puzzle("chocolate", "chocolate", 'c', "Chocolate", "A sweet treat made from cacao"),
    Puzzle("adventure", "adventure", 'e', "Adventure", "A novel undertaking"),
    Puzzle("blackbird", "blackbird", 'b', "Blackbird", "A dark-feathered garden songbird"),
    Puzzle("generator", "generator", 'e', "Generator", "A machine that produces electricity"),
    Puzzle("australia", "australia", 'a', "Australia", "The land down under"),
    Puzzle("birthdays", "birthdays", 'h', "Birthdays", "Annual celebrations with cake and candles"),
    Puzzle("forgotten", "forgotten", 'o', "Forgotten", "Slipped completely from memory"),
    Puzzle("databases", "databases", 'a', "Databases", "Organised collections of stored information"),
)
