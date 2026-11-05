package ru.sharl0t.utvplayer.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Stats

/** Настройки: переключатель обезличенной статистики, слева ссылки автора QR-кодами. */
class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, SettingsFragment(), android.R.id.content)
        }
    }
}

class SettingsFragment : GuidedStepSupportFragment() {
    override fun onProvideTheme() = R.style.SettingsTheme

    override fun onCreateGuidanceStylist() = object : GuidanceStylist() {
        override fun onProvideLayoutId() = R.layout.settings_guidance
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?) = GuidanceStylist.Guidance(
        getString(R.string.settings), getString(R.string.settings_description), "", null
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions += GuidedAction.Builder(requireContext())
            .id(ACTION_STATS)
            .title(R.string.stats_share)
            .description(R.string.stats_share_description)
            .multilineDescription(true)
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(Stats.isEnabled(requireContext()))
            .build()
        // Только показ, никуда не отправляется: пользователь может назвать это при обращении за помощью
        actions += GuidedAction.Builder(requireContext())
            .id(ACTION_INFO)
            .title(R.string.debug_info)
            .description(debugInfo())
            .multilineDescription(true)
            .infoOnly(true)
            .focusable(false)
            .build()
    }

    private fun debugInfo(): String {
        val ctx = requireContext()
        val version = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
        return getString(
            R.string.debug_info_format, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.firstOrNull().orEmpty(), version
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == ACTION_STATS) Stats.setEnabled(requireContext(), action.isChecked)
    }

    private companion object {
        const val ACTION_STATS = 1L
        const val ACTION_INFO = 2L
    }
}
