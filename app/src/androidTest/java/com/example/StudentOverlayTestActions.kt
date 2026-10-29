package com.example

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Like an extra user scroll: place the target above the floating navigation before tapping. */
fun SemanticsNodeInteraction.scrollAboveNavigation(
    rule: androidx.compose.ui.test.junit4.ComposeTestRule
): SemanticsNodeInteraction {
    performScrollTo()
    val bars = rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes()
    if (bars.isEmpty()) return this
    val target = fetchSemanticsNode()
    val bar = bars.single()
    fun rootId(node: androidx.compose.ui.semantics.SemanticsNode): Int {
        var root = node
        while (root.parent != null) root = root.parent!!
        return root.id
    }
    // A modal window owns its own viewport; the app bar behind it cannot obscure its controls.
    if (rootId(target) != rootId(bar)) return this
    val delta = target.boundsInRoot.bottom - bar.boundsInRoot.top + 8 * rule.density.density
    var parent = target.parent
    while (
        parent != null &&
            !(parent.config.contains(SemanticsProperties.VerticalScrollAxisRange) &&
                parent.config.contains(SemanticsActions.ScrollBy))
    ) parent = parent.parent
    val owner =
        checkNotNull(parent) { "The target needs a vertical scroll owner to clear navigation" }
    // ScrollBy launches an animation and returns immediately. Await the suspending variant
    // so a following touch cannot be consumed merely to stop that animation.
    val scroll = owner.config[SemanticsActions.ScrollByOffset]
    val frameClock = BroadcastFrameClock()
    var frameNanos = 0L
    var job: Job? = null
    var failure: Throwable? = null
    rule.runOnIdle {
        job =
            CoroutineScope(Dispatchers.Unconfined + frameClock).launch {
                try {
                    scroll(Offset(0f, delta.coerceAtLeast(0f)))
                } catch (error: Throwable) {
                    failure = error
                }
            }
    }
    try {
        rule.waitUntil(5000) {
            rule.runOnIdle {
                repeat(120) {
                    if (frameClock.hasAwaiters) {
                        frameNanos += 16_666_667L
                        frameClock.sendFrame(frameNanos)
                    }
                }
            }
            job?.isCompleted == true
        }
        rule.waitForIdle()
        rule.runOnIdle {
            failure?.let { throw AssertionError("Overlay clearance scroll failed", it) }
        }
    } finally {
        rule.runOnIdle { job?.cancel() }
    }
    check(fetchSemanticsNode().boundsInRoot.bottom <= bar.boundsInRoot.top + 1f) {
        "The target remains obscured by navigation"
    }
    return this
}
