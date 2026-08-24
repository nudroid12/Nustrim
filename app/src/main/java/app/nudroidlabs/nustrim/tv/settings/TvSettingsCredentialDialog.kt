package app.nudroidlabs.nustrim.tv.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun TvSettingsCredentialDialog(
    editor: TvSettingsEditor,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onTestOrConnect: (String, String) -> Unit,
) {
    val initialFirst = when (editor) {
        is TvSettingsEditor.Tmdb -> editor.credential
        is TvSettingsEditor.MdbList -> editor.apiKey
        is TvSettingsEditor.Trakt -> editor.clientId
    }
    val initialSecond = (editor as? TvSettingsEditor.Trakt)?.clientSecret.orEmpty()
    var first by remember(editor) { mutableStateOf(initialFirst) }
    var second by remember(editor) { mutableStateOf(initialSecond) }
    val requester = remember { FocusRequester() }

    LaunchedEffect(editor) {
        delay(120)
        runCatching { requester.requestFocus() }
    }

    val title = when (editor) {
        is TvSettingsEditor.Tmdb -> "TMDB credential"
        is TvSettingsEditor.MdbList -> "MDBList API key"
        is TvSettingsEditor.Trakt -> "Trakt application"
    }
    val firstLabel = when (editor) {
        is TvSettingsEditor.Tmdb -> "API key or read token"
        is TvSettingsEditor.MdbList -> "API key"
        is TvSettingsEditor.Trakt -> "Client ID"
    }
    val action = if (editor is TvSettingsEditor.Trakt) "Connect" else "Test"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Use the TV keyboard to enter your own credential. It is stored only on this device.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text(firstLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                )
                if (editor is TvSettingsEditor.Trakt) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = { Text("Client secret") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTestOrConnect(first.trim(), second.trim()) },
                enabled = first.isNotBlank() && (editor !is TvSettingsEditor.Trakt || second.isNotBlank()),
            ) { Text(action) }
        },
        dismissButton = {
            Column {
                OutlinedButton(onClick = { onSave(first.trim(), second.trim()) }) { Text("Save") }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
