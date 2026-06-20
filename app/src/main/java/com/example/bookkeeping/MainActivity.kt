package com.example.bookkeeping

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bookkeeping.ui.LedgerApp
import com.example.bookkeeping.ui.LedgerTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LedgerTheme {
                val app = application as BookkeepingApplication
                LedgerApp(BookkeepingRepository(this, app.database))
            }
        }
    }
}
