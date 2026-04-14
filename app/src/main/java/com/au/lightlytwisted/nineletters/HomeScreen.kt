/*
 * File:     HomeScreen.kt
 * Created:  2026-04-13
 * Modified: 2026-04-13
 * Author:   Jade
 * Purpose:  Landing screen shown when the app opens. Provides entry points to
 *           start a new puzzle or read the help instructions.
 * Notes:    Navigation to other screens is handled via callbacks so this
 *           composable stays decoupled from the NavController.
 */

package com.au.lightlytwisted.nineletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Home screen with title, tagline, and the two main navigation buttons
@Composable
fun HomeScreen(
    onNewPuzzle: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = "Nine Letters",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Find as many words as you can.\nEvery word must use the centre letter.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(64.dp))

        // Starts a new puzzle and navigates to the game screen
        Button(
            onClick = onNewPuzzle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("NEW PUZZLE", fontSize = 18.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Opens the how-to-play screen
        OutlinedButton(
            onClick = onHelp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("HOW TO PLAY", fontSize = 18.sp)
        }
    }
}
