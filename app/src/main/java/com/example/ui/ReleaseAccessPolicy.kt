package com.example.ui

/** Public release policy, identical for every user; Firebase/backend implementations stay intact. */
object ReleaseAccessPolicy {
    const val onlineAccountEntryEnabled = false

    // Uniform policy for guests and existing accounts; never keyed to reviewer identity.
    // Keep in sync with the permission removal in AndroidManifest.xml.
    const val bazaarBillingEnabled = false
}
