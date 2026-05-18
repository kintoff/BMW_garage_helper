package pl.garage.bmwassistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.StatusPill
import pl.garage.bmwassistant.ui.components.AccentBlue
import pl.garage.bmwassistant.ui.components.AccentGreen
import pl.garage.bmwassistant.ui.components.AccentPurple
import pl.garage.bmwassistant.ui.components.AccentYellow
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun GarageDashboard(
    vehicles: List<Vehicle>,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Vehicle) -> Unit,
    onCopyVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
) {
    var vehicleWithOpenOptions by remember { mutableStateOf<Vehicle?>(null) }

    vehicleWithOpenOptions?.let { vehicle ->
        VehicleOptionsDialog(
            vehicle = vehicle,
            onCopy = {
                onCopyVehicle(vehicle)
                vehicleWithOpenOptions = null
            },
            onDelete = {
                onDeleteVehicle(vehicle)
                vehicleWithOpenOptions = null
            },
            onDismiss = { vehicleWithOpenOptions = null }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF10283A), Color(0xFF06111A), Color(0xFF03090E)),
                        center = Offset(140f, 180f),
                        radius = 900f
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 26.dp, top = 42.dp, end = 26.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mój garaż",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(onClick = onAddVehicle),
                            shape = CircleShape,
                            color = Color(0xFF1B2A38)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Moje auta")
                }

                if (vehicles.isEmpty()) {
                    item { AddVehicleWideCard(onClick = onAddVehicle) }
                } else {
                    items(vehicles.take(1)) { vehicle ->
                        GarageVehicleCard(
                            vehicle = vehicle,
                            onOpen = { onOpenVehicle(vehicle) },
                            onLongPress = { vehicleWithOpenOptions = vehicle }
                        )
                    }
                    item { AddVehicleWideCard(onClick = onAddVehicle) }
                }

                item { SectionTitle("Szybki dostęp") }
                item { QuickAccessGrid() }
            }
            GarageHomeBottomNav(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GarageVehicleCard(
    vehicle: Vehicle,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13232F).copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(vehicleIconFor(vehicle)),
                    contentDescription = vehicle.displayName.ifBlank { "BMW" },
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "›",
                    fontSize = 38.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )
            }
            Text(
                text = vehicle.displayName.ifBlank { "BMW E61 520d" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = vehicle.technicalSummary.ifBlank { "M47N2 2.0d  •  2006  •  285 000 km" },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AddVehicleWideCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(
                        width = 1.3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+  Dodaj auto",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QuickAccessGrid() {
    val items = listOf(
        QuickAccessItem("Naprawy", "3 aktywne", AccentYellow, GarageHomeIcon.Wrench),
        QuickAccessItem("Części", "2 do kupienia", AccentGreen, GarageHomeIcon.Box),
        QuickAccessItem("Dokumenty", "12 plików", AccentPurple, GarageHomeIcon.Document),
        QuickAccessItem("Notatki", "5 notatek", AccentBlue, GarageHomeIcon.Note)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false
    ) {
        gridItems(items) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13232F).copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GarageLineIcon(
                        icon = item.icon,
                        color = item.color,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = item.subtitle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageHomeBottomNav(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF07111A).copy(alpha = 0.96f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            QuickAccessItem("Garaż", "", AccentBlue, GarageHomeIcon.Garage),
            QuickAccessItem("Naprawy", "", Color.White.copy(alpha = 0.55f), GarageHomeIcon.Wrench),
            QuickAccessItem("Części", "", Color.White.copy(alpha = 0.55f), GarageHomeIcon.Box),
            QuickAccessItem("Dokumenty", "", Color.White.copy(alpha = 0.55f), GarageHomeIcon.Document),
            QuickAccessItem("Profil", "", Color.White.copy(alpha = 0.55f), GarageHomeIcon.Profile)
        )
        items.forEach { item ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GarageLineIcon(
                    icon = item.icon,
                    color = item.color,
                    modifier = Modifier.size(23.dp)
                )
                Text(
                    text = item.title,
                    color = item.color,
                    fontSize = 11.sp,
                    fontWeight = if (item.title == "Garaż") FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

private data class QuickAccessItem(
    val title: String,
    val subtitle: String,
    val color: Color,
    val icon: GarageHomeIcon,
)

private enum class GarageHomeIcon {
    Garage,
    Wrench,
    Box,
    Document,
    Note,
    Profile
}

@Composable
private fun GarageLineIcon(
    icon: GarageHomeIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            GarageHomeIcon.Wrench -> {
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
            GarageHomeIcon.Box -> {
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
            GarageHomeIcon.Document -> {
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
            GarageHomeIcon.Note -> {
                val note = Path().apply {
                    moveTo(w * 0.16f, h * 0.14f)
                    lineTo(w * 0.8f, h * 0.14f)
                    lineTo(w * 0.8f, h * 0.64f)
                    lineTo(w * 0.62f, h * 0.82f)
                    lineTo(w * 0.16f, h * 0.82f)
                    close()
                }
                drawPath(note, color, style = stroke)
                drawLine(color, Offset(w * 0.3f, h * 0.36f), Offset(w * 0.66f, h * 0.36f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.3f, h * 0.53f), Offset(w * 0.6f, h * 0.53f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.82f), Offset(w * 0.62f, h * 0.64f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.64f), Offset(w * 0.8f, h * 0.64f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
            }
            GarageHomeIcon.Garage -> {
                val roof = Path().apply {
                    moveTo(w * 0.14f, h * 0.43f)
                    lineTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.43f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(color, topLeft = Offset(w * 0.22f, h * 0.43f), size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.4f), cornerRadius = CornerRadius(w * 0.04f), style = stroke)
                drawRoundRect(color, topLeft = Offset(w * 0.34f, h * 0.58f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.18f), cornerRadius = CornerRadius(w * 0.04f), style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.41f, h * 0.76f))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.59f, h * 0.76f))
            }
            GarageHomeIcon.Profile -> {
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

@Composable
private fun VehicleOptionsDialog(
    vehicle: Vehicle,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(vehicle.displayName.ifBlank { "Opcje auta" }) },
        text = {
            Text("Wybierz akcje dla tego kafelka.")
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("Skopiuj auto")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Usun auto")
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

@Composable
fun DeleteVehicleDialog(
    vehicle: Vehicle,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usunac profil auta?") },
        text = {
            Text(
                text = "Profil ${vehicle.displayName.ifBlank { "BMW" }} zostanie usuniety z aktualnej listy. W kolejnym kroku podepniemy to pod lokalna baze danych."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Usun")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun VehicleTileGrid(
    vehicles: List<Vehicle>,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Vehicle) -> Unit,
    onShowVehicleOptions: (Vehicle) -> Unit,
) {
    val tileCount = vehicles.size + 1
    val rowCount = (tileCount + 1) / 2

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 178).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        gridItems(vehicles) { vehicle ->
            VehicleTile(
                vehicle = vehicle,
                onOpen = { onOpenVehicle(vehicle) },
                onLongPress = { onShowVehicleOptions(vehicle) }
            )
        }
        item {
            AddVehicleTile(onClick = onAddVehicle)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VehicleTile(
    vehicle: Vehicle,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(vehicleIconFor(vehicle)),
                    contentDescription = vehicle.displayName.ifBlank { "BMW" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = vehicle.displayName.ifBlank { "BMW" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = vehicle.technicalSummary.ifBlank { "Dane do uzupelnienia" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(vehicle.generation.ifBlank { "Auto" })
            }
        }
    }
}

@Composable
private fun AddVehicleTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "+",
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Dodaj auto",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun vehicleIconFor(vehicle: Vehicle): Int {
    val searchText = "${vehicle.model} ${vehicle.generation}".uppercase()
    return when {
        "E61" in searchText -> R.drawable.car_bmw_e61
        else -> R.drawable.car_bmw_e61
    }
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun GarageDashboardPreview() {
    GarageTheme {
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBAXXXXXXXXXXXXXX",
            mileage = "285000",
            note = "Tylna zwrotnica lewa, zardzewiala sruba"
        )

        GarageDashboard(
            vehicles = listOf(vehicle),
            onAddVehicle = {},
            onOpenVehicle = {},
            onCopyVehicle = {},
            onDeleteVehicle = {}
        )
    }
}
