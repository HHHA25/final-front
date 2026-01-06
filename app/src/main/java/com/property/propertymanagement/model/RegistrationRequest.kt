package com.property.propertymanagement.model

// 定义注册请求状态枚举
enum class RequestStatus {
    PENDING,    // 待审批
    APPROVED,   // 已批准
    REJECTED    // 已拒绝
}

data class RegistrationRequest(
    val id: Int,
    val username: String,
    val password: String,
    val houseNumber: String,
    val status: RequestStatus, // 使用枚举类型
    val submitTime: String
)