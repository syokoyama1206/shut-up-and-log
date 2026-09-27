package com.shutupandlog

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.shutupandlog.data.SqlLogRepository
import com.shutupandlog.db.LogDatabase
import com.shutupandlog.presentation.App

private val repository by lazy {
    SqlLogRepository(LogDatabase(NativeSqliteDriver(LogDatabase.Schema, "shut-up-and-log.db")))
}
fun MainViewController() = ComposeUIViewController { App(repository) }
