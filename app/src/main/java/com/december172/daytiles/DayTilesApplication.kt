package com.december172.daytiles

import android.app.Application
import com.december172.daytiles.data.PlannerSettings
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.DayTilesDatabase

class DayTilesApplication : Application() {
    val settings: PlannerSettings by lazy { PlannerSettings(this) }
    val repository: DayRepository by lazy {
        val database = DayTilesDatabase.getInstance(this)
        DayRepository(database, applicationContext)
    }
}
