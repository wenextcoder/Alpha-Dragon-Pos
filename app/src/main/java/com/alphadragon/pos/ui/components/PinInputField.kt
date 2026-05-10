package com.alphadragon.pos.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Full-width PIN field that looks identical to every other OutlinedTextField in the app.
 * Digits are masked by default; the trailing eye icon toggles visibility.
 */
@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    pinLength: Int = 6,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= pinLength && input.all { it.isDigit() }) onValueChange(input)
        },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide PIN" else "Show PIN"
                )
            }
        },
        modifier = modifier
    )
}
