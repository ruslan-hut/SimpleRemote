package ua.com.programmer.simpleremote.ui.connection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.FragmentConnectionSettingsBinding
import kotlin.getValue
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@AndroidEntryPoint
class ConnectionFragment: Fragment(), MenuProvider {

    private val viewModel: ConnectionViewModel by viewModels()
    private var _binding: FragmentConnectionSettingsBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: ConnectionFragmentArgs by navArgs()

    private var _connection: ConnectionSettings = ConnectionSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentConnection(navigationArgs.guid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConnectionSettingsBinding.inflate(inflater)

        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.connection.observe(viewLifecycleOwner) {
            it?.let {
                binding.apply {
                    description.setText(it.description)
                    server.setText(it.serverAddress)
                    dbName.setText(it.databaseName)
                    dbUser.setText(it.user)
                    dbPassword.setText(it.password)
                    userId.text = it.guid
                }
                _connection = it
            }
        }

        binding.userId.setOnLongClickListener{
            copyToClipboard(binding.userId.text.toString())
            true
        }

        return binding.root
    }

    private fun copyToClipboard(text: String) {
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.pref_user_id), text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(activity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun deleteConnection() {
        val userID = binding.userId.text.toString()
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage(getString(R.string.text_erase_data))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                viewModel.deleteConnection(userID) {
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun saveConnection() {
        val connection = ConnectionSettings(
            guid = binding.userId.text.toString(),
            description = binding.description.text.toString().trim(),
            serverAddress = binding.server.text.toString().trim(),
            databaseName = binding.dbName.text.toString().trim(),
            user = binding.dbUser.text.toString().trim(),
            password = binding.dbPassword.text.toString().trim(),
            isCurrent = _connection.isCurrent,
            userOptions = _connection.userOptions,
            autoConnect = _connection.autoConnect
        )
        viewModel.saveConnection(connection, ::finish)
    }

    private fun finish() {
        findNavController().popBackStack()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_connection_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.delete -> {
                deleteConnection()
            }
            R.id.save -> {
                saveConnection()
            }
            else -> return false
        }
        return true
    }
}