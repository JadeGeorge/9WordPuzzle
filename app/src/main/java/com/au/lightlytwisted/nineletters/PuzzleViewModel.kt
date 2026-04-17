/*
 * File:     PuzzleViewModel.kt
 * Created:  2026-04-13
 * Modified: 2026-04-16
 * Author:   Jade
 * Purpose:  Holds all game state and logic for the Nine Letters puzzle.
 *           The UI observes PuzzleState via a StateFlow and calls the event
 *           functions (onLetterClick, onGuess, etc.) in response to user input.
 * Notes:    Dictionary loading runs once on startup; switching puzzles only
 *           reruns the answer-finding step, not the full dictionary load.
 *           selectedTileIndices tracks which grid positions have been tapped so
 *           each tile can only be used once per guess.
 *           Settings persist across puzzle loads (preserved in setupPuzzle).
 *           solveSound is a SharedFlow that fires once per correct guess so the
 *           UI can play a tone without coupling sound logic to the ViewModel.
 */

package com.au.lightlytwisted.nineletters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Tracks the result of the most recent guess so the UI can colour the display
enum class GuessResult { NONE, CORRECT, WRONG }

// Toggleable cheat/assist options — persisted across puzzle loads
data class Settings(
    val showClue: Boolean = false,          // Show a hint about the 9-letter word
    val showBlanks: Boolean = true,         // Show _ _ _ _ slots for undiscovered words
    val showFirstLetter: Boolean = false,   // Show B _ _ _ _ (first letter revealed)
    val showTimer: Boolean = true,          // Show elapsed time counter
    val playSoundOnSolve: Boolean = true    // Play a tone when a word is found
)

// Immutable snapshot of everything the UI needs to render the current game state
data class PuzzleState(
    val isLoading: Boolean = true,
    val puzzleName: String = "",
    val puzzleClue: String = "",                     // Short hint for the 9-letter word
    val gridLetters: List<Char> = emptyList(),       // 9 letters; index 4 is always the centre
    val centreLetter: Char = ' ',
    val currentGuess: String = "",
    val selectedTileIndices: List<Int> = emptyList(), // grid positions tapped for the current guess
    val remainingAnswers: Set<String> = emptySet(),
    val guessedWords: List<String> = emptyList(),
    val guessResult: GuessResult = GuessResult.NONE,
    val guessMessage: String = "",
    val answersRevealed: Boolean = false,
    val settings: Settings = Settings(),             // User's current cheat/assist settings
    val elapsedSeconds: Long = 0L                    // Elapsed time since this puzzle was loaded
) {
    // Total number of valid answers for the current puzzle
    val totalAnswers: Int get() = remainingAnswers.size + guessedWords.size

    // Fraction of answers found — used to drive the progress bar
    val progress: Float get() = if (totalAnswers == 0) 0f else guessedWords.size.toFloat() / totalAnswers
}

class PuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PuzzleState())
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    // The full list of puzzles loaded from assets/puzzles.txt
    private val _puzzles = MutableStateFlow<List<Puzzle>>(emptyList())
    val puzzles: StateFlow<List<Puzzle>> = _puzzles.asStateFlow()

    // Fires once per correct guess so the UI can play a sound without coupling audio to the ViewModel
    private val _solveSound = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val solveSound: SharedFlow<Unit> = _solveSound.asSharedFlow()

    private val dictionary = Dictionary()
    private var timerJob: Job? = null

    // Load the dictionary and puzzle list, then start with a random puzzle
    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDictionary()
            loadPuzzles()
            val p = _puzzles.value
            if (p.isNotEmpty()) setupPuzzle(p.random())
        }
    }

    // Reads puzzles.txt from assets and populates the puzzle list
    private fun loadPuzzles() {
        val loaded = mutableListOf<Puzzle>()
        getApplication<Application>().assets.open("puzzles.txt")
            .bufferedReader()
            .forEachLine { line -> parsePuzzleLine(line)?.let { loaded.add(it) } }
        _puzzles.value = loaded
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
        // Pick a random centre letter from the word each time the puzzle is played
        val centreLetter = puzzle.word.random()

        val answers = dictionary.findMatches(puzzle.word)
            .filter { it.contains(centreLetter) }
            .toSet()

        val outer = puzzle.word.toMutableList()
            .also { it.remove(centreLetter) }
            .shuffled()

        // Grid: outer[0..3] | centre | outer[4..7]
        val grid = outer.subList(0, 4) + listOf(centreLetter) + outer.subList(4, 8)

        // Preserve settings across puzzle loads; reset everything else including the timer
        val preserved = _state.value.settings

        withContext(Dispatchers.Main) {
            _state.value = PuzzleState(
                isLoading = false,
                puzzleName = puzzle.displayName,
                puzzleClue = puzzle.clue,
                gridLetters = grid,
                centreLetter = centreLetter,
                remainingAnswers = answers,
                settings = preserved
            )
            startTimer()
        }
    }

    // Increments elapsedSeconds every second until cancelled
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.value = _state.value.copy(elapsedSeconds = _state.value.elapsedSeconds + 1)
            }
        }
    }

    // Stops the timer — called when the player reveals answers
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // Loads a specific puzzle chosen by the player, resetting all game state
    fun loadPuzzle(puzzle: Puzzle) {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            setupPuzzle(puzzle)
        }
    }

    // Picks a random puzzle from the loaded list
    fun loadRandomPuzzle() {
        val p = _puzzles.value
        if (p.isNotEmpty()) loadPuzzle(p.random())
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
            if (s.settings.playSoundOnSolve) _solveSound.tryEmit(Unit)
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

    // Reveals all remaining unfound answers in the word list and stops the timer
    fun onShowAnswers() {
        stopTimer()
        _state.value = _state.value.copy(answersRevealed = true)
    }

    // Settings toggle functions — each flips one option without affecting anything else
    private fun updateSettings(transform: (Settings) -> Settings) {
        _state.value = _state.value.copy(settings = transform(_state.value.settings))
    }

    fun toggleClue()        { updateSettings { it.copy(showClue        = !it.showClue) } }
    fun toggleBlanks()      { updateSettings { it.copy(showBlanks      = !it.showBlanks) } }
    fun toggleFirstLetter() { updateSettings { it.copy(showFirstLetter = !it.showFirstLetter) } }
    fun toggleTimer()       { updateSettings { it.copy(showTimer       = !it.showTimer) } }
    fun toggleSound()       { updateSettings { it.copy(playSoundOnSolve = !it.playSoundOnSolve) } }
}
