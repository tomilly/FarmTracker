package pl.farmtracker.data.firebase

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aktywność na wierzchu – Firebase potrzebuje jej przy weryfikacji numeru telefonu (sprawdzenie,
 * że to prawdziwa aplikacja, a w razie potrzeby reCAPTCHA). Trzymana słabo, żeby jej nie „zatrzymać".
 */
@Singleton
class CurrentActivity @Inject constructor(@ApplicationContext context: Context) {

    private var current = WeakReference<Activity>(null)

    val activity: Activity? get() = current.get()

    init {
        (context as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    current = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (current.get() === activity) current = WeakReference(null)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    current = WeakReference(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
