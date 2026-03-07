package ua.com.programmer.simpleremote

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
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
import androidx.navigation.fragment.findNavController
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                    }
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
     * This method checks if the incoming [KeyEvent] is from a recognized scanner device.
     * If it is, it captures the keystrokes to assemble a barcode string.
     *
     * The logic is as follows:
     * 1. Identify if the event source is a scanner using [isScannerDevice]. If not, defer to the default system handling.
     * 2. On `ACTION_DOWN`, append the character to an internal `barcode` buffer. It also implements a timeout (60ms) to clear the buffer between separate scans, ensuring that partial scans or delayed inputs don't corrupt the next valid scan.
     * 3. On `ACTION_UP` for an `ENTER` or `TAB` key, it considers the barcode scan complete. The assembled barcode string is then passed to the [SharedViewModel.onBarcodeRead] for processing, and the buffer is cleared.
     * 4. For all events handled by the scanner logic, it returns `true` to indicate that the event has been consumed and should not be propagated further.
     *
     * @param event The [KeyEvent] to be dispatched.
     * @return `true` if the event was handled by the scanner logic, otherwise the result of `super.dispatchKeyEvent(event)`.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

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
                    Log.d("RC_MainActivity", "barcode: $barcode")
                    viewModel.onBarcodeRead(barcode.toString())
                    barcode.clear()
                }
                return true
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
