package sh.webmind.synapse.service

import android.app.*
import android.content.*
import android.media.*
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.*
import androidx.core.app.NotificationCompat
import sh.webmind.synapse.MainActivity
import sh.webmind.synapse.R

class NodeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var silentTrack: AudioTrack? = null
    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        // Silent AudioTrack + MediaSession: satisfies Android 14+ requirement
        // that mediaPlayback foreground services actually play media. The
        // track outputs silence at 0 gain; costs ~0 CPU, no audio hardware
        // wake, but marks the app as "media-active" so the system keeps the
        // process hot and GPU clocks up (via android:appCategory="game").
        startSilentMedia()

        // Acquire a partial wake lock so CPU stays awake while contributing
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Synapse:node").apply { acquire() }

        return START_STICKY
    }

    override fun onDestroy() {
        try { wakeLock?.release() } catch (_: Exception) {}
        try { silentTrack?.stop(); silentTrack?.release() } catch (_: Exception) {}
        try { mediaSession?.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun startSilentMedia() {
        try {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(2048)
            silentTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            ).apply {
                // Write a single buffer of silence and loop forever
                val silence = ShortArray(bufferSize / 2)
                write(silence, 0, silence.size)
                setLoopPoints(0, silence.size, -1)
                setVolume(0f)
                play()
            }
        } catch (e: Exception) {
            // AudioTrack setup is best-effort; if it fails, mediaPlayback FG
            // service may still work on older Android where media verification
            // is lenient. Don't block node startup on this.
            android.util.Log.w("SynapseNode", "silent AudioTrack setup failed: ${e.message}")
        }

        try {
            mediaSession = MediaSession(this, "SynapseNode").apply {
                setPlaybackState(
                    PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                        .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                        .build()
                )
                isActive = true
            }
        } catch (e: Exception) {
            android.util.Log.w("SynapseNode", "MediaSession setup failed: ${e.message}")
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Compute node",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while your phone is contributing compute"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Synapse node — contributing")
            .setContentText("Your phone is thinking")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "synapse_node"
        const val NOTIF_ID = 1

        fun start(ctx: Context) {
            val i = Intent(ctx, NodeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, NodeService::class.java))
        }
    }
}
