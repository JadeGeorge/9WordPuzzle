/*
 * File:     PuzzleViewModel.kt
 * Created:  2026-04-13
 * Modified: 2026-04-15
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
    val guessMessage: String = "",
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

    // Reads dictionary.txt from assets (one word per line), keeps words of 4+ letters
    private fun loadDictionary() {
        getApplication<Application>().assets.open("dictionary.txt")
            .bufferedReader()
            .forEachLine { line ->
                val word = line.trim().lowercase()
                if (word.length >= 4 && word.all { it.isLetter() }) dictionary.addWord(word)
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

    // Tapping a tile selects it and adds the letter; tapping it again deselects and removes the letter
    fun onLetterClick(tileIndex: Int, letter: Char) {
        val s = _state.value
        val existingPosition = s.selectedTileIndices.indexOf(tileIndex)
        if (existingPosition >= 0) {
            // Already selected — remove this letter from the guess at its exact position
            _state.value = s.copy(
                currentGuess = s.currentGuess.removeRange(existingPosition, existingPosition + 1),
                selectedTileIndices = s.selectedTileIndices.toMutableList().also { it.removeAt(existingPosition) },
                guessResult = GuessResult.NONE,
                guessMessage = ""
            )
        } else {
            // Not yet selected — append to guess and mark tile
            _state.value = s.copy(
                currentGuess = s.currentGuess + letter,
                selectedTileIndices = s.selectedTileIndices + tileIndex,
                guessResult = GuessResult.NONE,
                guessMessage = ""
            )
        }
    }

    // Checks the current guess and returns a specific message explaining any failure
    fun onGuess() {
        val s = _state.value
        val guess = s.currentGuess.lowercase()
        val display = guess.uppercase()

        val (result, message) = when {
            guess.length < 4 ->
                GuessResult.WRONG to "$display: words must be at least 4 letters"

            !guess.contains(s.centreLetter) ->
                GuessResult.WRONG to "$display: must include the letter '${s.centreLetter.uppercaseChar()}'"

            guess in s.guessedWords ->
                GuessResult.WRONG to "$display: already found"

            guess in s.remainingAnswers ->
                GuessResult.CORRECT to ""

            guess.endsWith('s') && dictionary.contains(guess.dropLast(1)) ->
                GuessResult.WRONG to "$display: plurals not allowed"

            dictionary.contains(guess) ->
                GuessResult.WRONG to "$display: not a puzzle word"

            else ->
                GuessResult.WRONG to "$display: not a valid word"
        }

        if (result == GuessResult.CORRECT) {
            _state.value = s.copy(
                remainingAnswers = s.remainingAnswers - guess,
                guessedWords = (s.guessedWords + guess).sorted(),
                currentGuess = "",
                selectedTileIndices = emptyList(),
                guessResult = GuessResult.CORRECT,
                guessMessage = ""
            )
        } else {
            _state.value = s.copy(
                currentGuess = "",
                selectedTileIndices = emptyList(),
                guessResult = GuessResult.WRONG,
                guessMessage = message
            )
        }
    }

    // Clears the current guess, deselects all tiles, and dismisses any error message
    fun onClear() {
        _state.value = _state.value.copy(
            currentGuess = "",
            selectedTileIndices = emptyList(),
            guessResult = GuessResult.NONE,
            guessMessage = ""
        )
    }

    // Reveals all remaining unfound answers in the word list
    fun onShowAnswers() {
        _state.value = _state.value.copy(answersRevealed = true)
    }
}
