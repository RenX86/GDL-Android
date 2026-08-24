package com.renx86.gdlapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---- NEOBRUTALISM COLORS ----
val NeoBackground = Color(0xFFFFFDF5) // Warm off-white
val NeoYellow = Color(0xFFFFD800)
val NeoPink = Color(0xFFFF8AE2)
val NeoBlue = Color(0xFF8AE2FF)
val NeoGreen = Color(0xFF8AFF8A)
val NeoOrange = Color(0xFFFF9040)
val NeoBorder = Color.Black

// ---- MODIFIER FOR THE HARD SHADOW & BORDER ----
fun Modifier.neoBrutalist(
    backgroundColor: Color,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 6.dp,
    shadowColor: Color = NeoBorder
): Modifier = this
    .drawBehind {
        // Draw the solid shadow block offset to the bottom right
        drawRect(
            color = shadowColor,
            topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
            size = size
        )
    }
    .background(backgroundColor)
    .border(borderWidth, NeoBorder)

// ---- CUSTOM NEO BUTTON ----
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeoYellow,
    enabled: Boolean = true
) {
    // Detect if button is pressed to create a satisfying "click down" effect
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // When pressed, the shadow disappears and the button shifts down/right
    val offset = if (isPressed && enabled) 6.dp else 0.dp
    val shadow = if (isPressed && enabled) 0.dp else 6.dp

    Box(
        modifier = modifier
            .offset(x = offset, y = offset) // Move the button itself
            .neoBrutalist(
                backgroundColor = if (enabled) color else Color.LightGray, 
                shadowOffset = shadow
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = NeoBorder,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// ---- CUSTOM NEO TEXT FIELD ----
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Instead of using alpha (which makes it transparent and shows the black shadow underneath),
    // we use a solid, pale yellow color for the focused state.
    val bgColor = if (isFocused) Color(0xFFFFF7C2) else Color.White
    val shadow = if (isFocused) 8.dp else 6.dp

    Box(
        modifier = modifier
            .neoBrutalist(backgroundColor = bgColor, shadowOffset = shadow)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = NeoBorder,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(NeoBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}
