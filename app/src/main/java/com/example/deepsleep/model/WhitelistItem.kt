package com.example.deepsleep.model

import java.util.UUID

enum class WhitelistType {
    SUPPRESS,
    BACKGROUND,
    NETWORK  // 网络白名单
}

data class WhitelistItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val note: String = "",
    val type: WhitelistType
)
