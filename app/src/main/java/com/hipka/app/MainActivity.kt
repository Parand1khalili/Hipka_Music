package com.hipka.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.presentation.main.MainViewModel
import com.hipka.app.presentation.navigation.HipkaNavGraph
import com.hipka.app.presentation.theme.HipkaTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.hipka.app.data.remote.api.SongApi
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var songApi: SongApi //temporary for test supabase

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //supabase test // parand
        lifecycleScope.launch {
            try {
                val songs = songApi.testGetSongs()
                android.util.Log.d("SUPABASE_TEST", "اتصال موفقیت‌آمیز بود! تعداد آهنگ‌ها: ${songs.size}")
            } catch (e: Exception) {
                // ریزِ خطاهای جاوا و شبکه چاپ شود
                android.util.Log.e("SUPABASE_TEST", "خطا در اتصال", e)
            }
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            HipkaTheme(darkTheme = darkTheme) {
                HipkaNavGraph(
                    mainUiState = uiState,
                    onMainIntent = viewModel::onIntent
                )
            }
        }
    }
}