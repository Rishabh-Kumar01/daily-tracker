package dev.rishabh.dailytracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.rishabh.dailytracker.core.common.di.ApplicationScope
import dev.rishabh.dailytracker.core.db.seed.TemplateSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DailyTrackerApplication : Application() {

    @Inject lateinit var seeder: TemplateSeeder

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Install the built-in activities on first run. Idempotent, so it's safe every
        // launch; Room's Flows push the seeded rows to Home whenever they land.
        appScope.launch { seeder.seedIfNeeded() }
    }
}
