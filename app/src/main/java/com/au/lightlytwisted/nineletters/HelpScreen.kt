/*
 * File:     HelpScreen.kt
 * Created:  2026-04-13
 * Modified: 2026-04-15
 * Author:   Jade
 * Purpose:  Interactive tutorial screen. Animates a live SCRAMBLER demo then
 *           lets the player try as many words as they like before starting a
 *           real random puzzle via the "Start with a new word" button.
 * Notes:    Grid is fixed as SCRAMBLER with S at centre (index 4).
 *           Grid layout:  C(0) R(1) A(2)
 *                         M(3) S(4) B(5)
 *                         L(6) E(7) R(8)
 *           Instruction text cycles through five teaching points, one at a time.
 *           onStartNewPuzzle is called when the player is ready — the caller
 *           loads a random puzzle and navigates to the game screen.
 */

package com.au.lightlytwisted.nineletters

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Fixed grid for the tutorial — SCRAMBLER with S at centre
private val helpGrid = listOf('C', 'R', 'A', 'M', 'S', 'B', 'L', 'E', 'R')

// Tile indices for each demo word
private val bearsTiles = listOf(5, 7, 2, 1, 4)  // B E A R S
private val seaTiles   = listOf(4, 7, 2)          // S E A
private val ambleTiles = listOf(2, 3, 5, 6, 7)   // A M B L E

// Two states: animation playing, or player is in control
private enum class HelpPhase { ANIMATING, PLAYER_TURN }

// Help screen — animated demo followed by an open-ended player practice round
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onStartNewPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phase           by remember { mutableStateOf(HelpPhase.ANIMATING) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentWord     by remember { mutableStateOf("") }
    var checkPressed    by remember { mutableStateOf(false) }
    var foundWords      by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage    by remember { mutableStateOf("") }
    var instruction     by remember { mutableStateOf("Tap the letters to make words.") }
    var foundAtLeastOne by remember { mutableStateOf(false) }

    // Shrinks the CHECK button briefly to simulate a physical press
    val checkScale by animateFloatAsState(
        targetValue   = if (checkPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 80),
        label         = "checkScale"
    )

    // Full animation sequence — runs once, ends by handing control to the player
    LaunchedEffect(Unit) {
        delay(900)

        // ── BEARS: valid word ──────────────────────────────────────────────
        for (i in bearsTiles.indices) {
            selectedIndices = bearsTiles.subList(0, i + 1)
            currentWord     = "BEARS".substring(0, i + 1)
            delay(550)
        }
        delay(350)
        checkPressed = true;  delay(150);  checkPressed = false
        delay(250)
        foundWords      = listOf("BEARS")
        selectedIndices = emptyList()
        currentWord     = ""
        delay(900)

        // ── SEA: too short ─────────────────────────────────────────────────
        instruction = "Words must be 4 letters or longer."
        delay(600)
        for (i in seaTiles.indices) {
            selectedIndices = seaTiles.subList(0, i + 1)
            currentWord     = "SEA".substring(0, i + 1)
            delay(550)
        }
        delay(350)
        checkPressed = true;  delay(150);  checkPressed = false
        delay(250)
        errorMessage    = "SEA: words must be at least 4 letters"
        selectedIndices = emptyList()
        currentWord     = ""
        delay(1800)
        errorMessage    = ""
        delay(600)

        // ── Extra words added silently ─────────────────────────────────────
        foundWords = foundWords + "ACRES";  delay(500)
        foundWords = foundWords + "SCARE";  delay(500)
        foundWords = foundWords + "CARES";  delay(700)

        // ── AMBLE: missing centre letter ───────────────────────────────────
        instruction = "Words must contain the special letter."
        delay(600)
        for (i in ambleTiles.indices) {
            selectedIndices = ambleTiles.subList(0, i + 1)
            currentWord     = "AMBLE".substring(0, i + 1)
            delay(550)
        }
        delay(350)
        checkPressed = true;  delay(150);  checkPressed = false
        delay(250)
        errorMessage    = "AMBLE: must include the letter 'S'"
        selectedIndices = emptyList()
        currentWord     = ""
        delay(1800)
        errorMessage    = ""
        delay(1200)

        // ── Hint about the 9-letter word ───────────────────────────────────
        instruction = "Try to find the 9-letter word."
        delay(1800)

        // ── Hand control to the player ─────────────────────────────────────
        instruction = "You're ready to go."
        phase       = HelpPhase.PLAYER_TURN
    }

    // Handles a tile tap during the player practice round
    fun onTileTap(index: Int, letter: Char) {
        errorMessage = ""
        val existingPos = selectedIndices.indexOf(index)
        if (existingPos >= 0) {
            selectedIndices = selectedIndices.toMutableList().also { it.removeAt(existingPos) }
            currentWord     = currentWord.removeRange(existingPos, existingPos + 1)
        } else {
            selectedIndices = selectedIndices + index
            currentWord     = currentWord + letter
        }
    }

    // Validates the player's word — 4+ letters containing S
    fun onPlayerCheck() {
        val word    = currentWord.lowercase()
        val display = currentWord.uppercase()
        when {
            word.length < 4     -> errorMessage = "$display: words must be at least 4 letters"
            !word.contains('s') -> errorMessage = "$display: must include the letter 'S'"
            else -> {
                foundWords      = foundWords + display
                selectedIndices = emptyList()
                currentWord     = ""
                errorMessage    = ""
                foundAtLeastOne = true
                instruction     = "Well done! Keep going or start a new puzzle."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar   = {
            TopAppBar(
                title          = { Text("How to Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Single-line instruction — changes as the demo progresses
            Text(
                text       = instruction,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = if (foundAtLeastOne) Color(0xFF2E7D32)
                             else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(28.dp))

            // 3x3 letter grid — interactive only during PLAYER_TURN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            LetterTile(
                                letter     = helpGrid[index].uppercaseChar(),
                                isCentre   = index == 4,
                                isSelected = index in selectedIndices,
                                onClick    = {
                                    if (phase == HelpPhase.PLAYER_TURN) {
                                        onTileTap(index, helpGrid[index])
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Error message or current word display
            if (errorMessage.isNotEmpty()) {
                Text(
                    text      = errorMessage,
                    fontSize  = 14.sp,
                    color     = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text          = currentWord.ifEmpty { "—" },
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // CHECK (always visible) and CLEAR (player turn only)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = { if (phase == HelpPhase.PLAYER_TURN) onPlayerCheck() },
                    modifier = Modifier.scale(checkScale)
                ) {
                    Text("CHECK")
                }
                if (phase == HelpPhase.PLAYER_TURN) {
                    OutlinedButton(onClick = {
                        selectedIndices = emptyList()
                        currentWord     = ""
                        errorMessage    = ""
                    }) {
                        Text("CLEAR")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Found words list — grows as words are accepted
            if (foundWords.isNotEmpty()) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    foundWords.forEach { word ->
                        Text(
                            text       = word,
                            color      = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            modifier   = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // "Start with a new word" appears once the player has found their first word
            if (foundAtLeastOne) {
                Button(
                    onClick  = onStartNewPuzzle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("START WITH A NEW WORD")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
