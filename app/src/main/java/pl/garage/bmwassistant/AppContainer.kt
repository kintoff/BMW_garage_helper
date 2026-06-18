package pl.garage.bmwassistant

import android.content.Context
import pl.garage.bmwassistant.database.migration.LegacyStorageRoomMigrator
import pl.garage.bmwassistant.database.repository.GarageRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val garageRepository: GarageRepository by lazy {
        GarageRepository(appContext)
    }

    val legacyStorageRoomMigrator: LegacyStorageRoomMigrator by lazy {
        LegacyStorageRoomMigrator(appContext)
    }
}
