package sh.webmind.synapse.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.statsStore by preferencesDataStore(name = "synapse_stats")

data class Stats(
    val tokensProcessed: Long = 0,
    val activeSeconds: Long = 0,
    val requestsHandled: Long = 0,
    val sessionsJoined: Int = 0,
    val firstStarted: Long = 0,
    val lastActive: Long = 0,
    val currentStreakDays: Int = 0,
    val badges: Set<String> = emptySet(),
    val handle: String = "anon-node"
)

object StatKeys {
    val TOKENS = longPreferencesKey("tokens_processed")
    val ACTIVE_S = longPreferencesKey("active_seconds")
    val REQUESTS = longPreferencesKey("requests_handled")
    val SESSIONS = intPreferencesKey("sessions_joined")
    val FIRST = longPreferencesKey("first_started")
    val LAST = longPreferencesKey("last_active")
    val STREAK = intPreferencesKey("streak_days")
    val BADGES = stringSetPreferencesKey("badges")
    val HANDLE = stringPreferencesKey("handle")
    val CONTRIBUTING = booleanPreferencesKey("is_contributing")
    val CHARGING_ONLY = booleanPreferencesKey("charging_only")
}

class StatsRepo(private val ctx: Context) {

    val flow: Flow<Stats> = ctx.statsStore.data.map { p ->
        Stats(
            tokensProcessed = p[StatKeys.TOKENS] ?: 0,
            activeSeconds = p[StatKeys.ACTIVE_S] ?: 0,
            requestsHandled = p[StatKeys.REQUESTS] ?: 0,
            sessionsJoined = p[StatKeys.SESSIONS] ?: 0,
            firstStarted = p[StatKeys.FIRST] ?: 0,
            lastActive = p[StatKeys.LAST] ?: 0,
            currentStreakDays = p[StatKeys.STREAK] ?: 0,
            badges = p[StatKeys.BADGES] ?: emptySet(),
            handle = p[StatKeys.HANDLE] ?: "anon-node-${System.nanoTime().toString().takeLast(4)}"
        )
    }

    suspend fun recordTokens(count: Long) {
        ctx.statsStore.edit { p ->
            p[StatKeys.TOKENS] = (p[StatKeys.TOKENS] ?: 0) + count
            p[StatKeys.LAST] = System.currentTimeMillis()
            if ((p[StatKeys.FIRST] ?: 0) == 0L) p[StatKeys.FIRST] = System.currentTimeMillis()
        }
        checkAchievements()
    }

    suspend fun recordRequest() {
        ctx.statsStore.edit { p ->
            p[StatKeys.REQUESTS] = (p[StatKeys.REQUESTS] ?: 0) + 1
            p[StatKeys.LAST] = System.currentTimeMillis()
        }
        checkAchievements()
    }

    suspend fun addActiveSeconds(s: Long) {
        ctx.statsStore.edit { p ->
            p[StatKeys.ACTIVE_S] = (p[StatKeys.ACTIVE_S] ?: 0) + s
        }
        checkAchievements()
    }

    suspend fun joinedSession() {
        ctx.statsStore.edit { p ->
            p[StatKeys.SESSIONS] = (p[StatKeys.SESSIONS] ?: 0) + 1
        }
    }

    private suspend fun checkAchievements() {
        ctx.statsStore.edit { p ->
            val badges = (p[StatKeys.BADGES] ?: emptySet()).toMutableSet()
            val tokens = p[StatKeys.TOKENS] ?: 0
            val seconds = p[StatKeys.ACTIVE_S] ?: 0
            val requests = p[StatKeys.REQUESTS] ?: 0

            if (tokens >= 1) badges += "first_token"
            if (tokens >= 100) badges += "hundred_tokens"
            if (tokens >= 1000) badges += "kilo_tokens"
            if (tokens >= 100_000) badges += "100k_tokens"
            if (seconds >= 3600) badges += "one_hour"
            if (seconds >= 24 * 3600) badges += "one_day"
            if (seconds >= 7 * 24 * 3600) badges += "one_week"
            if (requests >= 10) badges += "ten_requests"
            if (requests >= 100) badges += "hundred_requests"

            p[StatKeys.BADGES] = badges
        }
    }

    suspend fun setHandle(name: String) {
        ctx.statsStore.edit { p -> p[StatKeys.HANDLE] = name }
    }

    suspend fun setContributing(on: Boolean) {
        ctx.statsStore.edit { p -> p[StatKeys.CONTRIBUTING] = on }
    }

    val contributing: Flow<Boolean> = ctx.statsStore.data.map { it[StatKeys.CONTRIBUTING] ?: false }

    suspend fun setChargingOnly(on: Boolean) {
        ctx.statsStore.edit { p -> p[StatKeys.CHARGING_ONLY] = on }
    }

    val chargingOnly: Flow<Boolean> = ctx.statsStore.data.map { it[StatKeys.CHARGING_ONLY] ?: false }
}

object Badges {
    data class Badge(val id: String, val title: String, val emoji: String, val description: String)

    val ALL = listOf(
        Badge("first_token", "First spark", "⚡", "Your first contribution"),
        Badge("hundred_tokens", "Getting warm", "🌱", "100 tokens processed"),
        Badge("kilo_tokens", "Kilo thinker", "🧠", "1,000 tokens processed"),
        Badge("100k_tokens", "Heavy compute", "🔥", "100,000 tokens processed"),
        Badge("one_hour", "Hour of thought", "⏱", "1 hour of active compute"),
        Badge("one_day", "Day of service", "☀", "24 hours of active compute"),
        Badge("one_week", "Week in the mesh", "🌐", "7 days of active compute"),
        Badge("ten_requests", "Team player", "🤝", "Handled 10 requests"),
        Badge("hundred_requests", "Workhorse", "💪", "Handled 100 requests"),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id }
}
