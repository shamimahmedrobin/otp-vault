package com.otpvault.app.presentation.screens.search

import androidx.compose.runtime.Composable
import com.otpvault.app.presentation.screens.history.HistoryScreen

/** Search is the History layout focused on the query field: same data, same filters. */
@Composable
fun SearchScreen(onOpenDetails: (String) -> Unit) {
    HistoryScreen(onOpenDetails = onOpenDetails)
}
