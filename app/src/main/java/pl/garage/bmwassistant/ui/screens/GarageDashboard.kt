package pl.garage.bmwassistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.StatusPill
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header(
                    title = "Moj garaz",
                    subtitle = "Wybierz auto, a potem wejdziesz w jego naprawy, czesci, zdjecia i notatki."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Moje auta")
                    TextButton(onClick = onAddVehicle) {
                        Text("Dodaj")
                    }
                }
            }

            item {
                VehicleTileGrid(
                    vehicles = vehicles,
                    onAddVehicle = onAddVehicle,
                    onOpenVehicle = onOpenVehicle,
                    onShowVehicleOptions = { vehicleWithOpenOptions = it }
                )
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
        items(vehicles) { vehicle ->
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
