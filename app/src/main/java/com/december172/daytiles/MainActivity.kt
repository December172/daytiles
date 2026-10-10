package com.december172.daytiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.december172.daytiles.ui.AppNavigation
import com.december172.daytiles.ui.theme.DayTilesTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openDate = intent.getStringExtra(EXTRA_OPEN_DATE)
        val openJournal = intent.getBooleanExtra(EXTRA_OPEN_JOURNAL, false)
        setContent {
            DayTilesTheme {
                AppNavigation(initialDate = openDate?.let(LocalDate::parse), openJournal = openJournal)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_DATE = "com.december172.daytiles.OPEN_DATE"
        const val EXTRA_OPEN_JOURNAL = "com.december172.daytiles.OPEN_JOURNAL"
    }
}
