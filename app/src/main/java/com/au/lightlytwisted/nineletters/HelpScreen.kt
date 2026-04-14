/*
 * File:     HelpScreen.kt
 * Created:  2026-04-13
 * Modified: 2026-04-13
 * Author:   Jade
 * Purpose:  Explains the rules of the Nine Letters puzzle to the player.
 * Notes:    Reached from the Home screen via the HOW TO PLAY button.
 *           The back button returns to the Home screen.
 */

package com.au.lightlytwisted.nineletters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Help screen with scrollable game rules and a back button in the top bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            // Top bar with back navigation
            TopAppBar(
                title = { Text("How to Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Scrollable rules content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "The Grid",
                body = "You are given a 3×3 grid of 9 letters. The centre letter " +
                        "is highlighted in black — it is the key letter for the puzzle."
            )

            HelpSection(
                title = "The Goal",
                body = "Find as many words as possible using the letters in the grid. " +
                        "Every word you find must include the centre letter."
            )

            HelpSection(
                title = "Building a Word",
                body = "Tap the letter tiles to spell out a word, then press CHECK. " +
                        "Press CLEAR at any time to start the word again."
            )

            HelpSection(
                title = "Valid Words",
                body = "Words must be at least 4 letters long and must only use " +
                        "letters that appear in the grid. You can use each letter " +
                        "as many times as it appears in the 9-letter word."
            )

            HelpSection(
                title = "Progress",
                body = "The bar at the top of the game screen fills as you find more words. " +
                        "Your correct guesses are listed below it."
            )

            HelpSection(
                title = "Show Answers",
                body = "Tap SHOW ANSWERS to reveal any words you missed. " +
                        "Your final score is shown once answers are revealed."
            )

            Spacer(Modifier.height(32.dp))

            // Returns to the home screen
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GOT IT")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// A single titled rule section with a divider underneath
@Composable
private fun HelpSection(title: String, body: String) {
    Spacer(Modifier.height(16.dp))
    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(text = body, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
}
