package com.neko.weather

import android.os.Bundle
import android.os.Build
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.MainViewModelFactory
import com.neko.weather.ui.NekoWeatherApp
import com.neko.weather.notification.WeatherNotificationScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestRefreshRate()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))
            NekoWeatherApp(viewModel)
        }
        // 通知任务注册失败不能阻止主界面启动。
        WeatherNotificationScheduler.sync(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        preferHighestRefreshRate()
    }

    private fun preferHighestRefreshRate() {
        val targetRate = currentDisplay()?.supportedModes?.maxOfOrNull { it.refreshRate } ?: return

        window.attributes = window.attributes.apply {
            preferredRefreshRate = targetRate
        }
    }

    /** Context.getDisplay 需要 API 30；更早版本回退到 windowManager.defaultDisplay。 */
    private fun currentDisplay(): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
        else @Suppress("DEPRECATION") windowManager.defaultDisplay
}
