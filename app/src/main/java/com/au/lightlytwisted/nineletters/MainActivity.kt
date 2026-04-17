/*
 * File:     MainActivity.kt
 * Created:  2026-04-13
 * Modified: 2026-04-16
 * Author:   Jade
 * Purpose:  Entry point for the app. Sets up the NavHost that routes between
 *           the Home, Puzzle, and Help screens.
 * Notes:    All game logic lives in PuzzleViewModel; all UI is in the
 *           screen composables. This file only handles app setup and navigation.
 *           Sound is played here via ToneGenerator so the ViewModel stays
 *           free of Android audio dependencies.
 */

package com.au.lightlytwisted.nineletters

import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.au.lightlytwisted.nineletters.ui.theme.NineLettersTheme

// App entry point — sets up the theme and the navigation graph
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NineLettersTheme {
                AppNavigation()
            }
        }
    }
}

// Defines the three-screen navigation graph: home → puzzle, home → help
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModel scoped to the activity so Home and Help can both trigger puzzle loads
    val puzzleVm: PuzzleViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                HomeScreen(
                    onNewPuzzle = {
                        puzzleVm.loadRandomPuzzle()
                        navController.navigate("puzzle")
                    },
                    onHelp = { navController.navigate("help") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("puzzle") {
            // Receives the shared ViewModel rather than creating its own
            PuzzleScreen(vm = puzzleVm, onMainMenu = { navController.popBackStack() })
        }
        composable("help") {
            HelpScreen(
                onBack = { navController.popBackStack() },
                onStartNewPuzzle = {
                    puzzleVm.loadRandomPuzzle()
                    navController.navigate("puzzle") {
                        popUpTo("home")
                    }
                }
            )
        }
    }
}

