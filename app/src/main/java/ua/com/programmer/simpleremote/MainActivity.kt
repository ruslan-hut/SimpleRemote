package ua.com.programmer.simpleremote

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.dao.entity.getGuid
import ua.com.programmer.simpleremote.databinding.ActivityMainBinding
import ua.com.programmer.simpleremote.firebase.DeleteOldUsersWorker
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import java.util.concurrent.TimeUnit

private lateinit var drawerLayout: DrawerLayout

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel : SharedViewModel by viewModels()
    private lateinit var binding : ActivityMainBinding

    private var backPressedTime: Long = 0
    private var barcode = StringBuilder()
    private var lastKeystrokeTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        drawerLayout = binding.drawerLayout

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.selectDocumentTypeFragment,
                R.id.selectCatalogTypeFragment
                ), drawerLayout
        )

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        val navController = navHostFragment.navController

        setupActionBarWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.navView, navController)

        onBackPressedDispatcher.addCallback(this) {
            val navController = findNavController(R.id.nav_host_container)

            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, R.string.hint_press_back, Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        }


        viewModel.connection.observe(this) {
            it?.let {
                val textUserID = "${BuildConfig.VERSION_NAME} (${it.getGuid()})"
                val header = binding.navView.getHeaderView(0)
                val textVersion = header.findViewById<TextView>(R.id.nav_header_text1)
                textVersion.text = textUserID
                val textAccount = header.findViewById<TextView>(R.id.nav_header_text2)
                textAccount.text = it.description
            }
        }

        if (BuildConfig.DEBUG) {
            scheduleUserCleanupWorker()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_container)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val device = event.device
        if (!isScannerDevice(device)) {
            return super.dispatchKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (barcode.isNotEmpty() && currentTime - lastKeystrokeTime > 60) {
                barcode.clear()
            }

            val char = event.unicodeChar.toChar()
            if (char.isLetterOrDigit()) {
                barcode.append(char)
            }
            lastKeystrokeTime = currentTime

        } else if (event.action == KeyEvent.ACTION_UP) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
                if (barcode.isNotEmpty()) {
                    viewModel.onBarcodeRead(barcode.toString())
                    barcode.clear()
                }
                return true
            }
        }

        return true
    }


    private fun isScannerDevice(device: InputDevice?): Boolean {
        if (device == null || device.id == -1 || device.isVirtual) return false

        val name = device.name.lowercase()
        if (name == "virtual") return false

        val isKnownScannerName = name.contains("scanner") || name.contains("honeywell") || name.contains("zebra")
        if (isKnownScannerName) return true

        // Это условие стоит оставить только если вы уверены,
        // что есть сканеры, которые не попадают под проверку имени,
        // но при этом не мешают работе с обычными клавиатурами.
        val isGenericKeyboard = (device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD

        // Если это клавиатура общего типа, можно добавить дополнительную логику,
        // например, разрешать ввод только если фокус не на EditText.
        // Но для простоты, лучше полагаться на имена.

        return isGenericKeyboard // Оставьте, если это необходимо для ваших устройств
    }


    fun hideSoftKeyboard() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.ime())

        window.decorView.clearFocus()

        binding.drawerLayout.requestFocus()
    }

    private fun scheduleUserCleanupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DeleteOldUsersWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // only run when the device is connected to the internet
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DeleteOldUsersWorker",
            ExistingPeriodicWorkPolicy.KEEP, // don't replace any existing work with the same name
            workRequest
        )
    }

}
