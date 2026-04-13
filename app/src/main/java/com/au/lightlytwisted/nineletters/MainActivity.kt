/*
 * File:     MainActivity.kt
 * Created:  2026-04-13
 * Modified: 2026-04-13
 * Author:   Jade
 * Purpose:  Entry point for the app. Hosts the Compose UI for the Nine Letters puzzle —
 *           a 3x3 letter grid where the player taps letters to build words, with the
 *           centre tile being mandatory in every valid answer.
 * Notes:    Ported from the original 2015 HTML/JS web app (index.html).
 *           All game logic lives in PuzzleViewModel; this file is purely UI.
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.au.lightlytwisted.nineletters.ui.theme.NineLettersTheme

// App entry point — sets up the theme and hands off to the puzzle screen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NineLettersTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PuzzleScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Root composable — shows a loading spinner until the dictionary is ready, then renders the game
@Composable
fun PuzzleScreen(
    modifier: Modifier = Modifier,
    vm: PuzzleViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text("Nine Letters", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

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

        // Shows SHOW ANSWERS button until tapped, then shows the final score summary
        if (!state.answersRevealed) {
            Button(
                onClick = { vm.onShowAnswers() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SHOW ANSWERS")
            }
        } else {
            val total = state.totalAnswers
            val guessed = state.guessedWords.size
            val pct = if (total > 0) guessed * 100 / total else 0
            val message = when {
                pct == 100 -> "You got them all — AMAZING!"
                pct >= 70 -> "Wow, fantastic!"
                pct >= 40 -> "Great!"
                pct >= 10 -> "Good work!"
                else -> "Try some more!"
            }
            Text(
                text = "You guessed $guessed out of $total. $message",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
    }
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