// Puzzle screen — manages its own Scaffold so the TopAppBar and hamburger menu are self-contained
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(vm: PuzzleViewModel, onMainMenu: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Play a short tone each time the ViewModel signals a correct word
    LaunchedEffect(Unit) {
        vm.solveSound.collect {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(250)
            toneGen.release()
        }
    }

    // Local UI state for the hamburger menu and results popup
    var menuExpanded by remember { mutableStateOf(false) }
    var showResults  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nine Letters",
                        fontWeight = FontWeight.Bold
                    )
                },
                expandedHeight = 48.dp,
                actions = {
                    // Hamburger menu button in the top right
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                    // Dropdown that appears when the hamburger is tapped
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Random Puzzle") },
                            onClick = {
                                menuExpanded = false
                                vm.loadRandomPuzzle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Main Menu") },
                            onClick = {
                                menuExpanded = false
                                onMainMenu()
                            }
                        )
                        HorizontalDivider()
                        // Settings toggles — checkmark appears when option is on
                        SettingsMenuItem("Clue",             state.settings.showClue)         { vm.toggleClue() }
                        SettingsMenuItem("Show Blanks",      state.settings.showBlanks)       { vm.toggleBlanks() }
                        SettingsMenuItem("Show First Letter",state.settings.showFirstLetter)  { vm.toggleFirstLetter() }
                        SettingsMenuItem("Timer",            state.settings.showTimer)        { vm.toggleTimer() }
                        SettingsMenuItem("Sound on Solve",   state.settings.playSoundOnSolve) { vm.toggleSound() }
                    }
                }
            )
        }
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Shared computation used by both portrait and landscape layouts
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val tileSize    = if (isLandscape) 60.dp else 80.dp
        val grid        = state.gridLetters
        val guessColor  = when (state.guessResult) {
            GuessResult.CORRECT -> Color(0xFF2E7D32)
            GuessResult.WRONG   -> Color(0xFFC62828)
            GuessResult.NONE    -> MaterialTheme.colorScheme.onSurface
        }
        val guessedSet  = state.guessedWords.toSet()
        val allAnswers  = (state.guessedWords + state.remainingAnswers.toList())
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
        val s = state.settings
        val displayWords: List<Triple<String, Boolean, Int>> = when {
            state.answersRevealed ->
                allAnswers.map { Triple(it.uppercase(), it in guessedSet, it.length) }
            s.showFirstLetter ->
                allAnswers.map { word ->
                    if (word in guessedSet) Triple(word.uppercase(), true, word.length)
                    else Triple("${word[0].uppercaseChar()} ${"_ ".repeat(word.length - 1).trim()}", false, word.length)
                }
            s.showBlanks ->
                allAnswers.map { word ->
                    if (word in guessedSet) Triple(word.uppercase(), true, word.length)
                    else Triple("_ ".repeat(word.length).trim(), false, word.length)
                }
            else ->
                allAnswers.filter { it in guessedSet }.map { Triple(it.uppercase(), true, it.length) }
        }
        val byLength = displayWords.groupBy { it.third }
        val lengths  = byLength.keys.sortedDescending()

        if (isLandscape) {
            // ── Landscape: controls on the left, word list on the right ──────────────
            Row(Modifier.fillMaxSize().padding(innerPadding)) {

                // Left column — game controls, vertically centred
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                ) {
                    // Clue (if enabled)
                    if (s.showClue && state.puzzleClue.isNotEmpty()) {
                        Text(
                            text = state.puzzleClue,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3x3 letter grid (smaller tiles in landscape)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (col in 0..2) {
                                    val index = row * 3 + col
                                    LetterTile(
                                        letter    = grid[index].uppercaseChar(),
                                        isCentre  = index == 4,
                                        isSelected = index in state.selectedTileIndices,
                                        onClick   = { vm.onLetterClick(index, grid[index]) },
                                        size      = tileSize
                                    )
                                }
                            }
                        }
                    }

                    // Guess display or error message — only one shown at a time
                    if (state.guessMessage.isNotEmpty()) {
                        Text(
                            text      = state.guessMessage,
                            fontSize  = 12.sp,
                            color     = Color(0xFFC62828),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text          = state.currentGuess.uppercase().ifEmpty { "—" },
                            fontSize      = 22.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = guessColor,
                            letterSpacing = 4.sp
                        )
                    }

                    // CHECK / CLEAR buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.onGuess() }, enabled = state.currentGuess.isNotEmpty()) {
                            Text("CHECK")
                        }
                        OutlinedButton(onClick = { vm.onClear() }) { Text("CLEAR") }
                    }

                    // Progress bar and timer on one row to save vertical space
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.weight(1f).height(6.dp)
                        )
                        if (s.showTimer) {
                            Text(
                                text     = "%02d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60),
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right column — scrollable word list with Show Answers pinned below
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (displayWords.isNotEmpty()) {
                        LazyColumn(
                            modifier             = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement  = Arrangement.spacedBy(2.dp)
                        ) {
                            for (len in lengths) {
                                val group = byLength[len] ?: continue
                                val cols  = when (len) { 9 -> 1; 8 -> 2; else -> if (len >= 5) 3 else 4 }
                                val fSize = when (len) { 9 -> 18.sp; 8 -> 16.sp; 7, 6, 5 -> 14.sp; else -> 12.sp }
                                items(group.chunked(cols)) { row ->
                                    Row(Modifier.fillMaxWidth()) {
                                        row.forEach { (text, found, _) ->
                                            Text(
                                                text      = text,
                                                color     = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                fontSize  = fSize,
                                                modifier  = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    if (!state.answersRevealed) {
                        Button(
                            onClick  = { vm.onShowAnswers(); showResults = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("SHOW ANSWERS") }
                    }
                }
            }

        } else {
            // ── Portrait: single scrolling column ────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                // 3x3 letter grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                LetterTile(
                                    letter    = grid[index].uppercaseChar(),
                                    isCentre  = index == 4,
                                    isSelected = index in state.selectedTileIndices,
                                    onClick   = { vm.onLetterClick(index, grid[index]) },
                                    size      = tileSize
                                )
                            }
                        }
                    }
                }

                // Clue (if enabled)
                if (s.showClue && state.puzzleClue.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text      = state.puzzleClue,
                        fontSize  = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    Spacer(Modifier.height(24.dp))
                }

                // Guess display or error message — only one shown at a time
                if (state.guessMessage.isNotEmpty()) {
                    Text(
                        text      = state.guessMessage,
                        fontSize  = 13.sp,
                        color     = Color(0xFFC62828),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text          = state.currentGuess.uppercase().ifEmpty { "—" },
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = guessColor,
                        letterSpacing = 4.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // CHECK / CLEAR buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.onGuess() }, enabled = state.currentGuess.isNotEmpty()) {
                        Text("CHECK")
                    }
                    OutlinedButton(onClick = { vm.onClear() }) { Text("CLEAR") }
                }

                Spacer(Modifier.height(16.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )

                // Timer (right-aligned below progress bar)
                if (s.showTimer) {
                    Text(
                        text      = "%02d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60),
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                // Word list
                if (displayWords.isNotEmpty()) {
                    LazyColumn(
                        modifier            = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (len in lengths) {
                            val group = byLength[len] ?: continue
                            val cols  = when (len) { 9 -> 1; 8 -> 2; else -> if (len >= 5) 3 else 4 }
                            val fSize = when (len) { 9 -> 18.sp; 8 -> 16.sp; 7, 6, 5 -> 14.sp; else -> 12.sp }
                            items(group.chunked(cols)) { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { (text, found, _) ->
                                        Text(
                                            text      = text,
                                            color     = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            fontSize  = fSize,
                                            modifier  = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Show Answers button
                if (!state.answersRevealed) {
                    Button(
                        onClick  = { vm.onShowAnswers(); showResults = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("SHOW ANSWERS") }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // Results dialog — overlays the full screen in both orientations
        if (showResults) {
            ResultsDialog(
                guessedWords    = state.guessedWords,
                remainingAnswers = state.remainingAnswers,
                onNewPuzzle     = { showResults = false; vm.loadRandomPuzzle() },
                onDismiss       = { showResults = false }
            )
        }
    }
}

// A toggle row in the hamburger menu — checkmark icon when enabled, empty space to preserve alignment when off
@Composable
fun SettingsMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = {
            if (checked)
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            else
                Spacer(Modifier.size(24.dp))
        }
    )
}

// A single letter button — black for the centre tile, green when selected, surface colour otherwise
@Composable
fun LetterTile(letter: Char, isCentre: Boolean, isSelected: Boolean, onClick: () -> Unit, size: Dp = 80.dp) {
    val containerColor = when {
        isSelected -> Color(0xFF4CAF50)
        isCentre   -> Color.Black
        else       -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> Color.White
        isCentre   -> Color.White
        else       -> MaterialTheme.colorScheme.onSurface
    }
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = letter.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
