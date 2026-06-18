package pl.garage.bmwassistant

import android.app.Application
import android.content.Context

class GarageApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as? GarageApplication)?.appContainer
        ?: AppContainer(applicationContext)
