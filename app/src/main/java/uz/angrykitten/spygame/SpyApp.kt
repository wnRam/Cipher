package uz.angrykitten.spygame

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

@HiltAndroidApp
class SpyApp : Application() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var soundManager: SoundManager

    override fun onCreate() {
        super.onCreate()
        // Apply the user's saved language before the first Activity inflates
        // its resources, so every string read goes through the chosen locale.
        val lang = preferencesRepository.currentLanguageBlocking().ifBlank { "en" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))

        // Pause / resume the SoundPool with the foreground activity. We use
        // ActivityLifecycleCallbacks rather than ProcessLifecycleOwner so we
        // don't have to pull in lifecycle-process. The app has exactly one
        // Activity (MainActivity), so onActivityStopped == app backgrounded
        // and onActivityStarted == app foregrounded for our purposes.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                soundManager.autoResume()
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                soundManager.autoPause()
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun onTerminate() {
        // Note: not guaranteed to fire on real devices, but releases the
        // SoundPool cleanly during emulator shutdowns / tests.
        soundManager.release()
        super.onTerminate()
    }
}
