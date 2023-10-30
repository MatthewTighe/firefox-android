package org.mozilla.fenix

import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.withContext
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.utils.Settings

class PrivateBrowsingBinding(
    appStore: AppStore,
    private val themeManager: ThemeManager,
    private val retrieveWindow: () -> Window,
    private val settings: Settings,
) : AbstractBinding<AppState>(appStore) {
    override suspend fun onState(flow: Flow<AppState>) {
        flow.distinctUntilChangedBy { it.mode }.collect {
            themeManager.currentTheme = it.mode
            setWindowPrivacy(it.mode)
        }
    }

    private suspend fun setWindowPrivacy(mode: BrowsingMode) {
        if (mode == BrowsingMode.Private) {
            val allowScreenshots = withContext(Dispatchers.IO) {
                settings.allowScreenshotsInPrivateMode
            }
            if (!allowScreenshots) {
                retrieveWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } else {
            retrieveWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
