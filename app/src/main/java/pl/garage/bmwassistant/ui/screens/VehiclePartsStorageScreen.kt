package pl.garage.bmwassistant.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.data.sampleConsumablesFor
import pl.garage.bmwassistant.data.sampleInventoryPartsFor
import pl.garage.bmwassistant.data.sampleShoppingListFor
import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private val ShoppingReceiveButtonColor = Color(0xFF153D2F)
private val ShoppingReceiveButtonContentColor = Color(0xFF69F0AE)
private val EditActionButtonColor = Color(0xFF16293E)
private val EditActionButtonContentColor = Color(0xFF7DC4FF)
private val DeleteActionButtonColor = Color(0xFF34191C)
private val DeleteActionButtonContentColor = Color(0xFFFF7A70)
private val MetaSurfaceColor = Color(0xFF111E2C)
private val ShoppingPartCardColor = Color(0xFF1A2332)
private val ShoppingMarketRowColor = Color(0xFF1A2332)
private val ShoppingPartnerCardColor = Color(0xFF1B2D22)
private val ShoppingPartnerBorderColor = Color(0xFF2E5E3B)
private val ShoppingAiCardStart = Color(0xFF2A1E3D)
private val ShoppingAiCardEnd = Color(0xFF3B2370)
private val ShoppingPrimaryTextColor = Color(0xFFE6ECF5)
private val ShoppingSecondaryTextColor = Color(0xFFA6B0C3)

