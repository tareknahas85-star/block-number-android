package com.microbeaver.blocknumber

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import android.content.res.ColorStateList
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var store: BlockedCallStore
    private lateinit var blacklist: BlacklistStore
    private lateinit var spamDb: SpamDbStore

    private lateinit var statusCard: MaterialCardView
    private lateinit var statusIcon: ImageView
    private lateinit var statusText: TextView
    private lateinit var roleButton: MaterialButton
    private lateinit var dbStatusText: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var logAdapter: BlockedCallAdapter
    private lateinit var blacklistAdapter: BlacklistAdapter
    private var clearLogMenuItem: MenuItem? = null

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
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        store = BlockedCallStore(this)
        blacklist = BlacklistStore(this)
        spamDb = SpamDbStore(this)

        statusCard = findViewById(R.id.statusCard)
        statusIcon = findViewById(R.id.statusIcon)
        statusText = findViewById(R.id.statusText)
        roleButton = findViewById(R.id.roleButton)
        dbStatusText = findViewById(R.id.dbStatusText)
        fab = findViewById(R.id.addBlacklistFab)
        bottomNav = findViewById(R.id.bottomNav)

        setupToolbar()
        setupSwitches()
        setupLists()
        setupNavigation()

        roleButton.setOnClickListener { requestScreeningRole() }
        findViewById<MaterialButton>(R.id.updateDbButton).setOnClickListener {
            runDbUpdate(force = true, quiet = false)
        }
        fab.setOnClickListener { showAddBlacklistDialog(null) }

        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshLists()
        updateDbStatus()
        populateSimSwitches()
        if (SpamDbUpdater.autoCheckDue(prefs)) {
            runDbUpdate(force = false, quiet = true)
        }
    }

    // ---------------------------------------------------------------- setup

    private fun setupToolbar() {
        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        clearLogMenuItem = toolbar.menu.add(getString(R.string.clear_log)).apply {
            setIcon(R.drawable.ic_delete)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            isVisible = false
            setOnMenuItemClickListener {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.clear_log))
                    .setPositiveButton(getString(R.string.clear_log)) { _, _ ->
                        store.clear()
                        refreshLists()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
                true
            }
        }
    }

    private fun bindSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        findViewById<MaterialSwitch>(id).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }
    }

    private fun setupSwitches() {
        bindSwitch(R.id.enableSwitch, prefs.blockingEnabled) {
            prefs.blockingEnabled = it; updateStatus()
        }
        bindSwitch(R.id.unknownSwitch, prefs.blockUnknown) { prefs.blockUnknown = it }
        bindSwitch(R.id.hiddenSwitch, prefs.blockHidden) { prefs.blockHidden = it }
        bindSwitch(R.id.ratingSwitch, prefs.blockByRating) { prefs.blockByRating = it }
        bindSwitch(R.id.callerInfoSwitch, prefs.notifyCallerInfo) { prefs.notifyCallerInfo = it }
        bindSwitch(R.id.notifyBlockedSwitch, prefs.notifyBlocked) { prefs.notifyBlocked = it }
        bindSwitch(R.id.autoUpdateSwitch, prefs.autoUpdateDb) { prefs.autoUpdateDb = it }
    }

    private fun setupLists() {
        logAdapter = BlockedCallAdapter()
        findViewById<RecyclerView>(R.id.blockedList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
        blacklistAdapter = BlacklistAdapter()
        findViewById<RecyclerView>(R.id.blacklistList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = blacklistAdapter
        }
    }

    private fun setupNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.nav_blacklist -> 1
                R.id.nav_log -> 2
                else -> 0
            }
            findViewById<View>(R.id.pageHome).visibility =
                if (page == 0) View.VISIBLE else View.GONE
            findViewById<View>(R.id.pageBlacklist).visibility =
                if (page == 1) View.VISIBLE else View.GONE
            findViewById<View>(R.id.pageLog).visibility =
                if (page == 2) View.VISIBLE else View.GONE
            if (page == 1) fab.show() else fab.hide()
            clearLogMenuItem?.isVisible = page == 2
            true
        }
    }

    // ---------------------------------------------------------- permissions

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermissions() {
        val wanted = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= 33) {
            wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = wanted.filterNot { hasPermission(it) }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    // ------------------------------------------------------------------ SIM

    private fun populateSimSwitches() {
        val container = findViewById<LinearLayout>(R.id.simContainer)
        val title = findViewById<TextView>(R.id.simTitle)
        val card = findViewById<MaterialCardView>(R.id.simCard)
        container.removeAllViews()
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            title.visibility = View.GONE
            card.visibility = View.GONE
            return
        }
        val tm = getSystemService(TelecomManager::class.java)
        val accounts = try {
            tm.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList()
        }
        val show = accounts.size >= 2
        title.visibility = if (show) View.VISIBLE else View.GONE
        card.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
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

    // ------------------------------------------------------------ role/status

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
        val hasContacts = hasPermission(Manifest.permission.READ_CONTACTS)

        roleButton.visibility = if (hasRole) View.GONE else View.VISIBLE
        statusText.text = when {
            !hasRole -> getString(R.string.status_no_role)
            !hasContacts -> getString(R.string.status_no_contacts)
            prefs.blockingEnabled -> getString(R.string.status_active)
            else -> getString(R.string.status_paused)
        }

        val active = hasRole && hasContacts && prefs.blockingEnabled
        val error = !hasRole || !hasContacts
        val bgAttr: Int
        val fgAttr: Int
        val icon: Int
        when {
            error -> {
                bgAttr = com.google.android.material.R.attr.colorErrorContainer
                fgAttr = com.google.android.material.R.attr.colorOnErrorContainer
                icon = R.drawable.ic_warning
            }
            active -> {
                bgAttr = com.google.android.material.R.attr.colorPrimaryContainer
                fgAttr = com.google.android.material.R.attr.colorOnPrimaryContainer
                icon = R.drawable.ic_shield_check
            }
            else -> {
                bgAttr = com.google.android.material.R.attr.colorSurfaceVariant
                fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant
                icon = R.drawable.ic_shield
            }
        }
        val bg = MaterialColors.getColor(statusCard, bgAttr)
        val fg = MaterialColors.getColor(statusCard, fgAttr)
        statusCard.setCardBackgroundColor(bg)
        statusIcon.setImageResource(icon)
        ImageViewCompat.setImageTintList(statusIcon, ColorStateList.valueOf(fg))
        statusText.setTextColor(fg)
    }

    // ------------------------------------------------------------- spam DB

    private fun updateDbStatus() {
        val count = try {
            spamDb.count()
        } catch (e: Exception) {
            0L
        }
        val version = spamDb.getMeta("db_version")
        dbStatusText.text = if (count > 0 && version != null) {
            getString(R.string.db_status, count, version)
        } else {
            getString(R.string.db_status_empty)
        }
    }

    private fun runDbUpdate(force: Boolean, quiet: Boolean) {
        if (!quiet) snack(getString(R.string.db_updating))
        SpamDbUpdater.updateAsync(this, force) { result ->
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                updateDbStatus()
                if (quiet) return@runOnUiThread
                when (result) {
                    is SpamDbUpdater.Result.Updated ->
                        snack(getString(R.string.db_updated, result.count, result.version))
                    is SpamDbUpdater.Result.UpToDate ->
                        snack(getString(R.string.db_up_to_date))
                    is SpamDbUpdater.Result.Failed ->
                        snack(getString(R.string.db_update_failed, result.message))
                }
            }
        }
    }

    private fun snack(message: String) {
        Snackbar.make(findViewById(R.id.bottomNav), message, Snackbar.LENGTH_LONG)
            .setAnchorView(bottomNav)
            .show()
    }

    // ------------------------------------------------------------ blacklist

    private fun showAddBlacklistDialog(prefill: String?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_blacklist, null)
        val patternInput = view.findViewById<TextInputEditText>(R.id.inputPattern)
        val labelInput = view.findViewById<TextInputEditText>(R.id.inputLabel)
        prefill?.let { patternInput.setText(NumberUtils.normalize(it)) }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_blacklist_title))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val pattern = NumberUtils.normalizePattern(patternInput.text?.toString().orEmpty())
                if (pattern.isEmpty()) return@setPositiveButton
                val added = blacklist.add(pattern, labelInput.text?.toString().orEmpty())
                snack(
                    getString(
                        if (added) R.string.added_to_blacklist
                        else R.string.already_in_blacklist
                    )
                )
                refreshLists()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showLogItemDialog(item: BlockedCall) {
        val options = arrayOf(
            getString(R.string.add_to_blacklist),
            getString(R.string.copy_number)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(item.number)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddBlacklistDialog(item.number)
                    1 -> {
                        val cm = getSystemService(ClipboardManager::class.java)
                        cm.setPrimaryClip(ClipData.newPlainText("number", item.number))
                        snack(getString(R.string.copied))
                    }
                }
            }
            .show()
    }

    // ---------------------------------------------------------------- lists

    private fun refreshLists() {
        val logItems = store.getAll()
        logAdapter.submit(logItems)
        findViewById<View>(R.id.logEmpty).visibility =
            if (logItems.isEmpty()) View.VISIBLE else View.GONE

        val blItems = blacklist.getAll()
        blacklistAdapter.submit(blItems)
        findViewById<View>(R.id.blacklistEmpty).visibility =
            if (blItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun reasonText(reason: String): String = when (reason) {
        BlockedCallStore.REASON_BLACKLIST -> getString(R.string.reason_blacklist)
        BlockedCallStore.REASON_HIDDEN -> getString(R.string.reason_hidden)
        BlockedCallStore.REASON_SPAM -> getString(R.string.reason_spam)
        else -> getString(R.string.reason_not_in_contacts)
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
            val reason: TextView = view.findViewById(R.id.itemReason)
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
            holder.reason.text = reasonText(item.reason)
            holder.itemView.setOnClickListener { showLogItemDialog(item) }
        }

        override fun getItemCount() = items.size
    }

    private inner class BlacklistAdapter :
        RecyclerView.Adapter<BlacklistAdapter.Holder>() {

        private var items: List<BlacklistEntry> = emptyList()

        fun submit(newItems: List<BlacklistEntry>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val pattern: TextView = view.findViewById(R.id.itemPattern)
            val label: TextView = view.findViewById(R.id.itemLabel)
            val delete: ImageButton = view.findViewById(R.id.itemDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blacklist, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.pattern.text = item.pattern
            holder.label.text = item.label
            holder.label.visibility = if (item.label.isEmpty()) View.GONE else View.VISIBLE
            holder.delete.setOnClickListener {
                blacklist.remove(item.id)
                refreshLists()
            }
        }

        override fun getItemCount() = items.size
    }
}
