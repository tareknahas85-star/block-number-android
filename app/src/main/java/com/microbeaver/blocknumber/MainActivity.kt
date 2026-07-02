package com.microbeaver.blocknumber

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var store: BlockedCallStore
    private lateinit var statusText: TextView
    private lateinit var roleButton: MaterialButton
    private lateinit var adapter: BlockedCallAdapter

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateStatus()
        }

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updateStatus()
            populateSimSwitches()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        store = BlockedCallStore(this)

        statusText = findViewById(R.id.statusText)
        roleButton = findViewById(R.id.roleButton)

        val enableSwitch = findViewById<MaterialSwitch>(R.id.enableSwitch)
        enableSwitch.isChecked = prefs.blockingEnabled
        enableSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.blockingEnabled = checked
            updateStatus()
        }

        val hiddenSwitch = findViewById<MaterialSwitch>(R.id.hiddenSwitch)
        hiddenSwitch.isChecked = prefs.blockHidden
        hiddenSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.blockHidden = checked
        }

        roleButton.setOnClickListener { requestScreeningRole() }

        findViewById<MaterialButton>(R.id.clearButton).setOnClickListener {
            store.clear()
            refreshList()
        }

        adapter = BlockedCallAdapter()
        findViewById<RecyclerView>(R.id.blockedList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshList()
        populateSimSwitches()
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermissions() {
        val needed = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        ).filterNot { hasPermission(it) }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    /** One switch per call-capable SIM so blocking can be enabled per SIM. */
    private fun populateSimSwitches() {
        val container = findViewById<LinearLayout>(R.id.simContainer)
        val title = findViewById<TextView>(R.id.simTitle)
        container.removeAllViews()
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            title.visibility = View.GONE
            return
        }
        val tm = getSystemService(TelecomManager::class.java)
        val accounts = try {
            tm.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList()
        }
        title.visibility = if (accounts.size >= 2) View.VISIBLE else View.GONE
        if (accounts.size < 2) return
        accounts.forEachIndexed { index, handle ->
            val label = try {
                tm.getPhoneAccount(handle)?.label?.toString()
            } catch (e: SecurityException) {
                null
            }?.takeIf { it.isNotBlank() } ?: getString(R.string.sim_n, index + 1)

            val sw = MaterialSwitch(this).apply {
                text = getString(R.string.sim_block_on, label)
                isChecked = prefs.simBlockEnabled(handle.id)
                setOnCheckedChangeListener { _, checked ->
                    prefs.setSimBlockEnabled(handle.id, checked)
                }
            }
            container.addView(sw)
        }
    }

    private fun hasScreeningRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun updateStatus() {
        val hasRole = hasScreeningRole()
        val hasContac