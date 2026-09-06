package com.shohankhan.bokeya

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.shohankhan.bokeya.data.DefaultData
import com.shohankhan.bokeya.data.db.BokeyaDatabase
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.SettingsStore
import com.shohankhan.bokeya.notifications.NotificationScheduler
import com.shohankhan.bokeya.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual DI container — small enough that a DI framework would be overhead. */
class AppContainer(context: Context) {
    val database: BokeyaDatabase = BokeyaDatabase.get(context)
    val repository: BokeyaRepository = BokeyaRepository(database)
    val settings: SettingsStore = SettingsStore(context)
    val scheduler: NotificationScheduler = NotificationScheduler(context)
}

class BokeyaApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        scope.launch {
            val dao = container.database.categoryDao()
            if (dao.count() == 0) dao.insertAll(DefaultData.seed())
            container.repository.runDueRecurring()
        }
        container.scheduler.scheduleDailyDigest()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.INFO else android.util.Log.ERROR)
            .build()
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BokeyaApplication).container
