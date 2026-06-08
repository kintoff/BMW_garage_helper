package pl.garage.bmwassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AccentBlue = Color(0xFF4FB6FF)
val AccentYellow = Color(0xFFFFB51F)
val AccentGreen = Color(0xFF3ED66F)
val AccentRed = Color(0xFFFF5757)
val AccentPurple = Color(0xFFA77DFF)

@Composable
fun GaragePanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color = statusColorFor(text),
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SegmentTabs(
    tabs: List<String>,
    selectedTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f), RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = tab,
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                    color = if (isSelected) AccentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item == selectedItem
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(item) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                GarageNavLineIcon(
                    icon = navIconFor(item),
                    color = if (selected) AccentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = item,
                    color = if (selected) AccentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MetricBlock(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun RepairProgress(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (total <= 0) 0f else completed.toFloat() / total.toFloat()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = AccentBlue,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$completed/$total krokow",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
        }
    }
}

fun statusColorFor(status: String): Color {
    val key = status.lowercase()
    return when {
        "zakon" in key -> AccentGreen
        "plan" in key -> AccentBlue
        "problem" in key || "piln" in key -> AccentRed
        "trak" in key || "akty" in key || "wysoki" in key -> AccentYellow
        else -> AccentBlue
    }
}

private enum class GarageNavIcon {
    Garage,
    Wrench,
    Box,
    Document,
    Profile
}

private fun navIconFor(item: String): GarageNavIcon = when (item) {
    "Przeglad" -> GarageNavIcon.Garage
    "Naprawy" -> GarageNavIcon.Wrench
    "Czesci" -> GarageNavIcon.Box
    "Dokumenty" -> GarageNavIcon.Document
    "Wiecej" -> GarageNavIcon.Profile
    else -> GarageNavIcon.Garage
}

@Composable
private fun GarageNavLineIcon(
    icon: GarageNavIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            GarageNavIcon.Wrench -> {
                drawLine(color, Offset(w * 0.23f, h * 0.78f), Offset(w * 0.62f, h * 0.39f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.2f, h * 0.84f), Offset(w * 0.32f, h * 0.72f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                val jaw = Path().apply {
                    moveTo(w * 0.64f, h * 0.37f)
                    cubicTo(w * 0.58f, h * 0.21f, w * 0.7f, h * 0.08f, w * 0.86f, h * 0.14f)
                    lineTo(w * 0.74f, h * 0.27f)
                    lineTo(w * 0.82f, h * 0.36f)
                    lineTo(w * 0.95f, h * 0.25f)
                    cubicTo(w * 0.98f, h * 0.42f, w * 0.84f, h * 0.54f, w * 0.68f, h * 0.48f)
                }
                drawPath(jaw, color, style = stroke)
            }
            GarageNavIcon.Box -> {
                val top = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.86f, h * 0.3f)
                    lineTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.14f, h * 0.3f)
                    close()
                }
                val left = Path().apply {
                    moveTo(w * 0.14f, h * 0.3f)
                    lineTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.14f, h * 0.68f)
                    close()
                }
                val right = Path().apply {
                    moveTo(w * 0.86f, h * 0.3f)
                    lineTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.86f, h * 0.68f)
                    close()
                }
                drawPath(top, color, style = stroke)
                drawPath(left, color, style = stroke)
                drawPath(right, color, style = stroke)
            }
            GarageNavIcon.Document -> {
                val page = Path().apply {
                    moveTo(w * 0.23f, h * 0.1f)
                    lineTo(w * 0.62f, h * 0.1f)
                    lineTo(w * 0.78f, h * 0.27f)
                    lineTo(w * 0.78f, h * 0.88f)
                    lineTo(w * 0.23f, h * 0.88f)
                    close()
                }
                drawPath(page, color, style = stroke)
                drawLine(color, Offset(w * 0.62f, h * 0.1f), Offset(w * 0.62f, h * 0.28f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.28f), Offset(w * 0.78f, h * 0.28f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.44f), Offset(w * 0.62f, h * 0.44f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.59f), Offset(w * 0.62f, h * 0.59f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.74f), Offset(w * 0.54f, h * 0.74f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
            }
            GarageNavIcon.Garage -> {
                val roof = Path().apply {
                    moveTo(w * 0.14f, h * 0.43f)
                    lineTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.43f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(color, topLeft = Offset(w * 0.22f, h * 0.43f), size = Size(w * 0.56f, h * 0.4f), cornerRadius = CornerRadius(w * 0.04f), style = stroke)
                drawRoundRect(color, topLeft = Offset(w * 0.34f, h * 0.58f), size = Size(w * 0.32f, h * 0.18f), cornerRadius = CornerRadius(w * 0.04f), style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.41f, h * 0.76f))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.59f, h * 0.76f))
            }
            GarageNavIcon.Profile -> {
                drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.28f), style = stroke)
                val body = Path().apply {
                    moveTo(w * 0.25f, h * 0.86f)
                    cubicTo(w * 0.25f, h * 0.61f, w * 0.75f, h * 0.61f, w * 0.75f, h * 0.86f)
                }
                drawPath(body, color, style = stroke)
            }
        }
    }
}
