package com.example.sparks.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DisappearingMessagesDialog(
    currentDuration: Long, // in Milliseconds
    onDismiss: () -> Unit,
    onDurationSelected: (Long) -> Unit
) {
    // Label -> Milliseconds
    val options = listOf(
        "Off" to 0L,
        "5 seconds" to 5_000L,
        "10 seconds" to 10_000L,
        "30 seconds" to 30_000L,
        "1 minute" to 60_000L,
        "5 minutes" to 300_000L,
        "30 minutes" to 1_800_000L,
        "1 hour" to 3_600_000L,
        "6 hours" to 21_600_000L,
        "12 hours" to 43_200_000L,
        "1 day" to 86_400_000L,
        "1 week" to 604_800_000L
    )

    var selectedOption by remember { mutableStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disappearing messages") },
        text = {
            Column {
                Text(
                    "Messages in this chat will automatically disappear after the selected time.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(options) { (label, duration) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = duration }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedOption == duration),
                                onClick = { selectedOption = duration }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDurationSelected(selectedOption)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}