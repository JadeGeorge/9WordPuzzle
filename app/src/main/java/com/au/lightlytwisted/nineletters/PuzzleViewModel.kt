/*
 * File:     PuzzleViewModel.kt
 * Created:  2026-04-13
 * Modified: 2026-04-14
 * Author:   Jade
 * Purpose:  Holds all game state and logic for the Nine Letters puzzle.
 *           The UI observes PuzzleState via a StateFlow and calls the event
 *           functions (onLetterClick, onGuess, etc.) in response to user input.
 * Notes:    Dictionary loading runs once on startup; switching puzzles only
 *           reruns the answer-finding step, not the full dictionary load.
 *           selectedTileIndices tracks which grid positions have been tapped so
 *           each tile can only be used once per guess.
 */

package com.au.lightlytwisted.nineletters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Tracks the result of the most recent guess so the UI can colour the display
enum class GuessResult { NONE, CORRECT, WRONG }

// Immutable snapshot of everything the UI needs to render the current game state
data class PuzzleState(
    val isLoading: Boolean = true,
    val puzzleName: String = "",
    val gridLetters: List<Char> = emptyList(),      // 9 letters; index 4 is always the centre
    val centreLetter: Char = ' ',
    val currentGuess: String = "",
    val selectedTileIndices: List<Int> = emptyList(), // grid positions tapped for the current guess
    val remainingAnswers: Set<String> = emptySet(),
    val guessedWords: List<String> = emptyList(),
    val guessResult: GuessResult = GuessResult.NONE,
    val answersRevealed: Boolean = false
) {
    // Total number of valid answers for the current puzzle
    val totalAnswers: Int get() = remainingAnswers.size + guessedWords.size

    // Fraction of answers found — used to drive the progress bar
    val progress: Float get() = if (totalAnswers == 0) 0f else guessedWords.size.toFloat() / totalAnswers
}

class PuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PuzzleState())
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    private val dictionary = Dictionary()

    // Load the dictionary then start with the first puzzle in the list
    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDictionary()
            setupPuzzle(puzzleList.first())
        }
    }

    // Reads words.txt from assets, keeps words of 4+ letters, and adds them to the trie
    private fun loadDictionary() {
        getApplication<Application>().assets.open("words.txt")
            .bufferedReader()
            .forEachLine { line ->
                val word = line.trim().takeWhile { it.isLetter() }.lowercase()
                if (word.length >= 4) dictionary.addWord(word)
            }
    }

    // Finds all valid answers, builds the shuffled letter grid, and publishes the new state
    private suspend fun setupPuzzle(puzzle: Puzzle) {
        val answers = dictionary.findMatches(puzzle.word)
            .filter { it.contains(puzzle.centreLetter) }
            .toSet()

        val outer = puzzle.word.toMutableList()
            .also { it.remove(puzzle.centreLetter) }
            .shuffled()

        // Grid: outer[0..3] | centre | outer[4..7]
        val grid = outer.subList(0, 4) + listOf(puzzle.centreLetter) + outer.subList(4, 8)

        withContext(Dispatchers.Main) {
            _state.value = PuzzleState(
                isLoading = false,
                puzzleName = puzzle.displayName,
                gridLetters = grid,
                centreLetter = puzzle.centreLetter,
                remainingAnswers = answers
            )
        }
    }

    // Loads a specific puzzle chosen by the player, resetting all game state
    fun loadPuzzle(puzzle: Puzzle) {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            setupPuzzle(puzzle)
        }
    }

    // Picks a random puzzle from the list and loads it
    fun loadRandomPuzzle() {
        loadPuzzle(puzzleList.random())
    }

    // Appends a tapped letter to the current guess and marks the tile as selected
    fun onLetterClick(tileIndex: Int, letter: Char) {
        val s = _state.value
        if (tileIndex in s.selectedTileIndices) return  // tile already used in this guess
        _state.value = s.copy(
            currentGuess = s.currentGuess + letter,
            selectedTileIndices = s.selectedTileIndices + tileIndex,
            guessResult = GuessResult.NONE
        )
    }

    // Checks the current guess against remaining answers and resets tile selection
    fun onGuess() {
        val s = _state.value
        val guess = s.currentGuess.lowercase()
        if (guess in s.remainingAnswers) {
            _state.value = s.copy(
                remainingAnswers = s.remainingAnswers - guess,
                guessedWords = (s.guessedWords + guess).sorted(),
                currentGuess = "",
                selectedTileIndices = emptyList(),
                guessResult = GuessResult.CORRECT
            )
        } else {
            _state.value = s.copy(
                currentGuess = "",
                selectedTileIndices = emptyList(),
                guessResult = GuessResult.WRONG
            )
        }
    }

    // Clears the current guess and deselects all tiles
    fun onClear() {
        _state.value = _state.value.copy(
            currentGuess = "",
            selectedTileIndices = emptyList(),
            guessResult = GuessResult.NONE
        )
    }

    // Reveals all remaining unfound answers in the word list
    fun onShowAnswers() {
        _state.value = _state.value.copy(answersRevealed = true)
    }
}
