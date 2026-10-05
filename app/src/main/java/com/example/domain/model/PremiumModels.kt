package com.example.domain.model

data class PremiumPlan(val productId: String, val title: String, val months: Int, val priceToman: Long)
data class PremiumStatus(
    val plans: List<PremiumPlan> = emptyList(),
    val salesEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val rsaPublicKey: String = "",
    val subscription: SubscriptionDetails = SubscriptionDetails()
)
data class BazaarBillingState(
    val busy: Boolean = false,
    val connected: Boolean = false,
    val prices: Map<String, String> = emptyMap(),
    val message: String? = null
)
object BazaarPrice {
    fun matches(display: String, expectedToman: Long): Boolean {
        val normalized = display.map { c -> when (c) {
            in '۰'..'۹' -> '0' + (c - '۰')
            in '٠'..'٩' -> '0' + (c - '٠')
            else -> c
        } }.joinToString("").replace(",", "").replace("٬", "").replace("\u200e", "").replace("\u200f", "")
        val numbers = Regex("[0-9]+").findAll(normalized).toList()
        if (numbers.size != 1) return false
        val amount = numbers.single().value.toLongOrNull() ?: return false
        return when {
            normalized.contains("تومان") -> amount == expectedToman
            normalized.contains("ریال") || normalized.contains("IRR", ignoreCase = true) -> amount == expectedToman * 10
            else -> false
        }
    }
}
