/*
 * File:     MainActivity.kt
 * Created:  2026-04-13
 * Modified: 2026-04-14
 * Author:   Jade
 * Purpose:  Entry point for the app. Sets up the NavHost that routes between
 *           the Home, Puzzle, and Help screens.
 * Notes:    All game logic lives in PuzzleViewModel; all UI is in the
 *           screen composables. This file only handles app setup and navigation.
 */

package com.au.lightlytwisted.nineletters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                HomeScreen(
                    onNewPuzzle = { navController.navigate("puzzle") },
                    onHelp = { navController.navigate("help") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("puzzle") {
            PuzzleScreen()
        }
        composable("help") {
            HelpScreen(onBack = { navController.popBackStack() })
        }
    }
}

// Puzzle screen — manages its own Scaffold so the TopAppBar and hamburger menu are self-contained
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(vm: PuzzleViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Local UI state for the hamburger menu, puzzle chooser dialog, and results popup
    var menuExpanded by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nine Letters",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Choose Puzzle") },
                            onClick = {
                                menuExpanded = false
                                showChooser = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        // Puzzle chooser dialog — shown when the player taps Choose Puzzle
        if (showChooser) {
            PuzzleChooserDialog(
                onPuzzleSelected = { puzzle ->
                    showChooser = false
                    vm.loadPuzzle(puzzle)
                },
                onDismiss = { showChooser = false }
            )
        }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // 3x3 letter grid — index 4 is the centre tile
            val grid = state.gridLetters
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            LetterTile(
                                letter = grid[index].uppercaseChar(),
                                isCentre = index == 4,
                                onClick = { vm.onLetterClick(grid[index]) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Current guess display — turns green on correct, red on wrong
            val guessColor = when (state.guessResult) {
                GuessResult.CORRECT -> Color(0xFF2E7D32)
                GuessResult.WRONG -> Color(0xFFC62828)
                GuessResult.NONE -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = state.currentGuess.uppercase().ifEmpty { "—" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = guessColor,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(16.dp))

            // CHECK submits the guess; CLEAR wipes it without scoring
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.onGuess() },
                    enabled = state.currentGuess.isNotEmpty()
                ) {
                    Text("CHECK")
                }
                OutlinedButton(onClick = { vm.onClear() }) {
                    Text("CLEAR")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar showing how many answers have been found
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Scrollable list of found words (primary colour) and revealed answers (grey)
            val guessedItems = state.guessedWords.map { it to true }
            val revealedItems = if (state.answersRevealed)
                state.remainingAnswers.sorted().map { it to false }
            else
                emptyList()
            val allWords = guessedItems + revealedItems

            if (allWords.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allWords) { (word, guessed) ->
                        Text(
                            text = word.uppercase(),
                            color = if (guessed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // SHOW ANSWERS opens the results popup; afterwards the button is hidden
            if (!state.answersRevealed) {
                Button(
                    onClick = {
                        vm.onShowAnswers()
                        showResults = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SHOW ANSWERS")
                }
            }

            // Results popup — shown after the player reveals answers
            if (showResults) {
                ResultsDialog(
                    guessedWords = state.guessedWords,
                    remainingAnswers = state.remainingAnswers,
                    onNewPuzzle = {
                        showResults = false
                        vm.loadRandomPuzzle()
                    },
                    onDismiss = { showResults = false }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// Dialog showing the full list of puzzles for the player to choose from
@Composable
fun PuzzleChooserDialog(onPuzzleSelected: (Puzzle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Puzzle") },
        text = {
            LazyColumn {
                items(puzzleList) { puzzle ->
                    TextButton(
                        onClick = { onPuzzleSelected(puzzle) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = puzzle.displayName,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// A single letter button in the grid — black background for the centre tile, surface colour for the rest
@Composable
fun LetterTile(letter: Char, isCentre: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCentre) Color.Black else MaterialTheme.colorScheme.surface,
            contentColor = if (isCentre) Color.White else MaterialTheme.colorScheme.onSurface
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