@Composable
fun VehiclePartsStorageScreen(
    vehicle: Vehicle,
    availableRepairs: List<RepairProject> = emptyList(),
    inventoryParts: List<PartInventoryItem>,
    shoppingList: List<ShoppingListItem>,
    consumables: List<ConsumableItem>,
    initialSection: PartsStorageSection? = null,
    initialShoppingRepairTitle: String? = null,
    initialShoppingArea: VehicleArea? = null,
    initialShoppingItemId: String? = null,
    onInitialShoppingClosed: () -> Unit = {},
    onInventoryUpdated: (List<PartInventoryItem>) -> Unit = {},
    onShoppingListUpdated: (List<ShoppingListItem>) -> Unit = {},
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var isAddingPart by remember { mutableStateOf(false) }
    var isAddingManualPart by remember { mutableStateOf(false) }
    var isExternalLookupVisible by remember { mutableStateOf(false) }
    var isAddingShoppingItem by remember { mutableStateOf(false) }
    var partPendingEdit by remember { mutableStateOf<PartInventoryItem?>(null) }
    var partPendingDeletion by remember { mutableStateOf<PartInventoryItem?>(null) }
    var shoppingItemPendingEdit by remember { mutableStateOf<ShoppingListItem?>(null) }
    var shoppingItemPendingDeletion by remember { mutableStateOf<ShoppingListItem?>(null) }
    var shoppingItemPendingReceive by remember { mutableStateOf<ShoppingListItem?>(null) }
    var shoppingItemPendingAi by remember { mutableStateOf<ShoppingListItem?>(null) }
    var shoppingItemPreview by remember { mutableStateOf<ShoppingListItem?>(null) }
    var selectedSectionName by rememberSaveable(vehicle.id, initialShoppingRepairTitle) {
        mutableStateOf(initialSection?.name)
    }
    val selectedSection = selectedSectionName?.let(PartsStorageSection::valueOf)
    var storedInventoryParts by remember(vehicle.id, inventoryParts) { mutableStateOf(inventoryParts) }
    var storedShoppingList by remember(vehicle.id, shoppingList) { mutableStateOf(shoppingList) }
    val allInventoryParts = storedInventoryParts
    val allShoppingList = storedShoppingList

    LaunchedEffect(initialShoppingItemId, selectedSectionName, allShoppingList) {
        if (selectedSectionName == PartsStorageSection.Shopping.name && !initialShoppingItemId.isNullOrBlank()) {
            shoppingItemPreview = allShoppingList.firstOrNull { it.stableId() == initialShoppingItemId }
        }
    }

    fun updateStoredParts(parts: List<PartInventoryItem>) {
        storedInventoryParts = parts
        onInventoryUpdated(parts)
    }

    fun updateShoppingList(items: List<ShoppingListItem>) {
        storedShoppingList = items
        onShoppingListUpdated(items)
    }

    BackHandler(enabled = selectedSection != null) {
        if (initialShoppingRepairTitle == null) {
            selectedSectionName = null
            onInitialShoppingClosed()
        } else {
            onBack()
        }
    }

    if (isAddingPart) {
        AddPartEntryDialog(
            onDismiss = { isAddingPart = false },
            onManualAdd = {
                isAddingPart = false
                isAddingManualPart = true
            },
            onExternalAdd = {
                isAddingPart = false
                isExternalLookupVisible = true
            }
        )
    }

    if (isAddingManualPart) {
        ManualPartEntryDialog(
            nextId = nextPartId(storedInventoryParts),
            availableRepairs = availableRepairs,
            onDismiss = { isAddingManualPart = false },
            onSave = { part ->
                updateStoredParts(storedInventoryParts + part)
                isAddingManualPart = false
                selectedSectionName = PartsStorageSection.Inventory.name
            }
        )
    }

    if (isExternalLookupVisible) {
        ExternalPartLookupDialog(
            nextId = nextPartId(storedInventoryParts),
            availableRepairs = availableRepairs,
            onDismiss = { isExternalLookupVisible = false },
            onSave = { part ->
                updateStoredParts(storedInventoryParts + part)
                isExternalLookupVisible = false
                selectedSectionName = PartsStorageSection.Inventory.name
            }
        )
    }

    if (isAddingShoppingItem) {
        ShoppingPartLookupDialog(
            nextId = nextShoppingItemId(storedShoppingList),
            initialRepairTitle = initialShoppingRepairTitle.orEmpty(),
            initialArea = initialShoppingArea ?: VehicleArea.Service,
            onDismiss = { isAddingShoppingItem = false },
            onSave = { item ->
                updateShoppingList(storedShoppingList + item)
                isAddingShoppingItem = false
                selectedSectionName = PartsStorageSection.Shopping.name
            }
        )
    }

    shoppingItemPendingEdit?.let { item ->
        EditShoppingItemDialog(
            item = item,
            availableRepairs = availableRepairs,
            onDismiss = { shoppingItemPendingEdit = null },
            onSave = { updatedItem ->
                updateShoppingList(
                    storedShoppingList.map {
                        if (it.stableId() == item.stableId()) updatedItem else it
                    }
                )
                shoppingItemPendingEdit = null
            }
        )
    }

    shoppingItemPendingDeletion?.let { item ->
        ConfirmDeleteShoppingItemDialog(
            item = item,
            onConfirm = {
                updateShoppingList(storedShoppingList.filterNot { it.stableId() == item.stableId() })
                shoppingItemPendingDeletion = null
                shoppingItemPreview = null
                Toast.makeText(context, "Czesc zostala usunieta.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { shoppingItemPendingDeletion = null }
        )
    }

    partPendingEdit?.let { part ->
        ManualPartEntryDialog(
            nextId = part.id,
            availableRepairs = availableRepairs,
            initialPart = part,
            onDismiss = { partPendingEdit = null },
            onSave = { updatedPart ->
                updateStoredParts(
                    storedInventoryParts.map {
                        if (it.stableId() == part.stableId()) updatedPart else it
                    }
                )
                partPendingEdit = null
            }
        )
    }

    partPendingDeletion?.let { part ->
        ConfirmDeletePartDialog(
            part = part,
            onConfirm = {
                updateStoredParts(storedInventoryParts.filterNot { it.stableId() == part.stableId() })
                partPendingDeletion = null
            },
            onDismiss = { partPendingDeletion = null }
        )
    }

    shoppingItemPendingReceive?.let { item ->
        ReceiveShoppingItemDialog(
            item = item,
            onConfirm = {
                updateStoredParts(storedInventoryParts + item.toInventoryPart(nextPartId(storedInventoryParts)))
                updateShoppingList(storedShoppingList.filterNot { it.stableId() == item.stableId() })
                shoppingItemPendingReceive = null
                shoppingItemPreview = null
                Toast.makeText(context, "Czesc zostala przyjeta do magazynu.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { shoppingItemPendingReceive = null }
        )
    }

    shoppingItemPreview?.let { item ->
        ShoppingPartDetailsBottomSheet(
            item = item,
            onDismiss = { shoppingItemPreview = null },
            onEditItem = {
                shoppingItemPreview = null
                shoppingItemPendingEdit = item
            },
            onDeleteItem = {
                shoppingItemPreview = null
                shoppingItemPendingDeletion = item
            },
            onReceiveItem = {
                shoppingItemPreview = null
                shoppingItemPendingReceive = item
            },
            onAskAi = {
                shoppingItemPreview = null
                shoppingItemPendingAi = item
            }
        )
    }

    shoppingItemPendingAi?.let { item ->
        AiPartAssistantBottomSheet(
            item = item,
            vehicle = vehicle,
            onDismiss = { shoppingItemPendingAi = null }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = if (bottomBar == null) 18.dp else 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TextButton(onClick = onBack) {
                        Text(if (initialShoppingRepairTitle == null) "Wroc do auta" else "Wroc do naprawy")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Header(
                                title = selectedSection?.title ?: "Magazyn czesci",
                                subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                            )
                        }
                        if (selectedSection != PartsStorageSection.Shopping) {
                            AddPartButton(onClick = { isAddingPart = true })
                        }
                    }
                }

                selectedSection?.let { section ->
                    item {
                        TextButton(
                            onClick = {
                                if (initialShoppingRepairTitle == null) {
                                    selectedSectionName = null
                                } else {
                                    onBack()
                                }
                            }
                        ) {
                            Text(if (initialShoppingRepairTitle == null) "Wroc do kafelkow magazynu" else "Wroc do naprawy")
                        }
                    }

                    when (section) {
                        PartsStorageSection.Inventory -> {
                            item {
                                InventoryDatabaseSection(
                                    inventoryParts = allInventoryParts,
                                    onUpdatePart = { updatedPart ->
                                        updateStoredParts(
                                            storedInventoryParts.map {
                                                if (it.stableId() == updatedPart.stableId()) updatedPart else it
                                            }
                                        )
                                    },
                                    onEditPart = { partPendingEdit = it },
                                    onDeletePart = { partPendingDeletion = it }
                                )
                            }
                        }

                        PartsStorageSection.Shopping -> {
                            item {
                                ShoppingListSection(
                                    shoppingList = allShoppingList,
                                    initialRepairTitle = initialShoppingRepairTitle,
                                    onAddShoppingItem = { isAddingShoppingItem = true },
                                    onOpenItemDetails = { shoppingItemPreview = it }
                                )
                            }
                        }

                        PartsStorageSection.Consumables -> {
                            item {
                                PartsSection(
                                    title = "Baza materialow eksploatacyjnych",
                                    subtitle = "Oleje, smary, cleanery, preparaty i inne rzeczy zuzywalne.",
                                    countLabel = "${consumables.size} pozycji"
                                ) {
                                    if (consumables.isEmpty()) {
                                        EmptyPartsRow("Brak materialow eksploatacyjnych.")
                                    } else {
                                        consumables.forEach { item ->
                                            ConsumableRow(item = item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    item {
                        PartsStorageTile(
                            eyebrow = "Baza danych",
                            title = "Stan magazynu",
                            subtitle = "Pelna lista czesci, ktore masz fizycznie w garazu.",
                            countLabel = "${allInventoryParts.size} pozycji",
                            marker = "DB",
                            onClick = { selectedSectionName = PartsStorageSection.Inventory.name }
                        )
                    }

                    item {
                        PartsStorageTile(
                            eyebrow = "Polaczone z naprawami",
                            title = "Lista zakupow do napraw",
                            subtitle = "Rozwijana lista czesci pogrupowana wedlug konkretnej naprawy.",
                            countLabel = "${allShoppingList.size} pozycji",
                            marker = "ZK",
                            onClick = { selectedSectionName = PartsStorageSection.Shopping.name }
                        )
                    }

                    item {
                        PartsStorageTile(
                            eyebrow = "Baza danych",
                            title = "Materialy eksploatacyjne",
                            subtitle = "Oleje, smary, preparaty i inne rzeczy zuzywalne.",
                            countLabel = "${consumables.size} pozycji",
                            marker = "ME",
                            onClick = { selectedSectionName = PartsStorageSection.Consumables.name }
                        )
                    }
                }
            }
            bottomBar?.invoke(this)
        }
    }
}

private enum class InventorySearchColumn(val label: String) {
    Id("ID"),
    OemPartNumber("Nr czesci OEM"),
    ManufacturerPartNumber("Nr czesci producenta"),
    Name("Nazwa czesci"),
    Manufacturer("Producent"),
    Repair("Do jakiej naprawy"),
    Quantity("Ilosc"),
    Price("Cena zakupu"),
}

data class MockPartLookupResult(
    val oemPartNumber: String,
    val manufacturerPartNumber: String,
    val name: String,
    val manufacturer: String,
    val realOemPrice: String,
    val shopPrice: String,
    val diagram: String,
    val realOemUrl: String,
    val shopUrl: String,
    val imageSource: String,
    val imageUrl: String? = null,
    val imageSearchUrl: String,
) {
    val partNumber: String
        get() = manufacturerPartNumber.ifBlank { oemPartNumber }
}

data class ParsedPartLabel(
    val oemPartNumber: String?,
    val manufacturerPartNumber: String?,
    val manufacturer: String?,
)

enum class PartsStorageSection(val title: String) {
    Inventory("Stan magazynu"),
    Shopping("Lista zakupow do napraw"),
    Consumables("Materialy eksploatacyjne"),
}

private fun nextPartId(parts: List<PartInventoryItem>): String =
    ((parts.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()

private fun nextShoppingItemId(items: List<ShoppingListItem>): String =
    "shopping-${(items.mapNotNull { it.id.removePrefix("shopping-").toIntOrNull() }.maxOrNull() ?: 0) + 1}"

private fun shoppingRepairCountLabel(count: Int): String = when {
    count == 1 -> "naprawa"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "naprawy"
    else -> "napraw"
}

private fun shoppingPartCountLabel(count: Int): String = when {
    count == 1 -> "czesc"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "czesci"
    else -> "czesci"
}

private fun shoppingRepairTotalLabel(items: List<ShoppingListItem>): String {
    val prices = items.mapNotNull { item ->
        item.price
            .replace("PLN", "", ignoreCase = true)
            .replace("zl", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }
    return if (prices.isEmpty()) {
        "Cena do sprawdzenia"
    } else {
        String.format(Locale.US, "%.2f PLN", prices.sum()).replace(".", ",")
    }
}

private fun List<ShoppingListItem>.shoppingPrimaryArea(): VehicleArea =
    groupBy { it.area }
        .maxByOrNull { (_, values) -> values.size }
        ?.key
        ?: VehicleArea.Service

@Composable
private fun PartsStorageTile(
    eyebrow: String,
    title: String,
    subtitle: String,
    countLabel: String,
    marker: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = marker,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = eyebrow,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                Text(
                    text = countLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
            }
        }
    }
}

@Composable
private fun InventoryDatabaseSection(
    inventoryParts: List<PartInventoryItem>,
    onUpdatePart: (PartInventoryItem) -> Unit,
    onEditPart: (PartInventoryItem) -> Unit,
    onDeletePart: (PartInventoryItem) -> Unit,
) {
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchColumn by remember { mutableStateOf(InventorySearchColumn.Name) }
    var query by remember { mutableStateOf("") }
    var isColumnPickerVisible by remember { mutableStateOf(false) }
    var pendingPhotoPartId by remember { mutableStateOf<String?>(null) }
    var photoUris by remember(inventoryParts) {
        mutableStateOf(
            inventoryParts.mapNotNull { part ->
                part.photoUri?.let { uri -> part.stableId() to uri }
            }.toMap()
        )
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val partId = pendingPhotoPartId
        if (partId != null && uri != null) {
            val photoUri = uri.toString()
            photoUris = photoUris + (partId to photoUri)
            inventoryParts.firstOrNull { it.stableId() == partId }?.let { part ->
                onUpdatePart(part.copy(photoUri = photoUri))
            }
        }
        pendingPhotoPartId = null
    }

    val filteredParts = remember(inventoryParts, searchColumn, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            inventoryParts
        } else {
            inventoryParts.filter { part ->
                part.searchValue(searchColumn).lowercase().contains(normalizedQuery)
            }
        }
    }

    PartsSection(
        title = "Baza danych czesci",
        subtitle = "Widok tabeli magazynu, podobny do arkusza z kolumnami.",
        countLabel = "${filteredParts.size} z ${inventoryParts.size} pozycji"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Szukaj w magazynie",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (query.isNotBlank()) {
                TextButton(onClick = { query = "" }) {
                    Text("Wyczysc")
                }
            }
        }

        if (isSearchVisible) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Szukaj po kolumnie",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                    TextButton(onClick = { isColumnPickerVisible = !isColumnPickerVisible }) {
                        Text(searchColumn.label)
                    }
                    if (isColumnPickerVisible) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InventorySearchColumn.entries.forEach { column ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchColumn = column
                                            isColumnPickerVisible = false
                                        },
                                    color = if (column == searchColumn) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = column.label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        color = if (column == searchColumn) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (column == searchColumn) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            }
                        }
                    }
                    GarageTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Wpisz szukana wartosc",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "np. zwrotnica, BMW, do ustalenia"
                    )
                }
            }
        }

        if (filteredParts.isEmpty()) {
            EmptyPartsRow("Brak wynikow dla wybranej kolumny.")
        } else {
            InventoryCardList(
                parts = filteredParts,
                photoUris = photoUris,
                onAddPhoto = { partId ->
                    pendingPhotoPartId = partId
                    photoPickerLauncher.launch("image/*")
                },
                onSetPhotoUrl = { part, photoUrl ->
                    photoUris = photoUris + (part.stableId() to photoUrl)
                    onUpdatePart(part.copy(photoUri = photoUrl))
                },
                onEditPart = onEditPart,
                onDeletePart = onDeletePart
            )
        }
    }
}

@Composable
private fun InventoryCardList(
    parts: List<PartInventoryItem>,
    photoUris: Map<String, String>,
    onAddPhoto: (String) -> Unit,
    onSetPhotoUrl: (PartInventoryItem, String) -> Unit,
    onEditPart: (PartInventoryItem) -> Unit,
    onDeletePart: (PartInventoryItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        parts.forEach { part ->
            InventoryPartCard(
                part = part,
                photoUri = photoUris[part.stableId()],
                onAddPhoto = { onAddPhoto(part.stableId()) },
                onSetPhotoUrl = { photoUrl -> onSetPhotoUrl(part, photoUrl) },
                onEditPart = { onEditPart(part) },
                onDeletePart = { onDeletePart(part) }
            )
        }
    }
}

