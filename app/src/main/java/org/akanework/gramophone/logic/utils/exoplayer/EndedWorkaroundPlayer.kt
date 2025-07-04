package org.akanework.gramophone.logic.utils.exoplayer

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.logic.utils.CircularShuffleOrder


/**
 * If player in STATE_ENDED is resumed, state will be STATE_READY, on play button press it will
 * update to STATE_ENDED and only then media3 will wrap around playlist for us. This is a workaround
 * to restore STATE_ENDED as well and fake it for media3 until it indeed wraps around playlist.
 */
@OptIn(UnstableApi::class)
class EndedWorkaroundPlayer(player: ExoPlayer) : ForwardingPlayer(player), Player.Listener {

    companion object {
        private const val TAG = "EndedWorkaroundPlayer"
    }

    val exoPlayer
        get() = wrappedPlayer as ExoPlayer
    // TODO: can't we do this in a cleaner way?
    var nextShuffleOrder:
            ((firstIndex: Int, mediaItemCount: Int, EndedWorkaroundPlayer) -> CircularShuffleOrder)?
            = null
    var isEnded = false
        set(value) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "isEnded set to $value (was $field)")
            }
            field = value
            if (field) {
                wrappedPlayer.addListener(this)
            } else {
                wrappedPlayer.removeListener(this)
            }
        }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == DISCONTINUITY_REASON_SEEK) {
            isEnded = false
        }
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }

    override fun getPlaybackState(): Int {
        if (isEnded) return STATE_ENDED
        return super.getPlaybackState()
    }
}