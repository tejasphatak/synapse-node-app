package sh.webmind.synapse

import android.app.Application
import android.content.*
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sh.webmind.synapse.data.Stats
import sh.webmind.synapse.data.StatsRepo
import sh.webmind.synapse.service.NodeService

class NodeViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()
    private val repo = StatsRepo(ctx)

    val stats: StateFlow<Stats> = repo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())
    val contributing: StateFlow<Boolean> = repo.contributing.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val chargingOnly: StateFlow<Boolean> = repo.chargingOnly.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _coordinatorUrl = MutableStateFlow("https://chat.webmind.sh/node")
    val coordinatorUrl: StateFlow<String> = _coordinatorUrl.asStateFlow()

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
