package com.example.darts.ui.screens.score_entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import com.example.darts.ui.CardActive
import com.example.darts.ui.CardInactive
import com.example.darts.ui.MethodBarBg
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.LimeSecondary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.viewModel.states.PlayerDisplayState

// ── Local palette ─────────────────────────────────────────────────────────────
private val BtnSurface = Color(0xFF252B26)
private val BtnText    = Color(0xFFEEF2EE)
private val MultiplierBg = Color(0xFF1C221D)
private val MissColor  = LimePrimary
private val BullColor  = LimePrimary
private val UndoBg     = Color(0xFF1E2620)

// ─────────────────────────────────────────────────────────────────────────────
// Board Buttons Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardButtonsEntry(
    dartsEntered: Int,
    onDartAdded: (DartThrow) -> Unit,
    onUndo: () -> Unit,
    onSwitchToCamera: (() -> Unit)? = null,   // kept in signature, no longer rendered
    modifier: Modifier = Modifier
) {
    var multiplier by remember { mutableStateOf(Multiplier.SINGLE) }

    fun addAndReset(dart: DartThrow) {
        onDartAdded(dart)
        multiplier = Multiplier.SINGLE
    }

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        DartProgressDots(dartsEntered = dartsEntered)

        Spacer(Modifier.height(10.dp))

        MultiplierSelector(
            selected = multiplier,
            onSelect = { multiplier = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Number grid 1–20
        (1..20).chunked(5).forEach { row ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                row.forEach { num ->
                    NumberButton(
                        number     = num,
                        multiplier = multiplier,
                        onClick    = { addAndReset(DartThrow(num, multiplier)) },
                        modifier   = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        Spacer(Modifier.height(4.dp))

        // Special + Undo row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            SpecialDartButton(
                label    = "25",
                sublabel = "BULL",
                color    = BullColor,
                modifier = Modifier.weight(1f),
                onClick  = { addAndReset(DartThrow(25, Multiplier.SINGLE)) }
            )
            SpecialDartButton(
                label    = "50",
                sublabel = "D-BULL",
                color    = BullColor,
                modifier = Modifier.weight(1f),
                onClick  = { addAndReset(DartThrow(25, Multiplier.DOUBLE)) }
            )
            SpecialDartButton(
                label    = "0",
                sublabel = "MISS",
                color    = MissColor,
                modifier = Modifier.weight(1f),
                onClick  = { addAndReset(DartThrow(0, Multiplier.SINGLE)) }
            )
            UndoButton(
                onClick  = onUndo,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(Modifier.height(6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dart progress indicators
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DartProgressDots(dartsEntered: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { idx ->
            val filled = idx < dartsEntered
            val scale by animateFloatAsState(
                targetValue   = if (filled) 1.2f else 1f,
                animationSpec = tween(150),
                label         = "dot_scale_$idx"
            )
            Box(
                modifier         = Modifier.scale(scale),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    // Filled dart slot → lime arrow
                    ArrowIndicator(color = LimePrimary, size = 14.dp)
                } else {
                    // Empty → dim dash
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2E3830))
                    )
                }
            }
            if (idx < 2) Spacer(Modifier.width(12.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Arrow indicator — right-pointing filled triangle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArrowIndicator(
    color: Color,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(path = path, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Multiplier selector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MultiplierSelector(
    selected: Multiplier,
    onSelect: (Multiplier) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(22.dp),
        color    = MultiplierBg
    ) {
        Row(
            modifier              = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Multiplier.SINGLE to "SINGLE",
                Multiplier.DOUBLE to "DOUBLE",
                Multiplier.TRIPLE to "TRIPLE"
            ).forEach { (mult, label) ->
                val isSelected = selected == mult
                val bgColor by animateColorAsState(
                    targetValue   = if (isSelected) LimePrimary else Color.Transparent,
                    animationSpec = tween(200),
                    label         = "mult_bg_$label"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(bgColor)
                        .clickable { onSelect(mult) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = label,
                        color         = if (isSelected) Color(0xFF0B0F0C) else TextSecondary,
                        fontSize      = 12.sp,
                        fontWeight    = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NumberButton(
    number: Int,
    multiplier: Multiplier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isModified  = multiplier != Multiplier.SINGLE
    val computedVal = number * multiplier.factor
    val btnColor    = BtnSurface  // Always use default surface

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(btnColor)
            .then(
                if (isModified) Modifier.border(
                    width = 1.5.dp,
                    color = if (multiplier == Multiplier.TRIPLE) Color(0xFFFF5336) else LimePrimary,
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = number.toString(),
                color      = when {
                    multiplier == Multiplier.TRIPLE -> Color(0xFFFF5336)
                    isModified -> LimePrimary
                    else -> BtnText
                },
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (isModified) {
                Text(
                    text       = "=$computedVal",
                    color      = when {
                        multiplier == Multiplier.TRIPLE -> Color(0xFFFF5336)
                        else -> LimeSecondary.copy(alpha = 0.8f)
                    },
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Special dart button (25 / BULL / MISS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpecialDartButton(
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = label,
                color      = color,
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text          = sublabel,
                color         = color.copy(alpha = 0.65f),
                fontSize      = 8.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Undo button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UndoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(UndoBg)
            .border(1.dp, Color(0xFF2A3530), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Undo,
            contentDescription = "Undo",
            tint               = TextSecondary,
            modifier           = Modifier.size(26.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerCardMinimal(
    player: PlayerDisplayState,
    modifier: Modifier = Modifier
) {
    val isCurrent = player.isCurrent

    val cardBg = CardInactive  // Always use inactive background

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .then(
                if (isCurrent) Modifier.border(
                    width = 2.dp,
                    color = LimePrimary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Name + arrow turn indicator
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isCurrent) {
                    ArrowIndicator(
                        color    = LimePrimary,
                        size     = 9.dp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text          = player.name.uppercase(),
                    color         = Color.White,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text       = "${player.legsWon} LEG${if (player.legsWon != 1) "S" else ""}",
                color      = Color.White.copy(alpha = if (isCurrent) 0.80f else 0.45f),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text       = player.primaryScore,
                color      = Color.White,
                fontSize   = 42.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp
            )

            player.stats.take(2).forEach { stat ->
                Text(
                    text      = "${stat.label}: ${stat.value}",
                    color     = Color.White.copy(alpha = if (isCurrent) 0.65f else 0.35f),
                    fontSize  = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry method bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EntryMethodBar(
    methods: List<EntryMethod>,
    selectedMethod: EntryMethod,
    onSelect: (EntryMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color    = MethodBarBg,
        modifier = modifier
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            methods.forEach { method ->
                val isSelected = method == selectedMethod
                val bgColor by animateColorAsState(
                    targetValue   = if (isSelected) LimePrimary.copy(alpha = 0.15f) else Color.Transparent,
                    animationSpec = tween(200),
                    label         = "method_bg_${method.label}"
                )
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onSelect(method) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = methodIcon(method),
                        contentDescription = method.label,
                        tint               = if (isSelected) LimePrimary else TextSecondary,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = method.label,
                        color      = if (isSelected) LimePrimary else TextSecondary,
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Score input entry (keyboard mode)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreInputEntry(onScoreEntered: (DartThrow) -> Unit) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value         = input,
            onValueChange = { if (it.length <= 3) input = it.filter { c -> c.isDigit() } },
            label         = { Text("Enter score") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )
        Button(
            onClick  = {
                val score = input.toIntOrNull() ?: return@Button
                onScoreEntered(DartThrow(score, Multiplier.SINGLE))
                input = ""
            },
            enabled  = input.isNotEmpty(),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = LimePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Score", fontWeight = FontWeight.Bold, color = Color(0xFF0B0F0C))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun methodIcon(method: EntryMethod): ImageVector = when (method) {
    is EntryMethod.BoardButtons -> Icons.Default.GridOn
    is EntryMethod.ScoreInput   -> Icons.Default.Keyboard
    is EntryMethod.Voice        -> Icons.Default.Mic
    is EntryMethod.Camera       -> Icons.Default.Videocam
}

private val Multiplier.factor: Int
    get() = when (this) {
        Multiplier.SINGLE -> 1
        Multiplier.DOUBLE -> 2
        Multiplier.TRIPLE -> 3
    }