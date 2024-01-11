/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate.bindings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction

/**
 * Binding to update the [AppStore] based on changes to [BrowserState].
 */
class BrowserStoreBinding(
    browserStore: BrowserStore,
    private val appStore: AppStore,
) : AbstractBinding<BrowserState>(browserStore) {
    override suspend fun onState(flow: Flow<BrowserState>) {
        // Update the AppStore with the latest selected tab
        flow.distinctUntilChangedBy { it.selectedTabId }
            .collectLatest { state ->
                state.selectedTab?.let { tab ->
                    // Ignore re-emissions of the selected tab from BrowserStore when re-observing due
                    // to lifecycle events, otherwise pieces of state like [mode] may get overwritten.
                    if (appStore.state.selectedTabInfo?.id != tab.id && state.newTabIntentionallySelected()) {
                        appStore.dispatch(AppAction.SelectedTabChanged(tab))
                    }
                }
            }
    }

    /** TODO mode won't update if the last normal tab is manually selected from the tray
     *  so we need a way to have mode be fully divorced from the selected tab, but the TabListReducer
     *  selects a regular tab automatically. the tabs tray click then never chooses the selected tab
     *  (unless the clicked tab is different than the fallback tab)
     *
     *  options:
     *  combined stores would let mode and selected tab respond appropriately to the same actions
     *  BrowserStore middleware that dispatches changes to app store instead of this binding?
     *  talk to product and ignore bug?
     * */
    private fun BrowserState.newTabIntentionallySelected(): Boolean {
        val newMode = BrowsingMode.fromBoolean(selectedTab?.content?.private ?: false)
        val anyTabInOldMode = tabs.any { appStore.state.selectedTabInfo?.mode == newMode }
        val anyOtherTabs = tabs.size > 1
        return anyTabInOldMode && anyOtherTabs
    }
}