@Composable
private fun InventoryPhotoCell(
    photoUri: String?,
    imageSearchUrl: String,
    onAddPhoto: () -> Unit,
    onSetPhotoUrl: (String) -> Unit,
) {
    var isPreviewOpen by remember { mutableStateOf(false) }
    var isMissingPhotoMenuOpen by remember { mutableStateOf(false) }
    var isPhotoUrlDialogOpen by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (isPreviewOpen && photoUri != null) {
        AlertDialog(
            onDismissRequest = { isPreviewOpen = false },
            title = { Text("Zdjecie czesci") },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    PartPhotoContent(
                        photoUri = photoUri,
                        height = 280.dp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isPreviewOpen = false }) {
                    Text("Zamknij")
                }
            }
        )
    }

    if (isMissingPhotoMenuOpen) {
        AlertDialog(
            onDismissRequest = { isMissingPhotoMenuOpen = false },
            title = { Text("Dodaj zdjecie czesci") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddPartModeRow(
                        title = "Szukaj w Google grafika",
                        subtitle = "Otworz wyszukiwanie po numerze czesci i zapisz wybrane zdjecie.",
                        onClick = {
                            isMissingPhotoMenuOpen = false
                            uriHandler.openUri(imageSearchUrl)
                        }
                    )
                    AddPartModeRow(
                        title = "Wybierz zdjecie z telefonu",
                        subtitle = "Dodaj zdjecie zapisane po wyszukaniu albo zrobione samodzielnie.",
                        onClick = {
                            isMissingPhotoMenuOpen = false
                            onAddPhoto()
                        }
                    )
                    AddPartModeRow(
                        title = "Wklej link zdjecia",
                        subtitle = "Skopiuj adres obrazu z Google grafika i zapisz go bez pobierania pliku.",
                        onClick = {
                            isMissingPhotoMenuOpen = false
                            isPhotoUrlDialogOpen = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isMissingPhotoMenuOpen = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (isPhotoUrlDialogOpen) {
        PhotoUrlDialog(
            onDismiss = { isPhotoUrlDialogOpen = false },
            onSave = { photoUrl ->
                onSetPhotoUrl(photoUrl)
                isPhotoUrlDialogOpen = false
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable {
                if (photoUri == null) {
                    isMissingPhotoMenuOpen = true
                } else {
                    isPreviewOpen = true
                }
            },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        PartPhotoContent(
            photoUri = photoUri,
            height = 92.dp
        )
    }
}

@Composable
fun PartPhotoContent(
    photoUri: String?,
    height: Dp = 52.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, photoUri) {
        value = withContext(Dispatchers.IO) {
            photoUri?.let { value ->
                runCatching {
                    val uri = Uri.parse(value)
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        URL(value).openStream().use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Zdjecie czesci",
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentScale = contentScale
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = "Dodaj zdjecie czesci",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PhotoUrlDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var photoUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link do zdjecia") },
        text = {
            GarageTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = "URL zdjecia",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "https://..."
            )
        },
        confirmButton = {
            TextButton(
                enabled = photoUrl.startsWith("http://") || photoUrl.startsWith("https://"),
                onClick = { onSave(photoUrl.trim()) }
            ) {
                Text("Zapisz zdjecie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

private fun PartInventoryItem.searchValue(column: InventorySearchColumn): String =
    when (column) {
        InventorySearchColumn.Id -> id
        InventorySearchColumn.OemPartNumber -> oemPartNumber
        InventorySearchColumn.ManufacturerPartNumber -> manufacturerPartNumber
        InventorySearchColumn.Name -> name
        InventorySearchColumn.Manufacturer -> manufacturer
        InventorySearchColumn.Repair -> repairTitle.orEmpty()
        InventorySearchColumn.Quantity -> quantity.toString()
        InventorySearchColumn.Price -> purchasePrice
    }

private fun PartInventoryItem.imageSearchUrl(): String =
    imageSearchUrlFor(
        partNumber = manufacturerPartNumber.ifBlank { oemPartNumber },
        manufacturer = manufacturer
    )

@Composable
private fun InventoryPartCard(
    part: PartInventoryItem,
    photoUri: String?,
    onAddPhoto: () -> Unit,
    onSetPhotoUrl: (String) -> Unit,
    onEditPart: () -> Unit,
    onDeletePart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.34f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(108.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        InventoryPhotoCell(
                            photoUri = photoUri,
                            imageSearchUrl = part.imageSearchUrl(),
                            onAddPhoto = onAddPhoto,
                            onSetPhotoUrl = onSetPhotoUrl
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = part.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOf(part.manufacturer, part.manufacturerPartNumber)
                            .filter { it.isNotBlank() && it != "do uzupelnienia" }
                            .joinToString(" ")
                            .ifBlank { "Numer producenta do uzupelnienia" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    InventoryMetaLine("OEM", part.oemPartNumber.ifBlank { "Do uzupelnienia" })
                    Text(
                        text = "ID: ${part.id}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InventoryInfoPill(
                    label = "Na stanie",
                    value = "${part.quantity} szt."
                )
                InventoryInfoPill(
                    label = "Cena zakupu",
                    value = part.purchasePrice.ifBlank { "Do uzupelnienia" },
                    emphasize = true
                )
            }

            part.repairTitle?.let { repairTitle ->
                Surface(
                    color = MetaSurfaceColor.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Powiazana naprawa: $repairTitle",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PartActionButton(
                    label = "Edytuj",
                    iconRes = R.drawable.ic_edit,
                    containerColor = EditActionButtonColor,
                    contentColor = EditActionButtonContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onEditPart
                )
                PartActionButton(
                    label = "Usun",
                    iconRes = R.drawable.ic_delete,
                    containerColor = DeleteActionButtonColor,
                    contentColor = DeleteActionButtonContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onDeletePart
                )
            }
        }
    }
}

@Composable
private fun InventoryMetaLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun InventoryInfoPill(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    emphasized: Boolean,
) {
    Surface(
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MetaSurfaceColor.copy(alpha = 0.76f)
        },
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun PartActionButton(
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun AddPartButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PartsSection(
    title: String,
    subtitle: String,
    countLabel: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
                Text(
                    text = countLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            content()
        }
    }
}

@Composable
private fun ShoppingListSection(
    shoppingList: List<ShoppingListItem>,
    initialRepairTitle: String?,
    onAddShoppingItem: () -> Unit,
    onOpenItemDetails: (ShoppingListItem) -> Unit,
) {
    var expandedRepairTitles by rememberSaveable(shoppingList, initialRepairTitle) {
        mutableStateOf(
            if (initialRepairTitle.isNullOrBlank()) {
                shoppingList.map { it.repairTitle }.distinct()
            } else {
                listOf(initialRepairTitle)
            }
        )
    }
    val expandedRepairs = remember(expandedRepairTitles) { expandedRepairTitles.toSet() }
    val visibleItems = if (initialRepairTitle.isNullOrBlank()) {
        shoppingList
    } else {
        shoppingList.filter { it.repairTitle == initialRepairTitle }
    }
    val groupedRepairs = visibleItems
        .groupBy { it.repairTitle }
        .toList()
        .sortedBy { (repairTitle, _) -> repairTitle.lowercase(Locale.getDefault()) }
    val repairCount = groupedRepairs.size

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShoppingListSummaryCard(
            title = initialRepairTitle?.let { "Lista zakupow: $it" } ?: "Lista zakupow do napraw",
            description = "Dobierz OEM u partnera, sprawdz rynek i popros AI o wsparcie przy decyzji.",
            repairCount = repairCount,
            partCount = visibleItems.size
        )

        if (visibleItems.isEmpty()) {
            EmptyPartsRow("Brak czesci do kupienia.")
        } else {
            Text(
                text = "NAPRAWY",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA6B0C3)
            )
            groupedRepairs.forEach { (repairTitle, items) ->
                ExpandableRepairShoppingGroup(
                    repairTitle = repairTitle,
                    items = items,
                    isExpanded = repairTitle in expandedRepairs,
                    onToggle = {
                        expandedRepairTitles = if (repairTitle in expandedRepairs) {
                            expandedRepairTitles - repairTitle
                        } else {
                            expandedRepairTitles + repairTitle
                        }
                    },
                    onOpenItemDetails = onOpenItemDetails,
                )
            }
        }

        AddShoppingItemButton(onClick = onAddShoppingItem)
    }
}

@Composable
private fun ExpandableRepairShoppingGroup(
    repairTitle: String,
    items: List<ShoppingListItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenItemDetails: (ShoppingListItem) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101A26)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                        Image(
                        painter = painterResource(items.shoppingPrimaryArea().iconResource()),
                        contentDescription = items.shoppingPrimaryArea().label,
                        modifier = Modifier
                            .padding(12.dp)
                            .height(28.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = repairTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${items.size} ${shoppingPartCountLabel(items.size)} • ${shoppingRepairTotalLabel(items)}",
                        fontSize = 14.sp,
                        color = Color(0xFFA6B0C3)
                    )
                }
                Text(
                    text = if (isExpanded) "⌃" else "⌄",
                    color = Color(0xFF63C8FF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isExpanded) {
                ShoppingCardList(
                    items = items,
                    onOpenItemDetails = onOpenItemDetails,
                )
            }
        }
    }
}

@Composable
private fun ShoppingCardList(
    items: List<ShoppingListItem>,
    onOpenItemDetails: (ShoppingListItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            ShoppingListOverviewItemCard(
                item = item,
                onClick = { onOpenItemDetails(item) },
            )
        }
    }
}

@Composable
private fun ShoppingListSummaryCard(
    title: String,
    description: String,
    repairCount: Int,
    partCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121D29),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF1D2938),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = Color(0xFF88D6FF),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFFA6B0C3)
                )
                Text(
                    text = "${repairCount} ${shoppingRepairCountLabel(repairCount)} • ${partCount} ${shoppingPartCountLabel(partCount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF63C8FF)
                )
            }
        }
    }
}

@Composable
private fun ShoppingListOverviewItemCard(
    item: ShoppingListItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF141F2C),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF0F1723),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PartPhotoContent(
                        photoUri = item.imageUri,
                        height = 52.dp,
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "OEM: ${item.partNumber.ifBlank { "Do uzupelnienia" }}",
                    fontSize = 13.sp,
                    color = Color(0xFFA6B0C3)
                )
                Text(
                    text = item.price.ifBlank { "Cena do sprawdzenia" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF63C8FF)
                )
            }

            Text(
                text = "›",
                color = Color(0xFF63C8FF),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddShoppingItemButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF4FC8FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4FC8FF)
            )
            Text(
                text = "Dodaj czesc po OEM",
                modifier = Modifier.padding(start = 10.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4FC8FF)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingPartDetailsBottomSheet(
    item: ShoppingListItem,
    onDismiss: () -> Unit,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onReceiveItem: () -> Unit,
    onAskAi: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1621),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Szczegoly czesci",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ShoppingPrimaryTextColor
            )
            ShoppingListItemCard(
                item = item,
                onEditItem = onEditItem,
                onDeleteItem = onDeleteItem,
                onReceiveItem = onReceiveItem,
                onAskAi = onAskAi
            )
        }
    }
}

