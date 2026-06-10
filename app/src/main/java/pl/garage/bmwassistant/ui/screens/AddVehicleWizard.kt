package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.database.repository.GarageRepository
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.WizardCard
import pl.garage.bmwassistant.ui.theme.GarageTheme

private const val E61_CZESCIDOBMW_CATALOG_URL =
    "https://czescidobmw.pl/vin/YQBW6/48808/" +
        "\$" +
        "*KwHa7v-Xr6DYg7q23YSC9oKWtrGv293a24OOmtjRkLCYmpqs5PHopqa5oZa2np_H__Shs6-9rNvU2YOPx5afi4eBioKJ8cSBhYCKzcjTzs6Fh5vY0czDyMuaj5OfmcjLoKXb2MyRAAAAAFI-z5U%3D" +
        "\$"

@Composable
fun AddVehicleWizard(
    onVehicleCreated: (Vehicle) -> Unit,
    onCancel: (() -> Unit)? = null,
    initialVehicle: Vehicle? = null,
    title: String = "Wybierz auto do garazu",
    subtitle: String = "Na poczatek dodamy profil auta. Pozniej ten kafelek otworzy naprawy, zdjecia, czesci i dokumenty.",
    saveLabel: String = "Zapisz auto",
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val garageRepository = remember { GarageRepository(context.applicationContext) }
    var brand by rememberSaveable { mutableStateOf(initialVehicle?.brand ?: "BMW") }
    var model by rememberSaveable { mutableStateOf(initialVehicle?.model.orEmpty()) }
    var generation by rememberSaveable { mutableStateOf(initialVehicle?.generation.orEmpty()) }
    var engine by rememberSaveable { mutableStateOf(initialVehicle?.engine.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(initialVehicle?.year.orEmpty()) }
    var vin by rememberSaveable { mutableStateOf(initialVehicle?.vin.orEmpty()) }
    var mileage by rememberSaveable { mutableStateOf(initialVehicle?.mileage.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(initialVehicle?.note.orEmpty()) }
    var partsCatalogUrl by rememberSaveable { mutableStateOf(initialVehicle?.partsCatalogUrl.orEmpty()) }
    var importMessage by remember { mutableStateOf<String?>(null) }

    val canSave = model.isNotBlank() && engine.isNotBlank()
    val vehicleImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val importedVehicle = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            garageRepository.importVehicleBackup(input)
                        }
                    }.getOrNull()
                }
                if (importedVehicle != null) {
                    onVehicleCreated(importedVehicle)
                } else {
                    importMessage = "Nie udalo sie zaimportowac auta z pliku."
                }
            }
        }
    }

    importMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importMessage = null },
            title = { Text("Import auta") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { importMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("add_vehicle_wizard_list"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header(
                    title = title,
                    subtitle = subtitle
                )
            }

            if (initialVehicle == null) {
                item {
                    ImportVehicleCard(
                        onImport = {
                            vehicleImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }
                    )
                }
                item {
                    VehicleTemplateCard(
                        onUseTemplate = {
                            brand = "BMW"
                            model = "E61 520d"
                            generation = "E61"
                            engine = "M47N2 2.0d"
                            year = "2006"
                            mileage = "285000"
                            partsCatalogUrl = E61_CZESCIDOBMW_CATALOG_URL
                            note = "Pierwsze auto w garazu. Start: tylna zwrotnica lewa, zardzewiala sruba."
                        }
                    )
                }
            }

            item {
                WizardCard {
                    GarageTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = "Marka",
                        modifier = Modifier.fillMaxWidth()
                    )
                    GarageTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = "Model",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_vehicle_model_field"),
                        placeholder = "np. E60 520d"
                    )
                    GarageTextField(
                        value = generation,
                        onValueChange = { generation = it },
                        label = "Generacja / nadwozie",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "np. E60"
                    )
                    GarageTextField(
                        value = engine,
                        onValueChange = { engine = it },
                        label = "Silnik",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_vehicle_engine_field"),
                        placeholder = "np. M47N2 2.0d"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GarageTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit).take(4) },
                            label = "Rok",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                        GarageTextField(
                            value = mileage,
                            onValueChange = { mileage = it.filter(Char::isDigit).take(7) },
                            label = "Przebieg",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                    }
                    GarageTextField(
                        value = vin,
                        onValueChange = { vin = it.uppercase().take(17) },
                        label = "VIN",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "17 znakow"
                    )
                    GarageTextField(
                        value = partsCatalogUrl,
                        onValueChange = { partsCatalogUrl = it.trim() },
                        label = "Link katalogu czescidobmw.pl",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Opcjonalnie, gdy katalog po VIN wymaga gotowego linku"
                    )
                    GarageTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Notatka startowa",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "np. tylna zwrotnica lewa, diagnostyka DDE",
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onCancel?.let {
                        TextButton(
                            onClick = it,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Anuluj")
                        }
                    }
                    Button(
                        onClick = {
                            onVehicleCreated(
                                Vehicle(
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    generation = generation.trim(),
                                    engine = engine.trim(),
                                    year = year.trim(),
                                    vin = vin.trim(),
                                    mileage = mileage.trim(),
                                    note = note.trim(),
                                    id = initialVehicle?.id.orEmpty().ifBlank { "vehicle-${System.currentTimeMillis()}" },
                                    partsCatalogUrl = partsCatalogUrl.trim()
                                )
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(if (onCancel == null) 1f else 1.4f)
                            .testTag("add_vehicle_save_button")
                    ) {
                        Text(saveLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportVehicleCard(onImport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Masz juz zapisane auto?",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Text(
                text = "Mozesz od razu wczytac backup profilu, bazy i plikow auta.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            Button(onClick = onImport) {
                Text("Importuj auto")
            }
        }
    }
}

@Composable
private fun VehicleTemplateCard(onUseTemplate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Szybki start",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.car_bmw_e61),
                    contentDescription = "BMW E61 Touring",
                    modifier = Modifier
                        .width(150.dp)
                        .height(86.dp),
                    contentScale = ContentScale.Fit
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BMW E61 Touring",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kombi, diesel, profil pod pierwszy garazowy scenariusz.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                    Button(onClick = onUseTemplate) {
                        Text("Uzyj E61")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun AddVehicleWizardPreview() {
    GarageTheme {
        AddVehicleWizard(onVehicleCreated = {})
    }
}
