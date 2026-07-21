package com.roadtrippin.shared.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.roadtrippin.shared.data.AppScreen
import com.roadtrippin.shared.data.RoadtrippinStore

@Composable
fun RoadtrippinApp(store: RoadtrippinStore = remember { RoadtrippinStore() }) {
    val snackbarHostState = remember { SnackbarHostState() }
    RoadtrippinTheme(store.settings) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (!store.isLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!store.settings.safetyAccepted) {
                SafetyScreen(store::acceptSafety, padding)
            } else {
                when (store.screen) {
                    AppScreen.HOME -> HomeScreen(store, snackbarHostState, padding)
                    AppScreen.DASHBOARD -> DashboardScreen(store, snackbarHostState, padding)
                    AppScreen.PLATES -> PlatesScreen(store, snackbarHostState, padding)
                    AppScreen.JOURNAL -> JournalScreen(store, snackbarHostState, padding)
                    AppScreen.MAP -> MapScreen(store, snackbarHostState, padding)
                    AppScreen.AWARDS -> AwardsScreen(store, padding)
                    AppScreen.TRIP_INFO -> TripInfoScreen(store, padding)
                    AppScreen.SETTINGS -> SettingsScreen(store, padding)
                    AppScreen.ACCOUNT -> AccountScreen(store, snackbarHostState, padding)
                }
            }
        }
    }
}
