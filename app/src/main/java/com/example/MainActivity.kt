package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.screens.AbsenScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KasirScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MarketingScreen
import com.example.ui.screens.OmsetScreen
import com.example.ui.screens.PengaturanScreen
import com.example.ui.screens.PengeluaranScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.screens.ServiceKasirScreen
import com.example.ui.screens.SetoranScreen
import com.example.ui.screens.StokScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BengkelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.FeatureGate.getTrialStartEpoch(this)
        enableEdgeToEdge()

        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

            MyApplicationTheme(selectedTheme = selectedTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BengkelAppContent(
                        viewModel = viewModel,
                        currentScreen = currentScreen
                    )
                }
            }
        }
    }
}

@Composable
fun BengkelAppContent(
    viewModel: BengkelViewModel,
    currentScreen: BengkelScreen
) {
    // Handle Android system back button properly
    BackHandler(enabled = currentScreen != BengkelScreen.LOGIN && currentScreen != BengkelScreen.DASHBOARD) {
        when (currentScreen) {
            BengkelScreen.SERVICE_DETAIL -> viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE)
            BengkelScreen.SETORAN, BengkelScreen.PENGELUARAN -> viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE)
            else -> viewModel.navigateTo(BengkelScreen.DASHBOARD)
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            BengkelScreen.LOGIN -> LoginScreen(viewModel = viewModel)
            BengkelScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
            BengkelScreen.SERVICE_QUEUE -> ServiceKasirScreen(viewModel = viewModel)
            BengkelScreen.KASIR -> KasirScreen(viewModel = viewModel)
            BengkelScreen.SERVICE_DETAIL -> ServiceDetailScreen(viewModel = viewModel)
            BengkelScreen.SETORAN -> SetoranScreen(viewModel = viewModel)
            BengkelScreen.PENGELUARAN -> PengeluaranScreen(viewModel = viewModel)
            BengkelScreen.STOK -> StokScreen(viewModel = viewModel)
            BengkelScreen.ABSEN -> AbsenScreen(viewModel = viewModel)
            BengkelScreen.OMSET -> OmsetScreen(viewModel = viewModel)
            BengkelScreen.REPORT -> ReportScreen(viewModel = viewModel)
            BengkelScreen.MARKETING -> MarketingScreen(viewModel = viewModel)
            BengkelScreen.PENGATURAN -> PengaturanScreen(viewModel = viewModel)
        }
    }
}
