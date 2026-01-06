package com.property.propertymanagement.model

data class Fee(
    val id: Int,
    val houseNumber: String,    // 房号
    val residentName: String,   // 住户姓名
    val amount: Double,         // 金额
    val month: String,          // 月份 (格式: yyyy-MM)
    val status: String,         // 状态 (已缴/未缴)
    val paymentDate: String?    // 缴费日期 (格式: yyyy-MM-dd)
)