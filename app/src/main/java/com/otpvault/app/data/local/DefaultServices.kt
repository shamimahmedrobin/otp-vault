package com.otpvault.app.data.local

import com.otpvault.app.data.local.entity.ServiceEntity

/** Built-in services seeded on first launch. Icons are Material icon names. */
object DefaultServices {
    private val now = 0L // seeded rows are timeless; createdAt filled at insert time by repository if 0
    val all: List<ServiceEntity> = listOf(
        svc("bkash", "bKash", "MFS", "phone_android"),
        svc("nagad", "Nagad", "MFS", "phone_android"),
        svc("rocket", "Rocket", "MFS", "rocket_launch"),
        svc("cellfin", "CellFin", "MFS", "account_balance_wallet"),
        svc("upay", "Upay", "MFS", "payments"),
        svc("tap", "TAP", "MFS", "touch_app"),
        svc("bank", "Bank", "Bank", "account_balance"),
        svc("email", "Email", "Account", "mail"),
        svc("social", "Social Media", "Account", "share"),
        svc("shopping", "Shopping", "Shopping", "shopping_bag"),
        svc("other", "Other", "Other", "apps")
    )
    private fun svc(id: String, name: String, cat: String, icon: String) =
        ServiceEntity(id = id, name = name, category = cat, icon = icon, isCustom = false, createdAt = now)
}
