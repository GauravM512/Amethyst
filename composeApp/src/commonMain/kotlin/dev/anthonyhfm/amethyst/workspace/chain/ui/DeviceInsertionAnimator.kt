package dev.anthonyhfm.amethyst.workspace.chain.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Tracks devices that have just been inserted via a drag/drop so that
 * their composables can play an entry animation (scale/alpha expand).
 */
object DeviceInsertionAnimator {
    private val _pending: SnapshotStateList<String> = mutableStateListOf()
    val pending: List<String> get() = _pending

    fun register(id: String) {
        if (!_pending.contains(id)) _pending.add(id)
    }

    fun consume(id: String) {
        _pending.remove(id)
    }
}

