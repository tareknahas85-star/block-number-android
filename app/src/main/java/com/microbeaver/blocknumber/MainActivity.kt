package com.microbeaver.blocknumber

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            updateStatus()
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

        ensureContactsPermission()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshList()
    }

    private fun ensureContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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
        val hasContacts = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        roleButton.visibility = if (hasRole) View.GONE else View.VISIBLE
        statusText.text = when {
            !hasRole -> getString(R.string.status_no_role)
            !hasContacts -> getString(R.string.status_no_contacts)
            prefs.blockingEnabled -> getString(R.string.status_active)
            else -> getString(R.string.status_paused)
        }
    }

    private fun refreshList() {
        adapter.submit(store.getAll())
    }

    private inner class BlockedCallAdapter :
        RecyclerView.Adapter<BlockedCallAdapter.Holder>() {

        private var items: List<BlockedCall> = emptyList()

        fun submit(newItems: List<BlockedCall>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val number: TextView = view.findViewById(R.id.itemNumber)
            val time: TextView = view.findViewById(R.id.itemTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocked, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.number.text = item.number
            holder.time.text = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT
            ).format(Date(item.timestamp))
        }

        override fun getItemCount() = items.size
    }
}
