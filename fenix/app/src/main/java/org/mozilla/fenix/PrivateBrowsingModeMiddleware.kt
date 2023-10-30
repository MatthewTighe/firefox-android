package org.mozilla.fenix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

class PrivateBrowsingModeMiddleware(
    private val settings: Settings,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction
    ) {
        next(action)
        when (action) {
            is AppAction.Init -> {
                scope.launch {
                    val mode = settings.lastKnownMode
                    context.dispatch(AppAction.ModeChange(mode))
                }
            }
            is AppAction.ModeChange -> {
                scope.launch {
                    settings.lastKnownMode = action.mode
                }
            }
            else -> Unit
        }
    }
}
