package ru.sharl0t.utvplayer.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.leanback.LeanbackPlayerAdapter
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Net

/** Воспроизведение HLS: пауза, перемотка, выбор качества. Ошибка: сообщение с «Назад»; конец серии: выход. */
class PlayerFragment : VideoSupportFragment() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireContext()
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(Net.client)))
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = keepScreenOn(isPlaying)

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) requireActivity().finish()
            }

            override fun onPlayerError(error: PlaybackException) {
                val message = getString(R.string.play_error, error.message ?: error.errorCodeName)
                // Экран мог уже уйти в фон; при возврате LoadActivity всё равно грузит заново
                parentFragmentManager.commit(allowStateLoss = true) {
                    replace(R.id.content, MessageFragment.info(message))
                }
            }
        })

        val glue = Glue(LeanbackPlayerAdapter(context, player, 500))
        glue.host = VideoSupportFragmentGlueHost(this)
        glue.title = requireArguments().getString(ARG_TITLE)
        glue.isSeekEnabled = true
        glue.playWhenPrepared()

        player.setMediaItem(MediaItem.fromUri(requireArguments().getString(ARG_URL)!!))
        player.prepare()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroy() {
        keepScreenOn(false)
        player.release()
        super.onDestroy()
    }

    private fun keepScreenOn(on: Boolean) {
        val window = activity?.window ?: return
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Список «Авто» и высот из плейлиста; выбранная высота закрепляется, «Авто» снимает закрепление. */
    private fun showQualityDialog() {
        val group = player.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO } ?: return
        val tracks = (0 until group.length)
            .filter { group.isTrackSupported(it) }
            .distinctBy { group.getTrackFormat(it).height }
            .sortedByDescending { group.getTrackFormat(it).height }
        val override = player.trackSelectionParameters.overrides[group.mediaTrackGroup]
        val checked = override?.trackIndices?.firstOrNull()?.let { tracks.indexOf(it) + 1 } ?: 0

        // В режиме «Авто» в скобках текущая высота, которую выбрал ExoPlayer
        val current = player.videoFormat?.takeIf { checked == 0 }?.let { " (${it.height}p)" }.orEmpty()
        val auto = getString(R.string.quality_auto) + current
        val labels = listOf(auto) + tracks.map { "${group.getTrackFormat(it).height}p" }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quality)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                if (which > 0) builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tracks[which - 1]))
                player.trackSelectionParameters = builder.build()
                dialog.dismiss()
            }
            .show()
    }

    private inner class Glue(adapter: LeanbackPlayerAdapter) :
        PlaybackTransportControlGlue<LeanbackPlayerAdapter>(requireContext(), adapter) {
        private val rewind = PlaybackControlsRow.RewindAction(context)
        private val fastForward = PlaybackControlsRow.FastForwardAction(context)
        private val quality = Action(ACTION_QUALITY, context.getString(R.string.quality)).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_quality)
        }

        override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
            primaryActionsAdapter.add(rewind)
            super.onCreatePrimaryActions(primaryActionsAdapter)
            primaryActionsAdapter.add(fastForward)
        }

        override fun onCreateSecondaryActions(secondaryActionsAdapter: ArrayObjectAdapter) {
            secondaryActionsAdapter.add(quality)
        }

        override fun onActionClicked(action: Action) = when (action) {
            rewind -> player.seekBack()
            fastForward -> player.seekForward()
            quality -> showQualityDialog()
            else -> super.onActionClicked(action)
        }
    }

    companion object {
        private const val ARG_URL = "url"
        private const val ARG_TITLE = "title"
        private const val ACTION_QUALITY = 1L

        fun create(url: String, title: String) = PlayerFragment().apply {
            arguments = bundleOf(ARG_URL to url, ARG_TITLE to title)
        }
    }
}
