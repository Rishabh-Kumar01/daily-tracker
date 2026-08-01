package dev.rishabh.dailytracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.rishabh.dailytracker.core.common.di.ApplicationScope
import dev.rishabh.dailytracker.core.db.seed.GenericFoodSeeder
import dev.rishabh.dailytracker.core.db.seed.TemplateSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DailyTrackerApplication : Application() {

    @Inject lateinit var templateSeeder: TemplateSeeder
    @Inject lateinit var genericFoodSeeder: GenericFoodSeeder

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Install the built-in activities and generic foods on first run. Both are
        // idempotent, so they're safe every launch; Room's Flows push seeded rows to the
        // UI whenever they land.
        appScope.launch { templateSeeder.seedIfNeeded() }
        appScope.launch { genericFoodSeeder.seedIfNeeded() }
    }
}
