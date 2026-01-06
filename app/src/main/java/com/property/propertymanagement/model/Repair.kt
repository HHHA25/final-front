package com.property.propertymanagement.model

data class Repair(
    val id: Int,
    val houseNumber: String,    // 房号
    val residentName: String,   // 住户姓名
    val phone: String,          // 联系电话
    val type: String,           // 维修类型
    val description: String,    // 问题描述
    val status: String,         // 状态 (待处理/处理中/已完成)
    val submitTime: String,     // 提交时间 (格式: yyyy-MM-dd HH:mm)
    val completeTime: String?   // 完成时间 (格式: yyyy-MM-dd HH:mm)
)