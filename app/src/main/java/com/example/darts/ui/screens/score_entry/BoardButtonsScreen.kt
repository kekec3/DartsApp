package com.example.darts.ui.screens.score_entry

import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.R
import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import com.example.darts.ui.screens.CardInactive
import com.example.darts.ui.screens.MethodBarBg
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.viewModel.states.PlayerDisplayState

// ── Local palette ─────────────────────────────────────────────────────────────
private val BtnSurface   = Color(0xFF1E2420)
private val BtnText      = Color(0xFFEEF2EE)
private val MultiplierBg = Color(0xFF141A15)
private val MissColor    = BtnSurface
private val BullColor    = Color(0xFFFF5336)
private val OuterColor   = LimePrimary
private val UndoBg       = Color(0xFF18201B)

// ─────────────────────────────────────────────────────────────────────────────
// Board Buttons Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardButtonsEntry(
    dartsEntered: Int,
    onDartAdded: (DartThrow) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var multiplier by remember { mutableStateOf(Multiplier.SINGLE) }

    fun addAndReset(dart: DartThrow) {
        Log.d("Cricket Mode", "Button Pressed")
        onDartAdded(dart)
        multiplier = Multiplier.SINGLE
    }

    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DartProgressDots(dartsEntered = dartsEntered)

        MultiplierSelector(
            selected = multiplier,
            onSelect = { multiplier = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // Number grid 1–20 (Weighted rows fill remaining vertical height)
        (1..20).chunked(5).forEach { row ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { num ->
                    NumberButton(
                        number     = num,
                        multiplier = multiplier,
                        onClick    = { addAndReset(DartThrow(num, multiplier)) },
                        modifier   = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Special + Undo row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialDartButton(
                label    = "25",
                sublabel = "OUTER",
                color    = OuterColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick  = { addAndReset(DartThrow(25, Multiplier.SINGLE)) }
            )
            SpecialDartButton(
                label    = "50",
                sublabel = "BULL",
                color    = BullColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick  = { addAndReset(DartThrow(25, Multiplier.DOUBLE)) }
            )
            SpecialDartButton(
                label    = "0",
                sublabel = "MISS",
                color    = MissColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick  = { addAndReset(DartThrow(0, Multiplier.SINGLE)) }
            )
            UndoButton(
                onClick  = onUndo,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dart progress indicators (Fixed-height container to prevent shift)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DartProgressDots(dartsEntered: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { idx ->
            val filled = idx < dartsEntered
            val scale by animateFloatAsState(
                targetValue   = if (filled) 1.15f else 1f,
                animationSpec = tween(150),
                label         = "dot_scale_$idx"
            )

            Box(
                modifier         = Modifier
                    .size(24.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dart),
                        contentDescription = "Dart available",
                        tint = LimePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2E3830))
                    )
                }
            }
            if (idx < 2) Spacer(Modifier.width(16.dp))
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
fun MultiplierSelector(
    selected: Multiplier,
    onSelect: (Multiplier) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
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
                val activeColor = if (mult == Multiplier.TRIPLE) Color(0xFFFF5336) else LimePrimary
                val bgColor by animateColorAsState(
                    targetValue   = if (isSelected) activeColor else Color.Transparent,
                    animationSpec = tween(180),
                    label         = "mult_bg_$label"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(bgColor)
                        .clickable { onSelect(mult) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = label,
                        color         = if (isSelected) Color(0xFF0B0F0C) else TextSecondary,
                        fontSize      = 12.sp,
                        fontWeight    = if (isSelected) FontWeight.Black else FontWeight.Bold,
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
fun NumberButton(
    number: Int,
    multiplier: Multiplier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isModified  = multiplier != Multiplier.SINGLE
    val computedVal = number * multiplier.factor
    val accentColor = if (multiplier == Multiplier.TRIPLE) Color(0xFFFF5336) else LimePrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isModified) accentColor.copy(alpha = 0.15f) else BtnSurface)
            .then(
                if (isModified) Modifier.border(
                    width = 1.5.dp,
                    color = accentColor,
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isModified) {
            Text(
                text       = number.toString(),
                color      = BtnText,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text       = number.toString(),
                    color      = accentColor,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text       = "=$computedVal",
                    color      = accentColor.copy(alpha = 0.9f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Special dart button (25 / BULL / MISS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpecialDartButton(
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isNeutral = color == BtnSurface || color == Color.Transparent
    val bg = if (isNeutral) BtnSurface else color.copy(alpha = 0.14f)
    val textColor = if (isNeutral) BtnText else color
    val subLabelColor = if (isNeutral) TextSecondary else color.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (!isNeutral) Modifier.border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = label,
                color      = textColor,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text          = sublabel,
                color         = subLabelColor,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Undo button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UndoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(UndoBg)
            .border(1.dp, Color(0xFF2A3530), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Undo,
                contentDescription = "Undo",
                tint               = TextSecondary,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = "UNDO",
                color      = TextSecondary,
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    val cardBg = CardInactive

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

private val Multiplier.factor: Int
    get() = when (this) {
        Multiplier.SINGLE -> 1
        Multiplier.DOUBLE -> 2
        Multiplier.TRIPLE -> 3
    }