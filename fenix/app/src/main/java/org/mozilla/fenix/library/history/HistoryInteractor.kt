/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.selection.SelectionInteractor
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

/**
 * Interface for the HistoryInteractor. This interface is implemented by objects that want
 * to respond to user interaction on the HistoryView
 */
interface HistoryInteractor : SelectionInteractor<History> {
    /**
     * Called when search is tapped
     */
    fun onSearch()

    /**
     * Called when the delete menu button is tapped.
     */
    fun onDeleteTimeRange()

    /**
     * Called when multiple history items are deleted
     * @param items the history items to delete
     */
    fun onDeleteSome(items: Set<History>)

    /**
     * Called when the user has confirmed deletion of a time range.
     *
     * @param timeFrame The selected timeframe. `null` means no specific time frame has been
     * selected; should remove everything.
     */
    fun onDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?)

    /**
     * Called when the user requests a sync of the history
     */
    fun onRequestSync()

    /**
     * Called when the user clicks on recently closed tab button.
     */
    fun onRecentlyClosedClicked()
}

/**
 * Interactor for the history screen
 * Provides implementations for the HistoryInteractor
 */
@SuppressWarnings("TooManyFunctions")
class DefaultHistoryInteractor(
    private val historyController: HistoryController,
) : HistoryInteractor {
    override fun open(item: History) = Unit

    override fun select(item: History) = Unit

    override fun deselect(item: History) = Unit

    override fun onSearch() {
        GleanHistory.searchIconTapped.record(NoExtras())
        historyController.handleSearch()
    }

    override fun onDeleteTimeRange() {
        historyController.handleDeleteTimeRange()
    }

    override fun onDeleteSome(items: Set<History>) {
        historyController.handleDeleteSome(items)
    }

    override fun onDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?) {
        historyController.handleDeleteTimeRangeConfirmed(timeFrame)
    }

    override fun onRequestSync() {
        historyController.handleRequestSync()
    }

    override fun onRecentlyClosedClicked() {
        historyController.handleEnterRecentlyClosed()
    }
}
