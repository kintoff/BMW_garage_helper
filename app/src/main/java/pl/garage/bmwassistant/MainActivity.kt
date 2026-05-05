package pl.garage.bmwassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GarageTheme {
                GarageDashboard()
            }
        }
    }
}

private data class Vehicle(
    val name: String,
    val engine: String,
    val status: String,
    val activeRepair: String,
)

private data class GarageTask(
    val title: String,
    val vehicle: String,
    val status: String,
)

private val vehicles = listOf(
    Vehicle(
        name = "BMW E60 520d",
        engine = "M47N2 2.0d",
        status = "Aktywne prace",
        activeRepair = "Tylna zwrotnica lewa, zardzewiala sruba"
    ),
    Vehicle(
        name = "BMW #2",
        engine = "Do uzupelnienia",
        status = "Profil do stworzenia",
        activeRepair = "Dodaj VIN, silnik i aktualne naprawy"
    ),
    Vehicle(
        name = "BMW #3",
        engine = "Do uzupelnienia",
        status = "Profil do stworzenia",
        activeRepair = "Dodaj historie serwisowa"
    ),
    Vehicle(
        name = "BMW #4",
        engine = "Do uzupelnienia",
        status = "Profil do stworzenia",
        activeRepair = "Dodaj plan napraw"
    )
)

private val activeTasks = listOf(
    GarageTask(
        title = "Ustalic numer sruby tylnej zwrotnicy",
        vehicle = "E60 520d",
        status = "Do sprawdzenia w RealOEM"
    ),
    GarageTask(
        title = "Zrobic zdjecia mocowania od strony lewego kola",
        vehicle = "E60 520d",
        status = "Garaż"
    ),
    GarageTask(
        title = "Przygotowac liste narzedzi do demontazu",
        vehicle = "E60 520d",
        status = "Planowanie"
    )
)

@Composable
private fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF245B73),
            secondary = Color(0xFF7A5C36),
            background = Color(0xFFF7F2EA),
            surface = Color(0xFFFFFBF5),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF211C18),
            onSurface = Color(0xFF211C18)
        ),
        content = content
    )
}

@Composable
private fun GarageDashboard() {
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
                Header()
            }

            item {
                SectionTitle("Moje auta")
            }

            items(vehicles) { vehicle ->
                VehicleCard(vehicle)
            }

            item {
                SectionTitle("Aktywne zadania")
            }

            items(activeTasks) { task ->
                TaskRow(task)
            }

            item {
                SectionTitle("Pierwszy modul")
            }

            item {
                FirstModuleCard()
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "BMW Garage Assistant",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Prywatne centrum napraw, czesci, zdjec i notatek dla Twoich BMW.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 8.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun VehicleCard(vehicle: Vehicle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = vehicle.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vehicle.engine,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                StatusPill(vehicle.status)
            }

            Text(
                text = vehicle.activeRepair,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TaskRow(task: GarageTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(task.vehicle) })
                AssistChip(onClick = {}, label = { Text(task.status) })
            }
        }
    }
}

@Composable
private fun FirstModuleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Projekt naprawy: E60 tylna zwrotnica",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Pierwszy workflow bedzie laczyl opis problemu, zdjecia, liste czesci, RealOEM, notatki i checklisty.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Nastepny krok: dodanie lokalnej bazy i formularza naprawy.",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun GarageDashboardPreview() {
    GarageTheme {
        GarageDashboard()
    }
}
