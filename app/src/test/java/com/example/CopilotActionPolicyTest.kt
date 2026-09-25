package com.example

import com.example.domain.model.ActionImpactType
import com.example.domain.model.CopilotActionProposal
import com.example.domain.model.CopilotPayload
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopilotActionPolicyTest {

    private fun proposal(
        impact: ActionImpactType,
        destructive: Boolean = false
    ) = CopilotActionProposal(
        id = "policy",
        title = "Policy test",
        description = "Policy test",
        impactType = impact,
        payload = CopilotPayload.NavigateToTab("DASHBOARD"),
        isDestructive = destructive
    )

    @Test
    fun safe_query_does_not_require_confirmation() {
        assertFalse(proposal(ActionImpactType.SAFE_QUERY).requiresExplicitConfirmation)
    }

    @Test
    fun protected_read_only_does_not_require_mutation_confirmation() {
        assertFalse(proposal(ActionImpactType.PROTECTED_READONLY).requiresExplicitConfirmation)
    }

    @Test
    fun state_change_requires_confirmation() {
        assertTrue(proposal(ActionImpactType.REQUIRES_CONFIRMATION).requiresExplicitConfirmation)
    }

    @Test
    fun destructive_action_always_requires_confirmation() {
        assertTrue(proposal(ActionImpactType.SAFE_QUERY, destructive = true).requiresExplicitConfirmation)
    }
}
