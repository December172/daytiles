package com.december172.daytiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.december172.daytiles.ui.calendar.CalendarRoute
import com.december172.daytiles.ui.theme.DayTilesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayTilesTheme {
                CalendarRoute()
            }
        }
    }
}

