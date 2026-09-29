package pl.farmtracker.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import pl.farmtracker.data.firebase.CurrentActivity
import javax.inject.Inject

@HiltAndroidApp
class FarmTrackerApplication : Application() {

    /**
     * Tworzony razem z aplikacją, żeby „zobaczył" już pierwszy ekran – utworzony później (przy logowaniu)
     * przegapiłby otwarcie aktywności i Firebase nie miałby jej do weryfikacji numeru.
     */
    @Inject
    lateinit var currentActivity: CurrentActivity
}