@Composable
private fun ShoppingListItemCard(
    item: ShoppingListItem,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onReceiveItem: () -> Unit,
    onAskAi: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val schematicUrl = item.realOemUrl?.takeIf { it.isNotBlank() }
        ?: item.shopUrl?.takeIf { it.isNotBlank() }
    var isDiagramPreviewOpen by remember(schematicUrl) { mutableStateOf(false) }

    if (isDiagramPreviewOpen && schematicUrl != null) {
        ShoppingDiagramPreviewDialog(
            diagramPageUrl = schematicUrl,
            onDismiss = { isDiagramPreviewOpen = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1621)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ShoppingPartCardColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ShoppingPrimaryTextColor
                        )
                        Text(
                            text = "OEM: ${item.partNumber.ifBlank { "Do uzupelnienia" }}",
                            fontSize = 14.sp,
                            color = ShoppingSecondaryTextColor
                        )
                        SummaryBadge(
                            label = item.partnerCompatibilityLabel(),
                            emphasized = true
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        color = Color(0xFF111926),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PartPhotoContent(
                                photoUri = item.imageUri,
                                height = 156.dp,
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    schematicUrl?.let {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDiagramPreviewOpen = true },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF3A8BFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Zobacz na schemacie",
                                    tint = Color(0xFFE6F1FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Zobacz na schemacie",
                                    modifier = Modifier.padding(start = 10.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE6F1FF)
                                )
                            }
                        }
                    }
                }
            }

            ShoppingPartnerSection(
                item = item,
                onOpenPartner = item.shopUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { url -> { uriHandler.openUri(url) } }
            )

            MarketComparisonSection(
                item = item,
                onOpenSource = { source ->
                    uriHandler.openUri(item.marketSearchUrlFor(source))
                }
            )

            AiAssistantPromptCard(
                onAskAi = onAskAi
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PartActionButton(
                    label = "Przyjmij",
                    iconRes = R.drawable.ic_check,
                    containerColor = ShoppingReceiveButtonColor,
                    contentColor = ShoppingReceiveButtonContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onReceiveItem
                )
                PartActionButton(
                    label = "Edytuj",
                    iconRes = R.drawable.ic_edit,
                    containerColor = EditActionButtonColor,
                    contentColor = EditActionButtonContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onEditItem
                )
                PartActionButton(
                    label = "Usun",
                    iconRes = R.drawable.ic_delete,
                    containerColor = DeleteActionButtonColor,
                    contentColor = DeleteActionButtonContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onDeleteItem
                )
            }
        }
    }
}

@Composable
private fun ShoppingDiagramPreviewDialog(
    diagramPageUrl: String,
    onDismiss: () -> Unit,
) {
    val imageUrl by produceState<String?>(initialValue = null, diagramPageUrl) {
        value = withContext(Dispatchers.IO) {
            resolveShoppingDiagramImageUrl(diagramPageUrl)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schemat czesci",
                color = ShoppingPrimaryTextColor
            )
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF111926),
                shape = RoundedCornerShape(14.dp)
            ) {
                when (val resolvedImageUrl = imageUrl) {
                    null -> {
                        Text(
                            text = "Laduje podglad schematu...",
                            modifier = Modifier.padding(16.dp),
                            color = ShoppingSecondaryTextColor
                        )
                    }

                    "" -> {
                        Text(
                            text = "Nie udalo sie wczytac obrazu schematu dla tej czesci.",
                            modifier = Modifier.padding(16.dp),
                            color = ShoppingSecondaryTextColor
                        )
                    }

                    else -> {
                        PartPhotoContent(
                            photoUri = resolvedImageUrl,
                            height = 320.dp,
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
private fun ShoppingPartnerSection(
    item: ShoppingListItem,
    onOpenPartner: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ShoppingPartnerCardColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ShoppingPartnerBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sklep partnerski (OEM)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShoppingPrimaryTextColor
                    )
                    SummaryBadge(
                        label = "Rekomendowany",
                        emphasized = true
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.price.ifBlank { "Cena do sprawdzenia" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShoppingPrimaryTextColor
                    )
                    Text(
                        text = "${item.quantity} szt.",
                        fontSize = 12.sp,
                        color = ShoppingSecondaryTextColor
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BenefitLabel(label = "Pewne dopasowanie")
                BenefitLabel(label = "Schematy OEM")
            }

            onOpenPartner?.let { action ->
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = action
                ) {
                    Text("Przejdz do oferty OEM")
                }
            }
        }
    }
}

@Composable
private fun BenefitLabel(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(18.dp),
            color = Color(0xFF173724),
            shape = RoundedCornerShape(999.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color(0xFF9EF0A5),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            color = ShoppingPrimaryTextColor
        )
    }
}

@Composable
private fun MarketComparisonSection(
    item: ShoppingListItem,
    onOpenSource: (MarketSearchSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sprawdz rynek",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ShoppingPrimaryTextColor
        )
        MarketSearchSource.entries.forEach { source ->
            MarketSearchRow(
                source = source,
                item = item,
                onClick = { onOpenSource(source) }
            )
        }
    }
}

@Composable
private fun MarketSearchRow(
    source: MarketSearchSource,
    item: ShoppingListItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        color = ShoppingMarketRowColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceBadge(source = source)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "Szukaj w ${source.displayName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ShoppingPrimaryTextColor
                )
                if (!item.hasKnownOemNumber()) {
                    Text(
                        text = "Uzyjemy nazwy i producenta",
                        fontSize = 11.sp,
                        color = ShoppingSecondaryTextColor
                    )
                }
            }
            Text(
                text = ">",
                color = Color(0xFFA6B0C3),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SourceBadge(source: MarketSearchSource) {
    val color = when (source) {
        MarketSearchSource.Allegro -> Color(0xFFFF8A1E)
        MarketSearchSource.Ceneo -> Color(0xFFFF6C1F)
        MarketSearchSource.IParts -> Color(0xFFE44332)
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = source.badgeLabel.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AiAssistantPromptCard(
    onAskAi: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6C4DFF).copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(ShoppingAiCardStart, ShoppingAiCardEnd)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Zapytaj AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShoppingPrimaryTextColor
                    )
                    Text(
                        text = "Znajdz alternatywy, porownaj ceny i uzyskaj rekomendacje od AI",
                        fontSize = 13.sp,
                        color = Color(0xFFE2D8FF)
                    )
                }
            }

            PartActionButton(
                label = "Zapytaj AI o te czesc",
                iconRes = R.drawable.ic_search,
                containerColor = Color(0xFF6E42D9),
                contentColor = Color(0xFFF6F0FF),
                modifier = Modifier.fillMaxWidth(),
                onClick = onAskAi
            )
        }
    }
}

