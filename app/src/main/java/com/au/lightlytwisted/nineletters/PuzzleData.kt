/*
 * File:     PuzzleData.kt
 * Created:  2026-04-14
 * Modified: 2026-04-17
 * Author:   Jade
 * Purpose:  Defines the Puzzle data type and the parser for puzzles.txt.
 * Notes:    Puzzles live in assets/puzzles.txt — format: word|clue
 *           The word must be exactly 9 letters.
 *           The centre letter is chosen randomly each time the puzzle is played (in setupPuzzle).
 *           The clue is a short hint describing the 9-letter word, shown when the Clue setting is on.
 */

package com.au.lightlytwisted.nineletters

// Represents a single playable puzzle
data class Puzzle(
    val id: String,
    val word: String,        // The 9-letter word used to populate the grid
    val displayName: String, // Shown in the puzzle chooser list
    val clue: String         // Short hint describing the 9-letter word (shown when Clue setting is on)
)

// Parses one line from puzzles.txt ("word|clue"); returns null if the line is invalid
fun parsePuzzleLine(line: String): Puzzle? {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
    val parts = trimmed.split("|")
    if (parts.size != 2) return null
    val word = parts[0].trim().lowercase()
    val clue = parts[1].trim()
    if (word.length != 9 || !word.all { it.isLetter() }) return null
    return Puzzle(
        id          = word,
        word        = word,
        displayName = word.replaceFirstChar { it.uppercase() },
        clue        = clue
    )
}
