package ua.com.programmer.simpleremote

import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
            if (navController.previousBackStackEntry == null) {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                }else {
                    Toast.makeText(this@MainActivity, R.string.hint_press_back, Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            } else {
                navController.popBackStack()
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

        scheduleUserCleanupWorker()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_container)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        event.let {
            if (it.action == KeyEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()

                if (barcode.isNotEmpty() && currentTime - lastKeystrokeTime > 60) {
                    barcode.clear()
                }
                when (it.keyCode) {
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_TAB -> {
                        viewModel.onBarcodeRead(barcode.toString())
                        barcode.clear()
                        return true
                    }
                    else -> {
                        val char = it.unicodeChar.toChar()
                        if (Character.isDigit(char) || Character.isLetter(char)) {
                            barcode.append(char)
                        }
                    }
                }

                lastKeystrokeTime = currentTime
            }
        }
        return super.dispatchKeyEvent(event)
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
