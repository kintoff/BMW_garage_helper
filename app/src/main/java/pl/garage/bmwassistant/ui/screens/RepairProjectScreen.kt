package pl.garage.bmwassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.garage.bmwassistant.data.sampleRepairFor
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.components.ChecklistRow
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.InfoCard
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.SimpleListRow
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun RepairProjectScreen(
    project: RepairProject,
    onBack: () -> Unit,
) {
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
                TextButton(onClick = onBack) {
                    Text("Wroc do garazu")
                }
            }

            item {
                Header(
                    title = project.title,
                    subtitle = project.vehicleName
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(project.status) })
                    AssistChip(onClick = {}, label = { Text(project.priority) })
                }
            }

            item {
                InfoCard(
                    title = "Problem",
                    body = project.problemDescription
                )
            }

            item {
                InfoCard(
                    title = "Cel naprawy",
                    body = project.goal
                )
            }

            item {
                SectionTitle("Checklista")
            }

            items(project.checklist) { item ->
                ChecklistRow(item)
            }

            item {
                SectionTitle("Czesci do ustalenia")
            }

            items(project.partsToIdentify) { part ->
                SimpleListRow(part, "Do identyfikacji")
            }

            item {
                SectionTitle("Dokumenty i linki")
            }

            items(project.documentsToCollect) { document ->
                SimpleListRow(document, "Do zebrania")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun RepairProjectScreenPreview() {
    GarageTheme {
        RepairProjectScreen(
            project = sampleRepairFor(
                Vehicle(
                    brand = "BMW",
                    model = "E60 520d",
                    generation = "E60",
                    engine = "M47N2 2.0d",
                    year = "2006",
                    vin = "",
                    mileage = "285000",
                    note = ""
                )
            ),
            onBack = {}
        )
    }
}