private sealed interface AiAssistantUiState {
    data object Loading : AiAssistantUiState
    data object Empty : AiAssistantUiState
    data class ConnectionError(val message: String) : AiAssistantUiState
    data class Success(val response: AiPartComparisonResult) : AiAssistantUiState
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AiPartAssistantBottomSheet(
    item: ShoppingListItem,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val assistant = remember { shoppingAssistantProvider() }
    val request = remember(item, vehicle) { item.toAiComparisonRequest(vehicle) }
    val uiState by produceState<AiAssistantUiState>(
        initialValue = AiAssistantUiState.Loading,
        key1 = item,
        key2 = vehicle
    ) {
        value = runCatching { assistant.comparePart(request) }
            .fold(
                onSuccess = { AiAssistantUiState.Success(it) },
                onFailure = { error ->
                    when (error) {
                        is AiAssistantRequestException -> AiAssistantUiState.ConnectionError(error.userMessage)
                        else -> AiAssistantUiState.ConnectionError(
                            "Nie udało się połączyć z asystentem AI.\nSprawdź konfigurację backendu."
                        )
                    }
                }
            )
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Wyniki AI",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            AiAssistantResultContent(
                item = item,
                request = request,
                response = (uiState as? AiAssistantUiState.Success)?.response,
                connectionErrorMessage = (uiState as? AiAssistantUiState.ConnectionError)?.message,
                isLoading = uiState is AiAssistantUiState.Loading,
                onOpenOffer = { url -> uriHandler.openUri(url) }
            )
        }
    }
}

@Composable
private fun AiAssistantResultContent(
    item: ShoppingListItem,
    request: AiPartComparisonRequest,
    response: AiPartComparisonResult?,
    connectionErrorMessage: String?,
    isLoading: Boolean,
    onOpenOffer: (String) -> Unit,
) {
    val noOffersMessage = "AI nie znalazło jeszcze ofert dla tej części."
    val statusBanner = when {
        isLoading -> "AI analizuje oferty..."
        !connectionErrorMessage.isNullOrBlank() -> "Nie udało się połączyć z asystentem AI."
        response?.offers?.isNotEmpty() == true -> "Znaleziono alternatywne oferty."
        else -> noOffersMessage
    }
    val recommendationText = when {
        isLoading -> "AI analizuje oferty..."
        !connectionErrorMessage.isNullOrBlank() -> connectionErrorMessage
        response == null -> "Brak rekomendacji"
        response.offers.isEmpty() && response.recommendation.isBlank() -> noOffersMessage
        response.recommendation.isBlank() -> "Brak rekomendacji"
        else -> response.recommendation
    }

    AiQuerySummaryCard(
        item = item,
        request = request
    )

    AiStatusBanner(
        text = statusBanner,
        isLoading = isLoading,
        isError = !connectionErrorMessage.isNullOrBlank(),
        isSuccess = response?.offers?.isNotEmpty() == true
    )

    AiResultSectionCard(
        title = "Oferty rynkowe",
        accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    ) {
        if (response?.offers?.isNotEmpty() == true) {
            response.offers.forEach { offer ->
                OfferSummaryRow(
                    title = offer.source,
                    subtitle = offer.note,
                    price = formatCurrency(offer.price, offer.currency),
                    highlight = null,
                    onOpen = offer.url.takeIf { it.isNotBlank() }?.let { url -> { onOpenOffer(url) } }
                )
            }
        } else {
            AiPlaceholderState(
                marker = "RYNEK",
                title = "Brak znalezionych ofert.",
                body = "Po podłączeniu AI tutaj pojawią się: Allegro, Ceneo, iParts i AUTODOC."
            )
        }
    }

    AiHighlightResultCard(
        title = "Najtańsza oferta",
        accentColor = Color(0xFF163B28),
        titleColor = Color(0xFF8EF0AF)
    ) {
        val bestPrice = response?.bestPrice
        if (bestPrice != null) {
            Text(
                text = bestPrice.source,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatCurrency(bestPrice.price),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CF7B9)
            )
            response.saving?.amount
                ?.takeIf { it > 0.0 }
                ?.let {
                    AiMetricPill(
                        label = "Oszczędzasz",
                        value = formatCurrency(it),
                        accentColor = Color(0xFF8EF0AF)
                    )
                } ?: AiEmptyValue("Oczekiwanie na dane.")
        } else {
            AiPlaceholderState(
                marker = "TANIEJ",
                title = "Oczekiwanie na dane.",
                body = "Tu pojawi się najtańsza oferta oraz oszczędność względem partnera OEM."
            )
        }
    }

    AiHighlightResultCard(
        title = "Najbezpieczniejszy wybór",
        accentColor = Color(0xFF132B44),
        titleColor = Color(0xFF9FCEFF)
    ) {
        val safestChoice = response?.bestSafeChoice
        if (safestChoice != null) {
            Text(
                text = safestChoice.source.ifBlank { "Brak danych" },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = safestChoice.reason.ifBlank { "Brak danych" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        } else {
            AiPlaceholderState(
                marker = "PEWNIE",
                title = "Oczekiwanie na rekomendację.",
                body = "Tu pojawi się sklep, który AI uzna za najbezpieczniejszy wybór."
            )
        }
    }

    AiHighlightResultCard(
        title = "Możliwe oszczędności",
        accentColor = Color(0xFF163B28),
        titleColor = Color(0xFF8EF0AF)
    ) {
        val saving = response?.saving
        if (saving != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AiMetricBlock(
                    label = "Kwota",
                    value = saving.amount.takeIf { it > 0.0 }?.let(::formatCurrency) ?: "Brak porównania cen.",
                    accentColor = Color(0xFF8EF0AF),
                    modifier = Modifier.weight(1f)
                )
                AiMetricBlock(
                    label = "Procent",
                    value = saving.percentage.takeIf { it > 0.0 }?.let(::formatPercentage) ?: "Brak porównania cen.",
                    accentColor = Color(0xFF8EF0AF),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AiMetricBlock(
                    label = "Kwota",
                    value = "Oczekiwanie na dane",
                    accentColor = Color(0xFF8EF0AF),
                    modifier = Modifier.weight(1f)
                )
                AiMetricBlock(
                    label = "Procent",
                    value = "Brak porównania cen.",
                    accentColor = Color(0xFF8EF0AF),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF24193C),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Rekomendacja AI",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE3D0FF)
            )
            Text(
                text = if (
                    recommendationText == "Brak rekomendacji" ||
                    recommendationText == noOffersMessage
                ) {
                    "AI nie wygenerowało jeszcze rekomendacji."
                } else {
                    recommendationText
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun OfferSummaryRow(
    title: String,
    subtitle: String,
    price: String,
    highlight: String?,
    onOpen: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = price,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            highlight?.let {
                SummaryBadge(
                    label = it,
                    emphasized = true
                )
            }
            onOpen?.let { action ->
                TextButton(onClick = action) {
                    Text("Otworz oferte")
                }
            }
        }
    }
}

@Composable
private fun AiResultSectionCard(
    title: String,
    accentColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun AiQuerySummaryCard(
    item: ShoppingListItem,
    request: AiPartComparisonRequest,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.46f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AiCompactSummaryValue("Część", item.name)
            AiCompactSummaryValue("OEM", request.oem.ifBlank { "Brak danych" })
            AiCompactSummaryValue("Auto", request.vehicle.orEmpty().ifBlank { "Brak danych" })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Cena partnera OEM",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
                Text(
                    text = item.price.ifBlank { "Brak danych" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8EF0AF)
                )
            }
        }
    }
}

@Composable
private fun AiCompactSummaryValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$label:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AiStatusBanner(
    text: String,
    isLoading: Boolean,
    isError: Boolean,
    isSuccess: Boolean,
) {
    val containerColor = when {
        isError -> Color(0xFF34191C)
        isSuccess -> Color(0xFF163B28)
        isLoading -> Color(0xFF132B44)
        else -> MaterialTheme.colorScheme.background.copy(alpha = 0.52f)
    }
    val textColor = when {
        isError -> Color(0xFFFFB4AB)
        isSuccess -> Color(0xFF8EF0AF)
        isLoading -> Color(0xFF9FCEFF)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AiHighlightResultCard(
    title: String,
    accentColor: Color,
    titleColor: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            content()
        }
    }
}

@Composable
private fun AiMetricBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp
        )
    }
}

