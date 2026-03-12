package ua.com.programmer.simpleremote

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.getGuid
import ua.com.programmer.simpleremote.databinding.ActivityMainBinding
import ua.com.programmer.simpleremote.firebase.DeleteOldUsersWorker
import ua.com.programmer.simpleremote.settings.ScannerDiagnostics
import ua.com.programmer.simpleremote.settings.ScannerSettings
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import java.util.concurrent.TimeUnit

private lateinit var drawerLayout: DrawerLayout

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel : SharedViewModel by viewModels()
    private lateinit var binding : ActivityMainBinding

    private lateinit var scannerSettings: ScannerSettings
    lateinit var diagnostics: ScannerDiagnostics
        private set
    private var keyEventListener: ((KeyEvent) -> Boolean)? = null

    private var backPressedTime: Long = 0
    private var barcode = StringBuilder()
    private var lastKeystrokeTime = 0L
    private var barcodeConsumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        scannerSettings = ScannerSettings(this)
        diagnostics = ScannerDiagnostics(this)

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
//        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.navView, navController)

        onBackPressedDispatcher.addCallback(this) {
            val navController = findNavController(R.id.nav_host_container)

            if (navController.previousBackStackEntry != null) {
                if (viewModel.getDocument().modified){
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(R.string.exit_without_saving)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            navController.popBackStack()
                        }
                        .setNegativeButton(android.R.string.cancel){ _, _ -> }
                        .show()
                }else{
                    navController.popBackStack()
                }
            } else {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, R.string.hint_press_back, Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connection.collect {
                    it?.let {
                        val textUserID = "${BuildConfig.VERSION_NAME} (${it.getGuid()})"
                        val header = binding.navView.getHeaderView(0)
                        val textVersion = header.findViewById<TextView>(R.id.nav_header_text1)
                        textVersion.text = textUserID
                        val textAccount = header.findViewById<TextView>(R.id.nav_header_text2)
                        textAccount.text = it.description
                        diagnostics.deviceId = it.guid
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.networkError.collect { message ->
                    Snackbar.make(binding.navHostContainer, message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        if (BuildConfig.DEBUG) {
            scheduleUserCleanupWorker()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_container)

        // Check if the NavController can navigate up
        if (navController.previousBackStackEntry != null) {
            // If it can, we are on a "deep" screen.
            // Trigger our custom back press logic (which includes the dialog).
            onBackPressedDispatcher.onBackPressed()
            return true
        } else {
            // Otherwise, we are on a top-level screen.
            // Let NavigationUI handle it, which will open the drawer.
            val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
            return NavigationUI.navigateUp(navController, appBarConfiguration)
        }
    }

    /**
     * Intercepts key events to capture input from a barcode scanner.
     *
     * Barcode scanners connected as external keyboards send a rapid burst of
     * keystrokes followed by an ENTER or TAB terminator. This method buffers
     * those keystrokes using a timeout (200 ms) to separate consecutive scans.
     *
     * When a terminator arrives and the buffer contains at least
     * [MIN_BARCODE_LENGTH] characters, the assembled string is forwarded to
     * [SharedViewModel.onBarcodeRead] and both ACTION_DOWN and ACTION_UP for
     * the terminator are consumed so they never reach the focused view.
     */
    fun setKeyEventListener(listener: ((KeyEvent) -> Boolean)?) {
        keyEventListener = listener
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // When a test listener is active, forward all events to it instead
        keyEventListener?.let { listener ->
            if (listener(event)) return true
        }

        diagnostics.recordKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN) {
            val isTerminator = scannerSettings.isTerminator(event.keyCode)

            if (isTerminator) {
                if (barcode.length >= scannerSettings.minBarcodeLength) {
                    val raw = barcode.toString()
                    val cleaned = scannerSettings.cleanBarcode(raw)
                    Log.d("RC_MainActivity", "barcode: $cleaned")
                    diagnostics.recordDetection(raw, cleaned, success = true)
                    viewModel.onBarcodeRead(cleaned)
                    barcode.clear()
                    barcodeConsumed = true
                    return true
                }
                // Buffer had some chars but too few — still consume the
                // terminator to prevent TAB from switching ViewPager2 tabs.
                if (barcode.isNotEmpty()) {
                    Log.d("RC_MainActivity", "barcode too short: $barcode (${barcode.length} < ${scannerSettings.minBarcodeLength})")
                    diagnostics.recordDetection(barcode.toString(), "", success = false)
                    barcode.clear()
                    barcodeConsumed = false
                    return true
                }
                // Buffer empty — normal keyboard TAB/ENTER, let it through
                diagnostics.recordNote("terminator pass-through (empty buffer)")
                barcode.clear()
                barcodeConsumed = false
                return super.dispatchKeyEvent(event)
            }

            val currentTime = System.currentTimeMillis()
            if (barcode.isNotEmpty() && currentTime - lastKeystrokeTime > scannerSettings.keystrokeTimeout) {
                diagnostics.recordBuffer("timeout_clear", barcode.toString(), barcode.length)
                barcode.clear()
            }

            val char = event.unicodeChar.toChar()
            if (char.code in 0x20..0x7E) {
                barcode.append(char)
                lastKeystrokeTime = currentTime
                diagnostics.recordBuffer("append", barcode.toString(), barcode.length)
            }
        }

        if (event.action == KeyEvent.ACTION_UP) {
            if (scannerSettings.isTerminator(event.keyCode)) {
                if (barcodeConsumed) {
                    barcodeConsumed = false
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    /**
     * Hides the soft (on-screen) keyboard if it is currently visible.
     *
     * This function uses the [WindowInsetsControllerCompat] to explicitly hide the keyboard.
     * It also clears the focus from any currently focused view (like an EditText)
     * and then requests focus for the root `DrawerLayout`. This ensures that no view
     * retains focus that might trigger the keyboard to reappear.
     */
    fun hideSoftKeyboard() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.ime())

        window.decorView.clearFocus()

        binding.drawerLayout.requestFocus()
    }

    override fun onDestroy() {
        diagnostics.stop()
        diagnostics.clear()
        super.onDestroy()
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
