package com.julio.radio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.julio.radio.data.prefs.SettingsRepository
import com.julio.radio.ui.countries.CountriesScreen
import com.julio.radio.ui.stations.StationsScreen
import com.julio.radio.ui.stations.StationsViewModel
import com.julio.radio.ui.theme.RadioWorldTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeAppLanguage()
        setContent {
            RadioWorldTheme {
                NotificationPermissionEffect()
                AppRoot()
            }
        }
    }

    /**
     * Aplica el idioma efectivo (elección manual o el del país). Mientras no haya país ni
     * elección manual no toca nada, así la app arranca en el idioma del sistema.
     */
    private fun observeAppLanguage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings
                    .filter { it.countryCode != null || it.languageOverride != null }
                    .map { it.effectiveLanguage.tag }
                    .distinctUntilChanged()
                    .collect { tag -> applyLanguage(tag) }
            }
        }
    }

    private fun applyLanguage(tag: String) {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current.substringBefore('-') != tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}

@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun AppRoot() {
    val stationsVm: StationsViewModel = hiltViewModel()
    val state by stationsVm.state.collectAsStateWithLifecycle()

    if (state.countryCode == null) CountriesScreen() else StationsScreen()
}