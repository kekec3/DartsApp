package com.example.darts.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.darts.viewModel.PlayerStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsOverviewScreen(
    modifier: Modifier = Modifier,
    playerId: Int = -1,
    playerName: String = "Statistics",
    onBack: (() -> Unit)? = null,
    viewModel: PlayerStatsViewModel = hiltViewModel()
) {
    LaunchedEffect(playerId) {
        if (playerId != -1) viewModel.load(playerId)
    }

    val career by viewModel.careerStats.collectAsState()
    val legTrend by viewModel.legTrend.collectAsState()
    val stats = career

    // ── Derived values ──────────────────────────────────────────────────────
    val average = if ((stats?.totalDartsThrown ?: 0) > 0)
        stats!!.totalScored.toFloat() / stats.totalDartsThrown * 3
    else 0f

    val winRate = if ((stats?.matchesPlayed ?: 0) > 0)
        stats!!.matchesWon * 100f / stats.matchesPlayed
    else 0f

    val checkoutPct = if ((stats?.checkoutsAttempted ?: 0) > 0)
        stats!!.checkoutsHit * 100f / stats.checkoutsAttempted
    else 0f

    val maxScore = maxOf(
        stats?.scores180 ?: 0,
        stats?.scores140Plus ?: 0,
        stats?.scores100Plus ?: 0,
        1
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(playerName, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── KPI row 1 ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard("Matches Played", "${stats?.matchesPlayed ?: 0}", Modifier.weight(1f))
                StatKpiCard("Matches Won", "${stats?.matchesWon ?: 0}", Modifier.weight(1f))
                StatKpiCard(
                    label = "Win Rate",
                    value = "%.1f%%".format(winRate),
                    modifier = Modifier.weight(1f),
                    isHighlighted = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── KPI row 2 ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard("3-Dart Avg", "%.2f".format(average), Modifier.weight(1f))
                StatKpiCard("Highest Out", "${stats?.highestCheckout ?: 0}", Modifier.weight(1f))
                StatKpiCard("Checkout %", "%.1f%%".format(checkoutPct), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Trend chart ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "3-Dart Average Trend",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (legTrend.isNotEmpty()) {
                    Text(
                        "${legTrend.size} legs",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                AverageTrendChart(
                    averages = legTrend,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Scoring distribution ───────────────────────────────────────
            Text(
                "Scoring Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ScoreBreakdownRow(
                label = "180s",
                count = "${stats?.scores180 ?: 0}",
                percentage = (stats?.scores180 ?: 0).toFloat() / maxScore
            )
            ScoreBreakdownRow(
                label = "140+",
                count = "${stats?.scores140Plus ?: 0}",
                percentage = (stats?.scores140Plus ?: 0).toFloat() / maxScore
            )
            ScoreBreakdownRow(
                label = "100+",
                count = "${stats?.scores100Plus ?: 0}",
                percentage = (stats?.scores100Plus ?: 0).toFloat() / maxScore
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Detail card ────────────────────────────────────────────────
            Text(
                "Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val rows = listOf(
                        "Legs Played"         to "${stats?.legsPlayed         ?: 0}",
                        "Legs Won"            to "${stats?.legsWon            ?: 0}",
                        "Checkouts Hit"       to "${stats?.checkoutsHit       ?: 0}",
                        "Checkouts Attempted" to "${stats?.checkoutsAttempted ?: 0}"
                    )
                    rows.forEachIndexed { index, (label, value) ->
                        PlayerStatItem(label, value)
                        if (index < rows.lastIndex) {
                            HorizontalDivider(
                                color = Color.DarkGray.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Chart ─────────────────────────────────────────────────────────────────────

/**
 * Canvas-drawn line chart for per-leg 3-dart averages.
 *
 * Features:
 *  - Auto-scaled Y axis with a margin so values never sit on the edge
 *  - 4 horizontal grid lines with Y labels (native canvas text — no extra dep)
 *  - Gradient area fill beneath the trend line
 *  - Data-point dots shown when ≤ 20 legs (hides them when the chart gets dense)
 *  - X-axis labels: first leg, middle (if ≥ 6 legs), last leg
 *  - "No data" placeholder for < 2 points
 */
@Composable
fun AverageTrendChart(
    averages: List<Float>,
    modifier: Modifier = Modifier
) {
    if (averages.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = if (averages.isEmpty()) "No games played yet"
                else "Play more legs to see your trend",
                color = Color.DarkGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    val showDots = averages.size <= 20

    // Y-axis bounds — margin ensures the line never hugs the top/bottom edge
    val minRaw = averages.min()
    val maxRaw = averages.max()
    val span   = (maxRaw - minRaw).coerceAtLeast(20f)
    val margin = span * 0.18f
    val yMin   = (minRaw - margin).coerceAtLeast(0f)
    val yMax   = maxRaw + margin
    val yRange = (yMax - yMin).coerceAtLeast(1f)

    val green     = Color(0xFF76B947)
    val gridColor = Color(0xFF272727)
    val dotInner  = Color(0xFF1A1A1A)

    Canvas(modifier = modifier) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        val leftPad   = 42.dp.toPx()
        val rightPad  =  4.dp.toPx()
        val topPad    =  4.dp.toPx()
        val bottomPad = 22.dp.toPx()

        val chartW = size.width  - leftPad - rightPad
        val chartH = size.height - topPad  - bottomPad
        val n      = averages.size

        fun xOf(i: Int)   = leftPad + chartW * i.toFloat() / (n - 1).toFloat()
        fun yOf(v: Float) = topPad  + chartH * (1f - (v - yMin) / yRange)

        val labelPx = 10.dp.toPx()

        val yLabelPaint = android.graphics.Paint().apply {
            setARGB(180, 0x77, 0x77, 0x77)
            textSize  = labelPx
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        // ── Grid lines + Y labels ────────────────────────────────────────
        val gridCount = 4
        for (i in 0..gridCount) {
            val fraction = i.toFloat() / gridCount
            val yPx   = topPad + chartH * (1f - fraction)
            val value = yMin + yRange * fraction

            drawLine(
                color = gridColor,
                start = Offset(leftPad, yPx),
                end   = Offset(leftPad + chartW, yPx),
                strokeWidth = 1f
            )

            nativeCanvas.drawText(
                "%.0f".format(value),
                leftPad - 5.dp.toPx(),
                yPx + labelPx * 0.35f,   // vertically centred on the grid line
                yLabelPaint
            )
        }

        // ── Gradient area fill ───────────────────────────────────────────
        val areaPath = Path().apply {
            moveTo(xOf(0), yOf(averages[0]))
            for (i in 1 until n) lineTo(xOf(i), yOf(averages[i]))
            lineTo(xOf(n - 1), topPad + chartH)
            lineTo(xOf(0),     topPad + chartH)
            close()
        }
        drawPath(
            path  = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(green.copy(alpha = 0.28f), Color.Transparent),
                startY = topPad,
                endY   = topPad + chartH
            )
        )

        // ── Trend line ───────────────────────────────────────────────────
        for (i in 0 until n - 1) {
            drawLine(
                color       = green,
                start       = Offset(xOf(i),     yOf(averages[i])),
                end         = Offset(xOf(i + 1), yOf(averages[i + 1])),
                strokeWidth = 2.2.dp.toPx(),
                cap         = StrokeCap.Round
            )
        }

        // ── Data-point dots ──────────────────────────────────────────────
        if (showDots) {
            averages.forEachIndexed { i, avg ->
                val cx = xOf(i)
                val cy = yOf(avg)
                drawCircle(color = green,    radius = 4.dp.toPx(),   center = Offset(cx, cy))
                drawCircle(color = dotInner, radius = 2.2.dp.toPx(), center = Offset(cx, cy))
            }
        }

        // ── X-axis labels ────────────────────────────────────────────────
        val xLabelPaint = android.graphics.Paint().apply {
            setARGB(180, 0x77, 0x77, 0x77)
            textSize  = labelPx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val xLabelY = topPad + chartH + 16.dp.toPx()

        nativeCanvas.drawText("1",  xOf(0),     xLabelY, xLabelPaint)
        nativeCanvas.drawText("$n", xOf(n - 1), xLabelY, xLabelPaint)

        if (n >= 6) {
            val mid = n / 2
            nativeCanvas.drawText("$mid", xOf(mid), xLabelY, xLabelPaint)
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
fun StatKpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier,
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = label.uppercase(),
                fontSize   = 9.sp,
                color      = Color.Gray,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                color      = if (isHighlighted) Color(0xFF76B947) else Color.White
            )
        }
    }
}

@Composable
fun ScoreBreakdownRow(label: String, count: String, percentage: Float) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.LightGray, fontSize = 14.sp)
            Text(count, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color(0xFF76B947), RoundedCornerShape(2.dp))
            )
        }
    }
}