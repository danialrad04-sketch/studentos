package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*

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
    if (delta <= 0) return this
    var parent = target.parent
    while (
        parent != null &&
            !(parent.config.contains(SemanticsProperties.VerticalScrollAxisRange) &&
                parent.config.contains(SemanticsActions.ScrollBy))
    ) parent = parent.parent
    val owner =
        checkNotNull(parent) { "The target needs a vertical scroll owner to clear navigation" }
    rule
        .onNode(SemanticsMatcher("vertical scroll owner") { it.id == owner.id })
        .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
    check(fetchSemanticsNode().boundsInRoot.bottom <= bar.boundsInRoot.top + 1f) {
        "The target remains obscured by navigation"
    }
    return this
}
