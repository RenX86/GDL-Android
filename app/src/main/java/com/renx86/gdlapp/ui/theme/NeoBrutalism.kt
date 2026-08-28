package com.renx86.gdlapp.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.composed

// ---- NEOBRUTALISM COLORS ----
val NeoBackground: Color @Composable get() = NeoTheme.colors.background
val NeoYellow: Color @Composable get() = NeoTheme.colors.yellow
val NeoPink: Color @Composable get() = NeoTheme.colors.pink
val NeoBlue: Color @Composable get() = NeoTheme.colors.blue
val NeoGreen: Color @Composable get() = NeoTheme.colors.green
val NeoOrange: Color @Composable get() = NeoTheme.colors.orange
val NeoBorder: Color @Composable get() = NeoTheme.colors.border
val NeoText: Color @Composable get() = NeoTheme.colors.text
val NeoTextSecondary: Color @Composable get() = NeoTheme.colors.textSecondary

// ---- MODIFIER FOR THE HARD SHADOW & BORDER ----
fun Modifier.neoBrutalist(
    backgroundColor: Color,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 6.dp,
    shadowColor: Color? = null
): Modifier = composed {
    val actualShadowColor = shadowColor ?: NeoTheme.colors.shadow
    val actualBorderColor = NeoBorder
    this
        .drawBehind {
            drawRect(
                color = actualShadowColor,
                topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                size = size
            )
        }
        .background(backgroundColor)
        .border(borderWidth, actualBorderColor)
}

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
    val targetOffset = if (isPressed && enabled) 6.dp else 0.dp
    val targetShadow = if (isPressed && enabled) 0.dp else 6.dp
    
    val offset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "btnOffset"
    )
    val shadow by animateDpAsState(
        targetValue = targetShadow,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "btnShadow"
    )

    val bgColor by animateColorAsState(
        targetValue = if (enabled) color else NeoTheme.colors.surface,
        animationSpec = tween(150),
        label = "btnBgColor"
    )

    Box(
        modifier = modifier
            .offset(x = offset, y = offset) // Move the button itself
            .neoBrutalist(
                backgroundColor = bgColor, 
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
    
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) NeoTheme.colors.yellow else NeoTheme.colors.surface,
        animationSpec = tween(200),
        label = "tfBgColor"
    )
    val shadow by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 6.dp,
        animationSpec = tween(200),
        label = "tfShadow"
    )

    Box(
        modifier = modifier
            .neoBrutalist(backgroundColor = bgColor, shadowOffset = shadow)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = NeoTextSecondary, fontWeight = FontWeight.Bold)
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

// ---- COLLAPSIBLE NEO CARD ----
@Composable
fun NeoCollapsibleCard(
    title: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    titleTrailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .neoBrutalist(backgroundColor = backgroundColor, shadowOffset = 8.dp)
    ) {
        Column {
            // Header row — always visible, clickable to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = NeoBorder
                )
                if (titleTrailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    titleTrailing()
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "▼",
                    fontSize = 20.sp,
                    color = NeoBorder,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                )
            }

            // Animated content area
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = tween(250)
                ),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = tween(250)
                )
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    content = content
                )
            }
        }
    }
}

// ---- SEGMENTED TOGGLE ----
@Composable
fun <T> NeoSegmentedToggle(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) NeoYellow else NeoTheme.colors.surface,
                animationSpec = tween(150),
                label = "toggleBgColor"
            )
            val borderWidth by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 2.dp,
                animationSpec = tween(150),
                label = "toggleBorder"
            )
            val shadowOffset by animateDpAsState(
                targetValue = if (isSelected) 0.dp else 4.dp,
                animationSpec = tween(150),
                label = "toggleShadow"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .neoBrutalist(
                        backgroundColor = bgColor,
                        borderWidth = borderWidth,
                        shadowOffset = shadowOffset
                    )
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeoBorder
                )
            }
        }
    }
}
