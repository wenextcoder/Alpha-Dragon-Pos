package com.alphadragon.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alphadragon.pos.ui.theme.BrandRed
import com.alphadragon.pos.ui.theme.SurfaceContainer

/**
 * Row of [pinLength] square PIN blocks. Empty blocks show a faded "0" placeholder.
 * Filled blocks show a white dot (digits are always masked).
 * Tapping anywhere on the row focuses the hidden input field.
 */
@Composable
fun PinBlockInput(
    value: String,
    onValueChange: (String) -> Unit,
    pinLength: Int = 6,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Box(modifier = modifier) {
        // Hidden field — captures keyboard, invisible to user
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { input ->
                if (input.length <= pinLength && input.all { it.isDigit() }) onValueChange(input)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() },
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(pinLength) { index ->
                val filled = index < value.length
                val isActive = index == value.length

                val blockColor = when {
                    isError -> MaterialTheme.colorScheme.errorContainer
                    else    -> SurfaceContainer
                }
                val borderColor = when {
                    isError  -> MaterialTheme.colorScheme.error
                    isActive -> BrandRed
                    filled   -> BrandRed
                    else     -> MaterialTheme.colorScheme.outline
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(blockColor)
                        .border(
                            width = if (isActive) 2.dp else 1.5.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (filled) {
                        // Filled: red dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(BrandRed, RoundedCornerShape(50))
                        )
                    } else {
                        // Empty: faded "0" placeholder
                        Text(
                            text = "0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
