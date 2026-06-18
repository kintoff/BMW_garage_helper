package pl.garage.bmwassistant.ui.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleActiveRepairAreasFor
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.garageBottomContentPadding
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun VehicleStatusScreen(
    vehicle: Vehicle,
    activeRepairAreas: Set<VehicleArea>,
    onBack: () -> Unit,
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
) {
    var isVinDecoderOpen by rememberSaveable { mutableStateOf(false) }

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
                    bottom = garageBottomContentPadding(hasBottomBar = bottomBar != null)
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TextButton(onClick = onBack) {
                        Text("Wroc do auta")
                    }
                }

                item {
                    Header(
                        title = "Stan auta",
                        subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                    )
                }

                item {
                    VehicleFactsCard(
                        vehicle = vehicle,
                        isVinDecoderOpen = isVinDecoderOpen,
                        onToggleVinDecoder = { isVinDecoderOpen = !isVinDecoderOpen }
                    )
                }

                item {
                    SectionTitle("Kategorie")
                }

                item {
                    VehicleAreaGrid(
                        areas = VehicleArea.entries,
                        activeRepairAreas = activeRepairAreas
                    )
                }

                item {
                    QuickScanCard()
                }

                item {
                    AppInfoCard()
                }
            }
            bottomBar?.invoke(this)
        }
    }
}

@Composable
private fun VehicleFactsCard(
    vehicle: Vehicle,
    isVinDecoderOpen: Boolean,
    onToggleVinDecoder: () -> Unit,
) {
    val cleanVin = vehicle.vin.cleanVin()
    val hasValidVin = cleanVin.length == 17

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dane auta",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Rok: ${vehicle.year.ifBlank { "do uzupelnienia" }}")
            Text("Przebieg: ${vehicle.mileage.ifBlank { "do uzupelnienia" }}")
            Text("VIN: ${vehicle.vin.ifBlank { "do uzupelnienia" }}")
            Text(
                text = "Dane mozna wpisac recznie. Dekoder VIN korzysta z bimmer.work i wymaga internetu.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            Button(
                onClick = onToggleVinDecoder,
                enabled = hasValidVin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isVinDecoderOpen) "Ukryj dekoder VIN" else "Dekoduj VIN")
            }
            if (!hasValidVin) {
                Text(
                    text = "Wpisz pelny VIN auta, 17 znakow, aby uruchomic dekoder.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 13.sp
                )
            }
            if (isVinDecoderOpen && hasValidVin) {
                VinDecoderWebView(vin = cleanVin)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VinDecoderWebView(vin: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07121A))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
                    overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
                    setOnTouchListener { view, event ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.evaluateJavascript(
                                """
                                    (function() {
                                      var vin = "$vin";
                                      var input = document.querySelector('input[name="vin"], input[id="vin"], input[type="text"], input:not([type])');
                                      if (input) {
                                        input.value = vin;
                                        input.dispatchEvent(new Event('input', { bubbles: true }));
                                        input.dispatchEvent(new Event('change', { bubbles: true }));
                                      }
                                    })();
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                    loadUrl("https://bimmer.work/")
                }
            },
            update = { webView ->
                if (webView.url.isNullOrBlank()) {
                    webView.loadUrl("https://bimmer.work/")
                }
            }
        )
    }
}

@Composable
private fun VehicleAreaGrid(
    areas: List<VehicleArea>,
    activeRepairAreas: Set<VehicleArea>,
) {
    val rowCount = (areas.size + 1) / 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 162).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(areas) { area ->
            VehicleAreaTile(
                area = area,
                hasActiveRepair = area in activeRepairAreas
            )
        }
    }
}

@Composable
private fun VehicleAreaTile(
    area: VehicleArea,
    hasActiveRepair: Boolean,
) {
    val accentColor = if (hasActiveRepair) Color(0xFFE2C16B) else Color(0xFF395064)
    val statusText = if (hasActiveRepair) "Naprawa" else "OK"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
            .clickable { },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    shape = RoundedCornerShape(50)
                ) {}
            }
            Image(
                painter = painterResource(area.iconResource()),
                contentDescription = area.label,
                modifier = Modifier.height(34.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = area.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = area.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    maxLines = 3
                )
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QuickScanCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Szybki przeglad",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Docelowo pobierze bledy z BimmerTool i przypisze je do kategorii auta.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            Button(onClick = {}) {
                Text("Pozniej")
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Informacje o aplikacji",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Wersja pre alfa aplikacji BMW Garage Helper")
            Text("Aplikacja zawiera błędy")
            Text("Błędy proszę zgłaszać na wątku grupie facebook")
            Text("Autor Jędrzej Czapracki")
        }
    }
}

private fun String.cleanVin(): String =
    filter { it.isLetterOrDigit() }.uppercase()

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun VehicleStatusScreenPreview() {
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
        VehicleStatusScreen(
            vehicle = vehicle,
            activeRepairAreas = sampleActiveRepairAreasFor(vehicle),
            onBack = {}
        )
    }
}
