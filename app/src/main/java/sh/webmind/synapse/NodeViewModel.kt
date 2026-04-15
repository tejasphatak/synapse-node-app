package sh.webmind.synapse

import android.app.Application
import android.content.*
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sh.webmind.synapse.data.LatestRelease
import sh.webmind.synapse.data.Stats
import sh.webmind.synapse.data.StatsRepo
import sh.webmind.synapse.data.SynapseUpdater
import sh.webmind.synapse.service.NodeService

class NodeViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()
    private val repo = StatsRepo(ctx)

    val stats: StateFlow<Stats> = repo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())
    val contributing: StateFlow<Boolean> = repo.contributing.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val chargingOnly: StateFlow<Boolean> = repo.chargingOnly.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _coordinatorUrl = MutableStateFlow("https://synapse.webmind.sh/")
    val coordinatorUrl: StateFlow<String> = _coordinatorUrl.asStateFlow()

    // ── Update state ──
    private val _availableUpdate = MutableStateFlow<LatestRelease?>(null)
    val availableUpdate: StateFlow<LatestRelease?> = _availableUpdate.asStateFlow()
    private val _updateDownloading = MutableStateFlow(false)
    val updateDownloading: StateFlow<Boolean> = _updateDownloading.asStateFlow()

    init {
        // Periodic update check — on launch and every 6h
        viewModelScope.launch {
            while (true) {
                try {
                    val current = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
                    val release = SynapseUpdater.checkForUpdate(current)
                    if (release != null && SynapseUpdater.apkAssetOf(release) != null) {
                        _availableUpdate.value = release
                    }
                } catch (_: Exception) { }
                kotlinx.coroutines.delay(6 * 60 * 60 * 1000L)
            }
        }
    }

    fun installUpdate() {
        val release = _availableUpdate.value ?: return
        val asset = SynapseUpdater.apkAssetOf(release) ?: return
        viewModelScope.launch {
            _updateDownloading.value = true
            try {
                val file = SynapseUpdater.downloadApk(ctx, asset.downloadUrl)
                SynapseUpdater.promptInstall(ctx, file)
            } catch (_: Exception) { }
            _updateDownloading.value = false
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    fun toggleContributing() {
        val on = !contributing.value
        viewModelScope.launch {
            repo.setContributing(on)
            if (on) NodeService.start(ctx) else NodeService.stop(ctx)
        }
    }

    fun setChargingOnly(v: Boolean) {
        viewModelScope.launch { repo.setChargingOnly(v) }
    }

    fun setCoordinator(url: String) {
        _coordinatorUrl.value = url
    }

    fun recordTokens(n: Long) = viewModelScope.launch { repo.recordTokens(n) }
    fun recordRequest() = viewModelScope.launch { repo.recordRequest() }
    fun addActiveSeconds(s: Long) = viewModelScope.launch { repo.addActiveSeconds(s) }

    fun batteryLevel(): Int {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(): Boolean {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
}
