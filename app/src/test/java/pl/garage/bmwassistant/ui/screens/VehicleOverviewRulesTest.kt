package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.data.ImportedRepairArchive
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea

class VehicleOverviewRulesTest {

    @Test
    fun sameRepairTitleIgnoresWhitespaceAndCase() {
        assertTrue("  Tylna   Zwrotnica ".hasSameRepairTitleAs("tylna zwrotnica"))
        assertFalse("".hasSameRepairTitleAs("tylna zwrotnica"))
    }

    @Test
    fun belongsToRepairFallsBackToTitleAndAreaWhenIdsAreMissing() {
        val repair = repair()
        val documentation = documentation(repair).copy(repairId = "")
        val shopping = shoppingItem(repairTitle = repair.title).copy(repairId = "")
        val inventory = PartInventoryItem(
            id = "inventory_1",
            oemPartNumber = "11428575211",
            manufacturerPartNumber = "MANN-HU816X",
            name = "Filtr oleju",
            manufacturer = "Mann",
            repairTitle = repair.title,
            quantity = 1,
            purchasePrice = "42.50",
            realOemUrl = null,
            repairId = null
        )

        assertTrue(documentation.belongsToRepair(repair))
        assertTrue(shopping.belongsToRepair(repair))
        assertTrue(inventory.belongsToRepair(repair))
    }

    @Test
    fun nextAvailableRepairTitleFindsNextFreeSuffix() {
        val repairs = listOf(
            repair(title = "Importowana naprawa"),
            repair(title = "Importowana naprawa (2)"),
            repair(title = "Importowana naprawa (3)")
        )

        assertEquals("Importowana naprawa (4)", repairs.nextAvailableRepairTitle("Importowana naprawa"))
    }

    @Test
    fun nextAvailableRepairTitleUsesDefaultBaseWhenInputIsBlank() {
        val repairs = listOf(repair(title = "Importowana naprawa (2)"))

        assertEquals("Importowana naprawa (3)", repairs.nextAvailableRepairTitle("   "))
    }

    @Test
    fun mergeArchivedShoppingItemsCombinesQuantitiesAndKeepsBestMetadata() {
        val repair = repair()
        val merged = listOf(
            shoppingItem(
                id = "",
                partNumber = "33326763092",
                manufacturerPartNumber = "LEM-123",
                quantity = 1,
                source = "Magazyn"
            ),
            shoppingItem(
                id = "remote",
                partNumber = "",
                manufacturerPartNumber = "LEM-123",
                quantity = 2,
                source = "Autodoc",
                price = "249.99",
                shopUrl = "https://example.com",
                imageUri = "content://photo/1"
            )
        ).mergeArchivedShoppingItems(repair)

        assertEquals(1, merged.size)
        assertEquals(3, merged.single().quantity)
        assertEquals("Autodoc", merged.single().source)
        assertEquals("249.99", merged.single().price)
        assertEquals("https://example.com", merged.single().shopUrl)
        assertEquals("content://photo/1", merged.single().imageUri)
        assertEquals(repair.id, merged.single().repairId)
    }

    @Test
    fun withArchivedShoppingListUpdatesExistingDocumentation() {
        val repair = repair()
        val existing = documentation(repair).copy(
            archivedShoppingList = listOf(shoppingItem(id = "old", quantity = 1))
        )

        val updated = listOf(existing).withArchivedShoppingList(
            repair = repair,
            archivedShoppingList = listOf(shoppingItem(id = "new", quantity = 2))
        )

        assertEquals(1, updated.size)
        assertEquals(3, updated.single().archivedShoppingList.single().quantity)
    }

    @Test
    fun withArchivedShoppingListCreatesDocumentationWhenMissing() {
        val repair = repair()

        val created = emptyList<RepairDocumentation>().withArchivedShoppingList(
            repair = repair,
            archivedShoppingList = listOf(shoppingItem())
        )

        assertEquals(1, created.size)
        assertEquals("Dokumentacja: ${repair.title}", created.single().title)
        assertEquals(repair.id, created.single().repairId)
    }

