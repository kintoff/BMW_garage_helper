package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.garage.bmwassistant.database.repository.GarageRepository
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.update.AppUpdateCheckResult
import pl.garage.bmwassistant.update.AppUpdateManager
import pl.garage.bmwassistant.update.AppUpdateRelease
import pl.garage.bmwassistant.update.DownloadUpdateResult
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.selectionImageResource
import pl.garage.bmwassistant.ui.theme.GarageTheme
import java.io.File

@Composable
fun GarageDashboard(
    vehicles: List<Vehicle>,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Vehicle) -> Unit,
    onCopyVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
) {
    var vehicleWithOpenOptions by remember { mutableStateOf<Vehicle?>(null) }
    val context = LocalContext.current
    val updateManager = remember { AppUpdateManager(context.applicationContext) }
    val garageRepository = remember { GarageRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val isInPreview = LocalInspectionMode.current
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var vehiclePendingBackup by remember { mutableStateOf<Vehicle?>(null) }
    var updateCardState by remember {
        mutableStateOf<AppUpdateCardState>(
            if (isInPreview || !updateManager.isConfigured()) {
                AppUpdateCardState.Hidden
            } else {
                AppUpdateCardState.Checking
            }
        )
    }

    val vehicleBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val vehicle = vehiclePendingBackup
        if (uri != null && vehicle != null) {
            coroutineScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            garageRepository.exportVehicleBackup(vehicle, output)
                        } == true
                    }.getOrDefault(false)
                }
                backupMessage = if (saved) {
                    "Backup auta zapisany do pliku."
                } else {
                    "Nie udalo sie zapisac backupu auta."
                }
                vehiclePendingBackup = null
            }
        } else {
            vehiclePendingBackup = null
        }
    }

    LaunchedEffect(updateManager, isInPreview) {
        if (isInPreview || !updateManager.isConfigured()) {
            updateCardState = AppUpdateCardState.Hidden
            return@LaunchedEffect
        }

        updateCardState = when (val result = withContext(Dispatchers.IO) { updateManager.checkForUpdate() }) {
            AppUpdateCheckResult.NotConfigured -> AppUpdateCardState.Hidden
            is AppUpdateCheckResult.UpToDate -> AppUpdateCardState.UpToDate(result.currentVersionName)
            is AppUpdateCheckResult.UpdateAvailable -> AppUpdateCardState.UpdateAvailable(result.release)
            is AppUpdateCheckResult.Error -> AppUpdateCardState.Error(result.message)
        }
    }

    vehicleWithOpenOptions?.let { vehicle ->
        VehicleOptionsDialog(
            vehicle = vehicle,
            onBackup = {
                vehiclePendingBackup = vehicle
                val safeTitle = vehicle.displayName
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .ifBlank { "auto" }
                vehicleBackupLauncher.launch("$safeTitle.bmwgarage")
                vehicleWithOpenOptions = null
            },
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

    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            title = { Text("Backup auta") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupMessage = null }) {
                    Text("OK")
                }
            }
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
                contentPadding = PaddingValues(start = 26.dp, top = 42.dp, end = 26.dp, bottom = 32.dp),
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

                if (updateCardState !is AppUpdateCardState.Hidden) {
                    item {
                        AppUpdateCard(
                            state = updateCardState,
                            onCheckAgain = {
                                coroutineScope.launch {
                                    updateCardState = AppUpdateCardState.Checking
                                    updateCardState = when (
                                        val result = withContext(Dispatchers.IO) {
                                            updateManager.checkForUpdate()
                                        }
                                    ) {
                                        AppUpdateCheckResult.NotConfigured -> AppUpdateCardState.Hidden
                                        is AppUpdateCheckResult.UpToDate -> AppUpdateCardState.UpToDate(result.currentVersionName)
                                        is AppUpdateCheckResult.UpdateAvailable -> AppUpdateCardState.UpdateAvailable(result.release)
                                        is AppUpdateCheckResult.Error -> AppUpdateCardState.Error(result.message)
                                    }
                                }
                            },
                            onDownload = { release ->
                                coroutineScope.launch {
                                    updateCardState = AppUpdateCardState.Downloading(release)
                                    updateCardState = when (
                                        val result = withContext(Dispatchers.IO) {
                                            updateManager.downloadUpdate(release)
                                        }
                                    ) {
                                        is DownloadUpdateResult.Success -> {
                                            if (updateManager.canRequestPackageInstalls()) {
                                                updateManager.launchInstaller(result.file)
                                            }
                                            AppUpdateCardState.ReadyToInstall(
                                                release = release,
                                                apkFile = result.file,
                                                installerPermissionGranted = updateManager.canRequestPackageInstalls()
                                            )
                                        }

                                        is DownloadUpdateResult.Error -> AppUpdateCardState.Error(result.message)
                                    }
                                }
                            },
                            onInstall = { apkFile ->
                                if (updateManager.canRequestPackageInstalls()) {
                                    updateManager.launchInstaller(apkFile)
                                } else {
                                    updateManager.openUnknownSourcesSettings()
                                    updateCardState = (updateCardState as? AppUpdateCardState.ReadyToInstall)
                                        ?.copy(installerPermissionGranted = false)
                                        ?: updateCardState
                                }
                            },
                            onOpenInstallerPermission = {
                                if (updateManager.openUnknownSourcesSettings()) {
                                    updateCardState = (updateCardState as? AppUpdateCardState.ReadyToInstall)
                                        ?.copy(installerPermissionGranted = updateManager.canRequestPackageInstalls())
                                        ?: updateCardState
                                }
                            }
                        )
                    }
                }

                if (vehicles.isEmpty()) {
                    item { AddVehicleWideCard(onClick = onAddVehicle) }
                } else {
                    items(vehicles, key = { it.id.ifBlank { it.vin.ifBlank { it.displayName } } }) { vehicle ->
                        GarageVehicleCard(
                            vehicle = vehicle,
                            onOpen = { onOpenVehicle(vehicle) },
                            onLongPress = { vehicleWithOpenOptions = vehicle }
                        )
                    }
                    item { AddVehicleWideCard(onClick = onAddVehicle) }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateCard(
    state: AppUpdateCardState,
    onCheckAgain: () -> Unit,
    onDownload: (AppUpdateRelease) -> Unit,
    onInstall: (File) -> Unit,
    onOpenInstallerPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13232F).copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Aktualizacja aplikacji",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (state) {
                AppUpdateCardState.Checking -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Sprawdzam najnowszy release na GitHub.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                    }
                }

                is AppUpdateCardState.UpToDate -> {
                    Text(
                        text = "Masz aktualna wersje ${state.currentVersionName}.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                    TextButton(onClick = onCheckAgain) {
                        Text("Sprawdz ponownie")
                    }
                }

                is AppUpdateCardState.UpdateAvailable -> {
                    Text(
                        text = "Dostepna jest wersja ${state.release.versionName}. APK zostanie pobrany z GitHub Releases.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                    state.release.publishedAt.takeIf(String::isNotBlank)?.let { publishedAt ->
                        Text(
                            text = "Publikacja: $publishedAt",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    }
                    Button(onClick = { onDownload(state.release) }) {
                        Text("Pobierz aktualizacje")
                    }
                }

                is AppUpdateCardState.Downloading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Pobieram APK wersji ${state.release.versionName}.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                    }
                }

                is AppUpdateCardState.ReadyToInstall -> {
                    Text(
                        text = "APK wersji ${state.release.versionName} jest juz pobrany.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                    if (state.installerPermissionGranted) {
                        Button(onClick = { onInstall(state.apkFile) }) {
                            Text("Zainstaluj aktualizacje")
                        }
                    } else {
                        Text(
                            text = "Android wymaga jednorazowego wlaczenia instalacji z tej aplikacji.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                        Button(onClick = onOpenInstallerPermission) {
                            Text("Wlacz instalacje APK")
                        }
                    }
                }

                is AppUpdateCardState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onCheckAgain) {
                        Text("Sprobuj ponownie")
                    }
                }

                AppUpdateCardState.Hidden -> Unit
            }
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
                    painter = painterResource(vehicle.selectionImageResource()),
                    contentDescription = vehicle.displayName.ifBlank { "BMW" },
                    modifier = Modifier
                        .weight(1f)
                        .height(170.dp),
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
private fun VehicleOptionsDialog(
    vehicle: Vehicle,
    onBackup: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(vehicle.displayName.ifBlank { "Opcje auta" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wybierz akcje dla tego kafelka.")
                TextButton(onClick = onBackup) {
                    Text("Backup auta")
                }
                TextButton(onClick = onCopy) {
                    Text("Skopiuj auto")
                }
            }
        },
        confirmButton = {},
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
                text = "Profil ${vehicle.displayName.ifBlank { "BMW" }} zostanie usuniety razem z lokalna baza i plikami tego auta."
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

private sealed interface AppUpdateCardState {
    data object Hidden : AppUpdateCardState
    data object Checking : AppUpdateCardState
    data class UpToDate(val currentVersionName: String) : AppUpdateCardState
    data class UpdateAvailable(val release: AppUpdateRelease) : AppUpdateCardState
    data class Downloading(val release: AppUpdateRelease) : AppUpdateCardState
    data class ReadyToInstall(
        val release: AppUpdateRelease,
        val apkFile: File,
        val installerPermissionGranted: Boolean,
    ) : AppUpdateCardState

    data class Error(val message: String) : AppUpdateCardState
}
