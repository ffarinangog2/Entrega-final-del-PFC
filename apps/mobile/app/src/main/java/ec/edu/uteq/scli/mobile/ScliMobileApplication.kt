package ec.edu.uteq.scli.mobile

import android.app.Application
import ec.edu.uteq.scli.mobile.common.di.AppContainer
import ec.edu.uteq.scli.mobile.common.logging.JsonTree
import timber.log.Timber

class ScliMobileApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        Timber.plant(JsonTree())
        Timber.tag("ScliMobileApplication").i("App iniciada")

        container = AppContainer(this)
    }
}
