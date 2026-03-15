package com.property.propertymanagement.network

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)