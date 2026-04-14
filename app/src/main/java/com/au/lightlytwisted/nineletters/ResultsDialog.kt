/*
 * File:     ResultsDialog.kt
 * Created:  2026-04-14
 * Modified: 2026-04-14
 * Author:   Jade
 * Purpose:  Full-screen results popup shown when the player taps SHOW ANSWERS.
 *           Displays score stats, the best achievement earned, all answers in a
 *           3-column grid (green = found, grey = missed), and a New Puzzle button.
 * Notes:    Achievements are evaluated in order from most to least impressive;
 *           the first one the player qualifies for is shown.
 *           Add new achievements to achievementChecks in computeAchievement().
 */

package com.au.lightlytwisted.nineletters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Holds the title and description for a single achievement
data class Achievement(val title: String, val description: String)

// Evaluates which achievements the player earned and returns the most impressive one
fun computeAchievement(guessedWords: List<String>, remainingAnswers: Set<String>): Achievement? {
    val allWords = guessedWords + remainingAnswers.toList()
    val guessedSet = guessedWords.toSet()
    val total = allWords.size
    val guessed = guessedWords.size

    // Perfect score — check first as it's the best possible result
    if (remainingAnswers.isEmpty() && guessed > 0) {
        return Achievement("Perfect Score!", "You found every single word. Incredible!")
    }

    // All words of a specific length — checked longest first so the best applies
    val lengthAchievements = listOf(
        8 to Achievement("8 Letter Legend",  "You found all the 8-letter words!"),
        7 to Achievement("Lucky Seven",       "You found all the 7-letter words!"),
        6 to Achievement("Six Shooter",       "You found all the 6-letter words!"),
        5 to Achievement("5 Word Wonder",     "You found all the 5-letter words!"),
        4 to Achievement("Fab Four",          "You found all the 4-letter words!")
    )
    for ((len, achievement) in lengthAchievements) {
        val ofLength = allWords.filter { it.length == len }
        if (ofLength.isNotEmpty() && ofLength.all { it in guessedSet }) {
            return achievement
        }
    }

    // Percentage milestones
    val pct = if (total > 0) guessed * 100 / total else 0
    if (pct >= 75) return Achievement("Sharp Shooter",  "You found over 75% of the words!")
    if (pct >= 50) return Achievement("Halfway Hero",   "You found more than half the words!")
    if (guessed >= 1) return Achievement("Word Seeker", "You made a start — keep practising!")

    return null
}

// The results popup — shown when the player taps SHOW ANSWERS
@Composable
fun ResultsDialog(
    guessedWords: List<String>,
    remainingAnswers: Set<String>,
    onNewPuzzle: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = guessedWords.size + remainingAnswers.size
    val achievement = computeAchievement(guessedWords, remainingAnswers)
    val guessedSet = guessedWords.toSet()

    // Sort all words by length descending, then alphabetically — longest words shown first
    val allWords = (guessedWords + remainingAnswers.toList())
        .sortedWith(compareByDescending<String> { it.length }.thenBy { it })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score summary header
            Text(
                text = "Well done!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "You got ${ guessedWords.size } out of $total words",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Achievement badge — only shown if the player earned one
            if (achievement != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = achievement.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = achievement.description,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3-column word grid — green for found words, muted for missed
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allWords) { word ->
                    val wasGuessed = word in guessedSet
                    Text(
                        text = word.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = if (wasGuessed) FontWeight.Bold else FontWeight.Normal,
                        color = if (wasGuessed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // New Puzzle button pinned to the bottom of the dialog
            Button(
                onClick = onNewPuzzle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NEW PUZZLE", fontSize = 16.sp)
            }
        }
    }
}
