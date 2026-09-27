package com.shutupandlog

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shutupandlog.data.SqlLogRepository
import com.shutupandlog.db.LogDatabase
import com.shutupandlog.presentation.App

class LogApplication : Application() {
    val repository by lazy {
        SqlLogRepository(LogDatabase(AndroidSqliteDriver(LogDatabase.Schema, this, "shut-up-and-log.db")))
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App((application as LogApplication).repository) }
    }
}
