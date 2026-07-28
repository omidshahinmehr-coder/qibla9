package com.qibla.prayertimes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qibla.prayertimes.viewmodel.QiblaViewModel

private object Routes {
    const val HOME = "home"
    const val MENU = "menu"
    const val ALARMS = "alarms"
    const val ABOUT = "about"
    const val MONTHLY = "monthly"
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val CITY_PICKER = "city_picker"
    const val MAP_PICKER = "map_picker"
}

@Composable
fun QiblaNavHost(navController: NavHostController = rememberNavController()) {
    // Shared across the whole nav graph (the QiblaViewModel is Activity-scoped, see QiblaScreen),
    // so the city list stays consistent between the home screen and the city picker.
    val viewModel: QiblaViewModel = viewModel(
        viewModelStoreOwner = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    )

    // Simple hand-off for the map picker's result — plain Compose state at the NavHost's own
    // scope survives navigating to/from the map screen since the NavHost itself isn't recreated.
    var pendingMapResult by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            QiblaScreen(
                viewModel = viewModel,
                onOpenMenu = { navController.navigate(Routes.MENU) },
                onOpenCityPicker = { navController.navigate(Routes.CITY_PICKER) }
            )
        }
        composable(Routes.MENU) {
            MenuScreen(
                onBack = { navController.popBackStack() },
                onOpenAlarms = { navController.navigate(Routes.ALARMS) },
                onOpenMonthly = { navController.navigate(Routes.MONTHLY) },
                onOpenCityPicker = { navController.navigate(Routes.CITY_PICKER) },
                onOpenLanguage = { navController.navigate(Routes.LANGUAGE) },
                onOpenTheme = { navController.navigate(Routes.THEME) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ALARMS) {
            AlarmSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MONTHLY) {
            MonthlyTimesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LANGUAGE) {
            LanguageScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.THEME) {
            ThemeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CITY_PICKER) {
            val selected by viewModel.selectedCity.collectAsState()
            val customCities by viewModel.customCities.collectAsState()
            CityPickerScreen(
                selected = selected,
                customCities = customCities,
                onSelect = { viewModel.selectCity(it) },
                onAddCity = { viewModel.addCustomCity(it) },
                onRemoveCustom = { viewModel.removeCustomCity(it) },
                onBack = { navController.popBackStack() },
                onOpenMap = { navController.navigate(Routes.MAP_PICKER) },
                pendingMapResult = pendingMapResult,
                onConsumeMapResult = { pendingMapResult = null }
            )
        }
        composable(Routes.MAP_PICKER) {
            MapPickerScreen(
                onBack = { navController.popBackStack() },
                onPicked = { lat, lon ->
                    pendingMapResult = lat to lon
                    navController.popBackStack()
                }
            )
        }
    }
}
