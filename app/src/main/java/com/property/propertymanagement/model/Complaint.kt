package com.property.propertymanagement.model

data class Complaint(
    val id: Int,
    val houseNumber: String,    // 房号
    val residentName: String,   // 住户姓名
    val phone: String,          // 联系电话
    val type: String,           // 投诉类型
    val content: String,        // 投诉内容
    val status: String,         // 状态 (待处理/处理中/已解决)
    val submitTime: String,     // 提交时间 (格式: yyyy-MM-dd HH:mm)
    val handleResult: String?   // 处理结果
)