/*
 * File:     PuzzleViewModel.kt
 * Created:  2026-04-13
 * Modified: 2026-04-13
 * Author:   Jade
 * Purpose:  Holds all game state and logic for the Nine Letters puzzle.
 *           The UI observes PuzzleState via a StateFlow and calls the event
 *           functions (onLetterClick, onGuess, etc.) in response to user input.
 * Notes:    Dictionary loading runs on a background thread so the UI stays
 *           responsive on startup. The puzzle word and centre letter are
 *           hardcoded for now — multiple puzzles to be added later.
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
    val gridLetters: List<Char> = emptyList(),  // 9 letters; index 4 is always the centre
    val centreLetter: Char = ' ',
    val currentGuess: String = "",
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

    // Kick off dictionary loading and puzzle setup as soon as the ViewModel is created
    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDictionary()
            setupPuzzle(nineLetterWord = "scrambler", centreLetter = 's')
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

    // Finds all valid answers, builds the shuffled letter grid, and publishes the initial state
    private suspend fun setupPuzzle(nineLetterWord: String, centreLetter: Char) {
        val answers = dictionary.findMatches(nineLetterWord)
            .filter { it.contains(centreLetter) }
            .toSet()

        val outer = nineLetterWord.toMutableList()
            .also { it.remove(centreLetter) }
            .shuffled()

        // Grid: outer[0..3] | centre | outer[4..7]
        val grid = outer.subList(0, 4) + listOf(centreLetter) + outer.subList(4, 8)

        withContext(Dispatchers.Main) {
            _state.value = PuzzleState(
                isLoading = false,
                gridLetters = grid,
                centreLetter = centreLetter,
                remainingAnswers = answers
            )
        }
    }

    // Appends a tapped letter to the current guess
    fun onLetterClick(letter: Char) {
        _state.value = _state.value.copy(
            currentGuess = _state.value.currentGuess + letter,
            guessResult = GuessResult.NONE
        )
    }

    // Checks the current guess against remaining answers and updates state accordingly
    fun onGuess() {
        val s = _state.value
        val guess = s.currentGuess.lowercase()
        if (guess in s.remainingAnswers) {
            _state.value = s.copy(
                remainingAnswers = s.remainingAnswers - guess,
                guessedWords = (s.guessedWords + guess).sorted(),
                currentGuess = "",
                guessResult = GuessResult.CORRECT
            )
        } else {
            _state.value = s.copy(currentGuess = "", guessResult = GuessResult.WRONG)
        }
    }

    // Clears the current guess without scoring it
    fun onClear() {
        _state.value = _state.value.copy(currentGuess = "", guessResult = GuessResult.NONE)
    }

    // Reveals all remaining unfound answers in the word list
    fun onShowAnswers() {
        _state.value = _state.value.copy(answersRevealed = true)
    }
}
