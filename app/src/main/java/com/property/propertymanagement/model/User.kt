package com.property.propertymanagement.model

// 用户角色枚举
enum class UserRole {
    ADMIN,    // 管理员
    RESIDENT  // 小区居民
}

// 用户数据模型
data class User(
    val id: Int,
    val username: String,
    val password: String,
    val role: UserRole,
    val houseNumber: String? = null  // 居民才需要房号
)