    @Test
    fun inventoryPartConvertsToArchivedShoppingItem() {
        val repair = repair()
        val part = PartInventoryItem(
            id = "inventory_1",
            oemPartNumber = "11428575211",
            manufacturerPartNumber = "MANN-HU816X",
            name = "Filtr oleju",
            manufacturer = "Mann",
            repairTitle = repair.title,
            quantity = 1,
            purchasePrice = "42.50",
            realOemUrl = "https://czescidobmw.pl/oem/11428575211",
            photoUri = "content://oil/filter",
            repairId = repair.id
        )

        val archived = part.toArchivedShoppingListItem(repair)

        assertEquals("11428575211", archived.partNumber)
        assertEquals("Magazyn", archived.source)
        assertEquals("42.50", archived.price)
        assertEquals(repair.id, archived.repairId)
    }

    @Test
    fun importedArchiveRenameUpdatesRepairDocumentationAndShoppingTitles() {
        val repair = repair(title = "Stara nazwa")
        val archive = ImportedRepairArchive(
            repair = repair,
            documentation = documentation(repair).copy(
                title = "Dokumentacja: Stara nazwa",
                summary = "Dokumentacja powiazana z naprawa: Stara nazwa."
            ),
            shoppingList = listOf(shoppingItem(repairTitle = repair.title))
        )

        val renamed = archive.withRepairTitle("Nowa nazwa")

        assertEquals("Nowa nazwa", renamed.repair.title)
        assertEquals("Dokumentacja: Nowa nazwa", renamed.documentation.title)
        assertTrue(renamed.documentation.summary.contains("Nowa nazwa"))
        assertEquals("Nowa nazwa", renamed.shoppingList.single().repairTitle)
    }

    @Test
    fun archiveHelpersPreferExplicitPartKeysAndFilterPlaceholderValues() {
        val item = shoppingItem(
            id = "",
            partNumber = "3332 676 3092",
            manufacturerPartNumber = "do uzupelnienia"
        )

        assertEquals("part_3332_676_3092", item.archiveMergeKey(0))
        assertEquals("lem_123", "LEM-123".normalizedArchivePartKey())
        assertFalse("do_uzupelnienia".isUsableArchivePartKey())
        assertTrue("lem_123".isUsableArchivePartKey())
    }

    private fun repair(
        title: String = "Tylna zwrotnica lewa",
        id: String = "repair_rear_knuckle"
    ) = RepairProject(
        title = title,
        area = VehicleArea.Suspension,
        vehicleName = "BMW E61 520d",
        status = "W trakcie",
        priority = "Wysoki",
        problemDescription = "Zapieczona sruba",
        goal = "Naprawa zawieszenia",
        checklist = emptyList(),
        partsToIdentify = emptyList(),
        documentsToCollect = emptyList(),
        id = id
    )

    private fun documentation(repair: RepairProject) = RepairDocumentation(
        title = "Dokumentacja: ${repair.title}",
        area = repair.area,
        repairTitle = repair.title,
        summary = "Dokumentacja powiazana z naprawa: ${repair.title}.",
        repairId = repair.id
    )

    private fun shoppingItem(
        id: String = "shopping_1",
        repairTitle: String = "Tylna zwrotnica lewa",
        partNumber: String = "33326763092",
        manufacturerPartNumber: String = "",
        quantity: Int = 1,
        source: String = "Autodoc",
        price: String = "",
        imageUri: String? = null,
        shopUrl: String? = null
    ) = ShoppingListItem(
        id = id,
        partNumber = partNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        name = "Tuleja wahacza",
        manufacturer = "Lemforder",
        repairTitle = repairTitle,
        repairId = "repair_rear_knuckle",
        area = VehicleArea.Suspension,
        quantity = quantity,
        source = source,
        price = price,
        imageUri = imageUri,
        shopUrl = shopUrl
    )
}