@Composable
private fun AiMetricPill(
    label: String,
    value: String,
    accentColor: Color,
) {
    Surface(
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AiPlaceholderState(
    marker: String,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = marker,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun AiEmptyValue(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
    )
}

@Composable
private fun ReceiveShoppingItemDialog(
    item: ShoppingListItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scanPreview by remember { mutableStateOf<Bitmap?>(null) }
    var scanStatus by remember {
        mutableStateOf("Mozesz przyjac czesc recznie albo zeskanowac etykiete przed przeniesieniem do magazynu.")
    }
    var scannedLabel by remember { mutableStateOf<ParsedPartLabel?>(null) }

    fun recognizeBitmap(bitmap: Bitmap) {
        scanPreview = bitmap
        scanStatus = "Odczytuje etykiete..."
        recognizePartLabelFromBitmap(
            bitmap = bitmap,
            onResult = { parsedLabel ->
                scannedLabel = parsedLabel
                val values = buildList {
                    parsedLabel.oemPartNumber?.let { add("OEM: $it") }
                    parsedLabel.manufacturerPartNumber?.let { add("producent: $it") }
                    parsedLabel.manufacturer?.let { add("marka: $it") }
                }
                scanStatus = if (values.isEmpty()) {
                    "Nie udalo sie pewnie odczytac etykiety. Nadal mozesz przyjac czesc recznie."
                } else {
                    "Skan odczytany: ${values.joinToString(" / ")}."
                }
            },
            onError = { message -> scanStatus = message }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            recognizeBitmap(bitmap)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Przyjecie do magazynu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "OEM: ${item.partNumber} / ilosc: ${item.quantity} / naprawa: ${item.repairTitle}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                Text(
                    text = scanStatus,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
                scanPreview?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Skan etykiety czesci",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                TextButton(onClick = { cameraLauncher.launch(null) }) {
                    Text("Zeskanuj etykiete")
                }
                scannedLabel?.let { label ->
                    Text(
                        text = "Odczyt: ${label.oemPartNumber ?: item.partNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Przenies do magazynu")
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
private fun EditShoppingItemDialog(
    item: ShoppingListItem,
    availableRepairs: List<RepairProject>,
    onDismiss: () -> Unit,
    onSave: (ShoppingListItem) -> Unit,
) {
    var name by remember(item) { mutableStateOf(item.name) }
    var oemPartNumber by remember(item) { mutableStateOf(item.partNumber) }
    var manufacturerPartNumber by remember(item) { mutableStateOf(item.manufacturerPartNumber) }
    var manufacturer by remember(item) { mutableStateOf(item.manufacturer) }
    val repairOptions = remember(availableRepairs) {
        availableRepairs
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
            .map { repair -> repair.id to repair.title }
    }
    val initialRepairSelection = remember(item, repairOptions) {
        repairOptions.firstOrNull { (repairId, repairTitle) ->
            repairId == item.repairId || (item.repairId.isBlank() && repairTitle == item.repairTitle)
        }?.first
    }
    var selectedRepairId by remember(item, initialRepairSelection) { mutableStateOf(initialRepairSelection) }
    var area by remember(item) { mutableStateOf(item.area) }
    var quantity by remember(item) { mutableStateOf(item.quantity.toString()) }
    var price by remember(item) { mutableStateOf(item.price) }
    var source by remember(item) { mutableStateOf(item.source) }
    var isAreaPickerVisible by remember { mutableStateOf(false) }
    var isRepairPickerVisible by remember { mutableStateOf(false) }
    val selectedRepairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second.orEmpty()
    val quantityValue = quantity.toIntOrNull()
    val canSave = name.isNotBlank() && quantityValue != null && quantityValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj pozycje zakupowa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GarageTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nazwa czesci",
                    modifier = Modifier.fillMaxWidth()
                )
                GarageTextField(
                    value = oemPartNumber,
                    onValueChange = { oemPartNumber = it },
                    label = "Kod OEM",
                    modifier = Modifier.fillMaxWidth()
                )
                GarageTextField(
                    value = manufacturerPartNumber,
                    onValueChange = { manufacturerPartNumber = it },
                    label = "Nr producenta",
                    modifier = Modifier.fillMaxWidth()
                )
                GarageTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = "Producent",
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { isRepairPickerVisible = !isRepairPickerVisible }) {
                    Text(
                        if (selectedRepairTitle.isBlank()) {
                            "Naprawa: brak przypisania"
                        } else {
                            "Naprawa: $selectedRepairTitle"
                        }
                    )
                }
                if (isRepairPickerVisible) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRepairId = null
                                    isRepairPickerVisible = false
                                },
                            color = if (selectedRepairId == null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Brak przypisania",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                color = if (selectedRepairId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        repairOptions.forEach { (repairId, repairTitle) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRepairId = repairId
                                        isRepairPickerVisible = false
                                    },
                                color = if (repairId == selectedRepairId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = repairTitle,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (repairId == selectedRepairId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { isAreaPickerVisible = !isAreaPickerVisible }) {
                    Text("Katalog: ${area.label}")
                }
                if (isAreaPickerVisible) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VehicleArea.entries.forEach { candidate ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        area = candidate
                                        isAreaPickerVisible = false
                                    },
                                color = if (candidate == area) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = candidate.label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (candidate == area) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                GarageTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { character -> character.isDigit() } },
                    label = "Ilosc",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Number
                )
                GarageTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Cena",
                    modifier = Modifier.fillMaxWidth()
                )
                GarageTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = "Zrodlo",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val repairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second.orEmpty()
                    onSave(
                        item.copy(
                            partNumber = oemPartNumber.trim(),
                            manufacturerPartNumber = manufacturerPartNumber.trim(),
                            name = name.trim(),
                            manufacturer = manufacturer.trim(),
                            repairTitle = repairTitle,
                            repairId = selectedRepairId.orEmpty(),
                            area = area,
                            quantity = quantityValue ?: item.quantity,
                            source = source.trim().ifBlank { item.source },
                            price = price.trim()
                        )
                    )
                }
            ) {
                Text("Zapisz")
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
private fun ConfirmDeleteShoppingItemDialog(
    item: ShoppingListItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usunac czesc z listy zakupow?") },
        text = {
            Text(item.name)
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
private fun ConsumableRow(item: ConsumableItem) {
    PartLikeRow(
        title = item.name,
        subtitle = "${item.producer} / ${item.quantity}",
        meta = "Cena: ${item.purchasePrice}",
        relation = item.notes
    )
}

@Composable
private fun PartLikeRow(
    title: String,
    subtitle: String,
    meta: String,
    relation: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Text(
                text = meta,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = relation,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun EmptyPartsRow(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
        )
    }
}

@Composable
private fun AddPartEntryDialog(
    onDismiss: () -> Unit,
    onManualAdd: () -> Unit,
    onExternalAdd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj czesc") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AddPartModeRow(
                    title = "Dodanie reczne",
                    subtitle = "Sam wpisujesz dane rekordu w magazynie.",
                    onClick = onManualAdd
                )
                AddPartModeRow(
                    title = "Dodaj z RealOEM / Czescidobmw",
                    subtitle = "Pobieranie danych po numerze czesci obsluzymy w kolejnym kroku.",
                    onClick = onExternalAdd
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun ConfirmDeletePartDialog(
    part: PartInventoryItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usun rekord") },
        text = {
            Text("Czy usunac z magazynu: ${part.name}?")
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
private fun AddPartModeRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }
    }
}

@Composable
private fun ManualPartEntryDialog(
    nextId: String,
    availableRepairs: List<RepairProject>,
    initialPart: PartInventoryItem? = null,
    onDismiss: () -> Unit,
    onSave: (PartInventoryItem) -> Unit,
) {
    var oemPartNumber by remember(initialPart) { mutableStateOf(initialPart?.oemPartNumber.orEmpty()) }
    var manufacturerPartNumber by remember(initialPart) { mutableStateOf(initialPart?.manufacturerPartNumber.orEmpty()) }
    var name by remember(initialPart) { mutableStateOf(initialPart?.name.orEmpty()) }
    var manufacturer by remember(initialPart) { mutableStateOf(initialPart?.manufacturer.orEmpty()) }
    val repairOptions = remember(availableRepairs) {
        availableRepairs
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
            .map { repair -> repair.id to repair.title }
    }
    val initialRepairSelection = remember(initialPart, repairOptions) {
        repairOptions.firstOrNull { (repairId, repairTitle) ->
            repairId == initialPart?.repairId || (initialPart?.repairId.isNullOrBlank() && repairTitle == initialPart?.repairTitle)
        }?.first
    }
    var selectedRepairId by remember(initialPart, initialRepairSelection) { mutableStateOf(initialRepairSelection) }
    var quantity by remember(initialPart) { mutableStateOf(initialPart?.quantity?.toString() ?: "1") }
    var purchasePrice by remember(initialPart) { mutableStateOf(initialPart?.purchasePrice.orEmpty()) }
    var isRepairPickerVisible by remember { mutableStateOf(false) }
    val selectedRepairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second.orEmpty()

    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPart == null) "Dodanie reczne" else "Edycja rekordu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = oemPartNumber,
                    onValueChange = { oemPartNumber = it },
                    label = "Nr czesci OEM BMW",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 333267..."
                )
                GarageTextField(
                    value = manufacturerPartNumber,
                    onValueChange = { manufacturerPartNumber = it },
                    label = "Nr czesci producenta",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 37832 01/LMI"
                )
                GarageTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nazwa czesci",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Sruba mocowania zwrotnicy"
                )
                GarageTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = "Producent",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. BMW / Lemforder"
                )
                TextButton(onClick = { isRepairPickerVisible = !isRepairPickerVisible }) {
                    Text(
                        if (selectedRepairTitle.isBlank()) {
                            "Naprawa: brak przypisania"
                        } else {
                            "Naprawa: $selectedRepairTitle"
                        }
                    )
                }
                if (isRepairPickerVisible) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRepairId = null
                                    isRepairPickerVisible = false
                                },
                            color = if (selectedRepairId == null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Brak przypisania",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                color = if (selectedRepairId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        repairOptions.forEach { (repairId, repairTitle) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRepairId = repairId
                                        isRepairPickerVisible = false
                                    },
                                color = if (repairId == selectedRepairId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = repairTitle,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (repairId == selectedRepairId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                GarageTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = "Ilosc",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "1"
                )
                GarageTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = "Cena zakupu",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 45 zl"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val repairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second
                    onSave(
                        PartInventoryItem(
                            id = nextId,
                            oemPartNumber = oemPartNumber.ifBlank { "do uzupelnienia" },
                            manufacturerPartNumber = manufacturerPartNumber.ifBlank { "do uzupelnienia" },
                            name = name,
                            manufacturer = manufacturer.ifBlank { "do uzupelnienia" },
                            repairTitle = repairTitle,
                            quantity = quantity.toIntOrNull() ?: 1,
                            purchasePrice = purchasePrice.ifBlank { "do uzupelnienia" },
                            realOemUrl = initialPart?.realOemUrl,
                            photoUri = initialPart?.photoUri,
                            repairId = selectedRepairId
                        )
                    )
                }
            ) {
                Text(if (initialPart == null) "Dodaj rekord" else "Zapisz zmiany")
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
private fun ShoppingPartLookupDialog(
    nextId: String,
    initialRepairTitle: String,
    initialArea: VehicleArea,
    onDismiss: () -> Unit,
    onSave: (ShoppingListItem) -> Unit,
) {
    var oemPartNumber by remember { mutableStateOf("") }
    var repairTitle by remember { mutableStateOf(initialRepairTitle) }
    var area by remember { mutableStateOf(initialArea) }
    var isAreaPickerVisible by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var lookupResults by remember { mutableStateOf(emptyList<MockPartLookupResult>()) }
    var selectedResult by remember { mutableStateOf<MockPartLookupResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val quantityValue = quantity.toIntOrNull()
    val canSave = selectedResult != null && repairTitle.isNotBlank() && quantityValue != null && quantityValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lista zakupow") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Wpisz numer OEM. Aplikacja pobierze dostepne czesci, ceny i zdjecia z obslugiwanego sklepu BMW.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = repairTitle,
                    onValueChange = { repairTitle = it },
                    label = "Naprawa",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Tylna zwrotnica lewa"
                )
                TextButton(onClick = { isAreaPickerVisible = !isAreaPickerVisible }) {
                    Text("Katalog: ${area.label}")
                }
                if (isAreaPickerVisible) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VehicleArea.entries.forEach { candidate ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        area = candidate
                                        isAreaPickerVisible = false
                                    },
                                color = if (candidate == area) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = candidate.label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (candidate == area) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                GarageTextField(
                    value = oemPartNumber,
                    onValueChange = {
                        oemPartNumber = it
                        lookupResults = emptyList()
                        selectedResult = null
                        searchError = null
                    },
                    label = "Kod OEM",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 33326763463"
                )
                GarageTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { character -> character.isDigit() } },
                    label = "Ilosc",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "1",
                    keyboardType = KeyboardType.Number
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = oemPartNumber.isNotBlank() && !isSearching,
                        onClick = {
                            coroutineScope.launch {
                                isSearching = true
                                searchError = null
                                selectedResult = null
                                lookupResults = runCatching {
                                    fetchCzescidobmwResults(oemPartNumber)
                                }.recoverCatching {
                                    listOf(mockPartLookup(oemPartNumber))
                                }.getOrDefault(emptyList())
                                if (lookupResults.isEmpty()) {
                                    searchError = "Nie znaleziono wynikow dla tego OEM."
                                }
                                isSearching = false
                            }
                        }
                    ) {
                        Text(if (isSearching) "Szukam..." else "Szukaj w sklepie")
                    }
                    TextButton(
                        onClick = {
                            oemPartNumber = "33326763463"
                            lookupResults = emptyList()
                            selectedResult = null
                            searchError = null
                        }
                    ) {
                        Text("Przyklad")
                    }
                }
                searchError?.let { error ->
                    EmptyPartsRow(error)
                }
                if (lookupResults.isNotEmpty()) {
                    Text(
                        text = "Wybierz produkt do listy",
                        fontWeight = FontWeight.SemiBold
                    )
                    lookupResults.forEach { lookup ->
                        LookupResultCard(
                            title = lookup.manufacturer,
                            subtitle = lookup.name,
                            primary = "Cena brutto: ${lookup.shopPrice}",
                            secondary = "OEM: ${lookup.oemPartNumber} / Producent: ${lookup.manufacturerPartNumber}",
                            source = lookup.shopUrl,
                            imageUrl = lookup.imageUrl,
                            imageSearchUrl = lookup.imageSearchUrl,
                            isSelected = selectedResult == lookup,
                            onClick = { selectedResult = lookup }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val lookup = selectedResult ?: return@TextButton
                    onSave(
                        ShoppingListItem(
                            id = nextId,
                            partNumber = lookup.oemPartNumber,
                            manufacturerPartNumber = lookup.manufacturerPartNumber,
                            name = lookup.name,
                            manufacturer = lookup.manufacturer,
                            repairTitle = repairTitle.trim(),
                            area = area,
                            quantity = quantityValue ?: 1,
                            source = "czescidobmw.pl",
                            price = lookup.shopPrice,
                            imageUri = lookup.imageUrl,
                            shopUrl = lookup.shopUrl,
                            realOemUrl = lookup.realOemUrl
                        )
                    )
                }
            ) {
                Text("Dodaj do zakupow")
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
fun ExternalPartLookupDialog(
    nextId: String,
    availableRepairs: List<RepairProject>,
    initialRepairTitle: String = "",
    initialRepairId: String? = null,
    onDismiss: () -> Unit,
    onSave: (PartInventoryItem) -> Unit,
) {
    var partNumber by remember { mutableStateOf("") }
    val repairOptions = remember(availableRepairs) {
        availableRepairs
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
            .map { repair -> repair.id to repair.title }
    }
    val initialRepairSelection = remember(initialRepairId, initialRepairTitle, repairOptions) {
        repairOptions.firstOrNull { (repairId, repairTitle) ->
            repairId == initialRepairId || (initialRepairId.isNullOrBlank() && repairTitle == initialRepairTitle)
        }?.first
    }
    var selectedRepairId by remember(initialRepairSelection) { mutableStateOf(initialRepairSelection) }
    var quantity by remember { mutableStateOf("1") }
    var lookupResults by remember { mutableStateOf(emptyList<MockPartLookupResult>()) }
    var selectedResult by remember { mutableStateOf<MockPartLookupResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var scanPreview by remember { mutableStateOf<Bitmap?>(null) }
    var scanStatus by remember { mutableStateOf("Zrob zdjecie etykiety albo wybierz je z telefonu. Aplikacja sprobuje odczytac tekst, QR i kod kreskowy.") }
    var scannedManufacturerPartNumber by remember { mutableStateOf<String?>(null) }
    var scannedManufacturer by remember { mutableStateOf<String?>(null) }
    var isRepairPickerVisible by remember { mutableStateOf(false) }
    val selectedRepairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second.orEmpty()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val quantityValue = quantity.toIntOrNull()
    val canAddToInventory = selectedResult != null && quantityValue != null && quantityValue > 0

    fun applyParsedLabel(parsedLabel: ParsedPartLabel, source: String) {
        partNumber = parsedLabel.oemPartNumber.orEmpty()
        scannedManufacturerPartNumber = parsedLabel.manufacturerPartNumber
        scannedManufacturer = parsedLabel.manufacturer
        lookupResults = emptyList()
        selectedResult = null
        searchError = null

        val recognizedValues = buildList {
            parsedLabel.oemPartNumber?.let { add("OEM BMW: $it") }
            parsedLabel.manufacturerPartNumber?.let { add("nr producenta: $it") }
            parsedLabel.manufacturer?.let { add("producent: $it") }
        }

        scanStatus = if (recognizedValues.isEmpty()) {
            "Nie udalo sie pewnie odczytac numerow z $source. Sprobuj zrobic zdjecie blizej etykiety."
        } else {
            "Odczyt z $source: ${recognizedValues.joinToString(" / ")}."
        }
    }

    fun recognizeBitmap(bitmap: Bitmap, source: String) {
        scanPreview = bitmap
        scanStatus = "Odczytuje etykiete z $source..."
        recognizePartLabelFromBitmap(
            bitmap = bitmap,
            onResult = { parsedLabel -> applyParsedLabel(parsedLabel, source) },
            onError = { message -> scanStatus = message }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            recognizeBitmap(bitmap, "aparatu")
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                recognizeBitmap(bitmap, "galerii")
            } else {
                scanStatus = "Nie udalo sie wczytac zdjecia z telefonu."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj z RealOEM / Czescidobmw") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tryb testowy odczytu",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Po rozpoznaniu numeru OEM mozesz od razu wyszukac czesc w Czescidobmw.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
                GarageTextField(
                    value = partNumber,
                    onValueChange = {
                        partNumber = it
                        lookupResults = emptyList()
                        selectedResult = null
                        searchError = null
                    },
                    label = "Nr czesci OEM BMW",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 33326760349"
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Odczyt aparatem",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = scanStatus,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                        scanPreview?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Zdjecie do odczytu numeru czesci",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { cameraLauncher.launch(null) }) {
                                Text("Zrob zdjecie")
                            }
                            TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Text("Wybierz zdjecie")
                            }
                            TextButton(
                                onClick = {
                                    val parsedLabel = parsePartLabelText(
                                        """
                                        Nr No. 44158
                                        Vergl-Nr./Repl.-No. 11 61 7 503 520
                                        Passend/To fit: BMW
                                        febi bilstein
                                        """.trimIndent()
                                    )
                                    applyParsedLabel(parsedLabel, "testu FEBI")
                                }
                            ) {
                                Text("Test FEBI")
                            }
                        }
                        if (scannedManufacturerPartNumber != null || scannedManufacturer != null) {
                            Text(
                                text = "Rozpoznano producenta: ${scannedManufacturer ?: "brak"} / nr producenta: ${scannedManufacturerPartNumber ?: "brak"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                GarageTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.filter { character -> character.isDigit() }
                    },
                    label = "Ilosc czesci",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "1",
                    keyboardType = KeyboardType.Number
                )
                TextButton(
                    onClick = {
                        partNumber = "33326763463"
                        lookupResults = emptyList()
                        selectedResult = null
                        searchError = null
                    }
                ) {
                    Text("Zasymuluj skan QR / numeru")
                }
                TextButton(
                    enabled = partNumber.isNotBlank() && !isSearching,
                    onClick = {
                        coroutineScope.launch {
                            isSearching = true
                            searchError = null
                            selectedResult = null
                            lookupResults = runCatching {
                                fetchCzescidobmwResults(partNumber)
                            }.onFailure {
                                searchError = "Nie udalo sie pobrac wynikow z czescidobmw.pl."
                            }.getOrDefault(emptyList())
                            isSearching = false
                        }
                    }
                ) {
                    Text(if (isSearching) "Szukam..." else "Szukaj")
                }
                searchError?.let { error ->
                    EmptyPartsRow(error)
                }
                if (!isSearching && partNumber.isNotBlank() && lookupResults.isEmpty() && searchError == null) {
                    Text(
                        text = "Po wyszukaniu wybierzesz konkretna pozycje z listy producentow i cen.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
                if (lookupResults.isNotEmpty()) {
                    Text(
                        text = "Wybierz czesc do dodania",
                        fontWeight = FontWeight.SemiBold
                    )
                    lookupResults.forEach { lookup ->
                        LookupResultCard(
                            title = lookup.manufacturer,
                            subtitle = lookup.name,
                            primary = "Cena brutto: ${lookup.shopPrice}",
                            secondary = "OEM: ${lookup.oemPartNumber} / Producent: ${lookup.manufacturerPartNumber}",
                            source = lookup.shopUrl,
                            imageUrl = lookup.imageUrl,
                            imageSearchUrl = lookup.imageSearchUrl,
                            isSelected = selectedResult == lookup,
                            onClick = { selectedResult = lookup }
                        )
                    }
                    TextButton(onClick = { isRepairPickerVisible = !isRepairPickerVisible }) {
                        Text(
                            if (selectedRepairTitle.isBlank()) {
                                "Naprawa: brak przypisania"
                            } else {
                                "Naprawa: $selectedRepairTitle"
                            }
                        )
                    }
                    if (isRepairPickerVisible) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRepairId = null
                                        isRepairPickerVisible = false
                                    },
                                color = if (selectedRepairId == null) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Brak przypisania",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (selectedRepairId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            repairOptions.forEach { (repairId, repairTitle) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRepairId = repairId
                                            isRepairPickerVisible = false
                                        },
                                    color = if (repairId == selectedRepairId) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = repairTitle,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        color = if (repairId == selectedRepairId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAddToInventory,
                onClick = {
                    val lookup = selectedResult ?: return@TextButton
                    val repairTitle = repairOptions.firstOrNull { it.first == selectedRepairId }?.second
                    onSave(
                        PartInventoryItem(
                            id = nextId,
                            oemPartNumber = lookup.oemPartNumber,
                            manufacturerPartNumber = lookup.manufacturerPartNumber,
                            name = lookup.name,
                            manufacturer = lookup.manufacturer,
                            repairTitle = repairTitle,
                            quantity = quantityValue ?: 1,
                            purchasePrice = lookup.shopPrice,
                            realOemUrl = lookup.realOemUrl,
                            photoUri = lookup.imageUrl,
                            repairId = selectedRepairId
                        )
                    )
                }
            ) {
                Text("Dodaj do magazynu")
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
fun LookupResultCard(
    title: String,
    subtitle: String,
    primary: String,
    secondary: String,
    source: String,
    imageUrl: String? = null,
    imageSearchUrl: String? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (isSelected) "$title / wybrane" else title,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            if (imageUrl != null) {
                Surface(
                    modifier = Modifier.width(132.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    PartPhotoContent(photoUri = imageUrl)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Zdjecie: do znalezienia w internecie",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                    if (imageSearchUrl != null) {
                        TextButton(onClick = { uriHandler.openUri(imageSearchUrl) }) {
                            Text("Szukaj zdjecia po numerze")
                        }
                    }
                }
            }
            Text(
                text = subtitle,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = primary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Text(
                text = secondary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Text(
                text = source,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun mockPartLookup(partNumber: String): MockPartLookupResult {
    val normalizedPartNumber = partNumber.filter { it.isLetterOrDigit() }.ifBlank { "33326760349" }
    return MockPartLookupResult(
        oemPartNumber = normalizedPartNumber,
        manufacturerPartNumber = normalizedPartNumber,
        name = "Sruba mocowania tylnej zwrotnicy",
        manufacturer = "BMW / OEM",
        realOemPrice = "ok. 8.42 EUR",
        shopPrice = "39.90 zl",
        diagram = "Tylna os / wahacze / zwrotnica",
        realOemUrl = "https://www.realoem.com/bmw/partxref?q=$normalizedPartNumber",
        shopUrl = "https://czescidobmw.pl/szukaj?search=$normalizedPartNumber",
        imageSource = "mock: zdjecie produktu z katalogu sklepu",
        imageUrl = null,
        imageSearchUrl = imageSearchUrlFor(normalizedPartNumber, "BMW / OEM")
    )
}

suspend fun fetchCzescidobmwResults(partNumber: String): List<MockPartLookupResult> =
    withContext(Dispatchers.IO) {
        val normalizedPartNumber = partNumber.filter { it.isLetterOrDigit() }
        if (normalizedPartNumber.isBlank()) return@withContext emptyList()

        val encodedPartNumber = URLEncoder.encode(normalizedPartNumber, "UTF-8")
        val searchUrl = "https://czescidobmw.pl/wyniki-wyszukiwania?q=$encodedPartNumber"
        val connection = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BmwGarageAssistant/0.1")
        }

        val responseStream = if (connection.responseCode >= 400) {
            connection.errorStream ?: connection.inputStream
        } else {
            connection.inputStream
        }
        val html = responseStream.bufferedReader().use { it.readText() }
        parseCzescidobmwResults(
            html = html,
            searchUrl = searchUrl,
            searchedOemPartNumber = normalizedPartNumber
        )
    }


fun recognizePartLabelFromBitmap(
    bitmap: Bitmap,
    onResult: (ParsedPartLabel) -> Unit,
    onError: (String) -> Unit,
) {
    val image = InputImage.fromBitmap(bitmap, 0)

    BarcodeScanning.getClient()
        .process(image)
        .addOnSuccessListener { barcodes ->
            val barcodeValues = barcodes.mapNotNull { barcode -> barcode.rawValue }
            recognizePartLabelTextFromImage(
                image = image,
                barcodeValues = barcodeValues,
                onResult = onResult,
                onError = onError
            )
        }
        .addOnFailureListener {
            recognizePartLabelTextFromImage(
                image = image,
                barcodeValues = emptyList(),
                onResult = onResult,
                onError = onError
            )
        }
}

private fun recognizePartLabelTextFromImage(
    image: InputImage,
    barcodeValues: List<String>,
    onResult: (ParsedPartLabel) -> Unit,
    onError: (String) -> Unit,
) {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { recognizedText ->
            val combinedText = buildString {
                appendLine(recognizedText.text)
                barcodeValues.forEach { value -> appendLine(value) }
            }
            val parsedLabel = parsePartLabelText(combinedText)

            if (parsedLabel.oemPartNumber == null && parsedLabel.manufacturerPartNumber == null) {
                onError("Nie udalo sie rozpoznac numeru czesci. Sprobuj zrobic zdjecie blizej etykiety i dobrze doswietlic kod.")
            } else {
                onResult(parsedLabel)
            }
        }
        .addOnFailureListener {
            onError("OCR nie odczytal tekstu z etykiety. Sprobuj ponownie albo wybierz zdjecie z galerii.")
        }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()

private fun allegroSearchUrlFor(query: String): String {
    val normalizedQuery = query.ifBlank { "BMW czesci" }
    val encoded = URLEncoder.encode(normalizedQuery, "UTF-8")
    return "https://allegro.pl/listing?string=$encoded"
}

private fun resolveShoppingDiagramImageUrl(url: String): String =
    runCatching {
        if (
            url.contains("img.altechopersys.com", ignoreCase = true) ||
            url.endsWith(".jpg", ignoreCase = true) ||
            url.endsWith(".jpeg", ignoreCase = true) ||
            url.endsWith(".png", ignoreCase = true) ||
            url.endsWith(".webp", ignoreCase = true)
        ) {
            return@runCatching url
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BmwGarageAssistant/0.1")
        }
        val html = (if (connection.responseCode >= 400) {
            connection.errorStream ?: connection.inputStream
        } else {
            connection.inputStream
        }).bufferedReader().use { it.readText() }

        Regex(
            pattern = "<img[^>]+src=\"([^\"]+)\"[^>]*>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html)
            .mapNotNull { match -> match.groupValues.getOrNull(1) }
            .map { decodeHtmlCompat(it) }
            .firstOrNull { src ->
                src.contains("img.altechopersys.com", ignoreCase = true) &&
                    !src.endsWith(".svg", ignoreCase = true)
            }
            ?.let(::absoluteCzescidobmwUrl)
            ?: ""
    }.getOrDefault("")

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun VehiclePartsStorageScreenPreview() {
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

    GarageTheme {
        VehiclePartsStorageScreen(
            vehicle = vehicle,
            inventoryParts = sampleInventoryPartsFor(vehicle),
            shoppingList = sampleShoppingListFor(vehicle),
            consumables = sampleConsumablesFor(),
            onBack = {}
        )
    }
}
