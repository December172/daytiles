package com.december172.daytiles

import android.app.Application
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.DayTilesDatabase

class DayTilesApplication : Application() {
    val repository: DayRepository by lazy {
        val database = DayTilesDatabase.getInstance(this)
        DayRepository(
            database.journalDao(),
            database.taskDao(),
            database.stickerPlacementDao(),
            applicationContext,
        )
    }
}
