package ua.com.programmer.simpleremote.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentScannerSettingsBinding
import ua.com.programmer.simpleremote.settings.ScannerSettings

class ScannerSettingsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentScannerSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var scannerSettings: ScannerSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerSettingsBinding.inflate(inflater)
        scannerSettings = ScannerSettings(requireContext())

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        loadSettings()

        binding.testScanner.setOnClickListener {
            findNavController().navigate(R.id.action_scannerSettingsFragment_to_scannerTestFragment)
        }

        return binding.root
    }

    private fun loadSettings() {
        binding.apply {
            keystrokeTimeout.setText(scannerSettings.keystrokeTimeout.toString())
            minBarcodeLength.setText(scannerSettings.minBarcodeLength.toString())
            prefixToStrip.setText(scannerSettings.prefixToStrip)
            suffixToStrip.setText(scannerSettings.suffixToStrip)

            when (scannerSettings.terminatorKey) {
                ScannerSettings.TERMINATOR_ENTER -> terminatorEnter.isChecked = true
                ScannerSettings.TERMINATOR_TAB -> terminatorTab.isChecked = true
                else -> terminatorBoth.isChecked = true
            }
        }
    }

    private fun saveSettings() {
        val timeoutText = binding.keystrokeTimeout.text.toString()
        val timeout = timeoutText.toLongOrNull() ?: ScannerSettings.DEFAULT_TIMEOUT
        scannerSettings.keystrokeTimeout = timeout

        val minLengthText = binding.minBarcodeLength.text.toString()
        val minLength = minLengthText.toIntOrNull() ?: ScannerSettings.DEFAULT_MIN_LENGTH
        scannerSettings.minBarcodeLength = minLength

        scannerSettings.terminatorKey = when {
            binding.terminatorEnter.isChecked -> ScannerSettings.TERMINATOR_ENTER
            binding.terminatorTab.isChecked -> ScannerSettings.TERMINATOR_TAB
            else -> ScannerSettings.TERMINATOR_BOTH
        }

        scannerSettings.prefixToStrip = binding.prefixToStrip.text.toString()
        scannerSettings.suffixToStrip = binding.suffixToStrip.text.toString()

        Toast.makeText(activity, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_scanner_settings, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.save -> saveSettings()
            else -> return false
        }
        return true
    }
}
