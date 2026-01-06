package com.property.propertymanagement.model

data class Parking(
    val id: Int,
    val parkingNumber: String,  // 车位编号
    val houseNumber: String,    // 对应房号
    val residentName: String,   // 住户姓名
    val carPlate: String,       // 车牌号
    val status: String,         // 状态 (已分配/空闲)
    val startTime: String       // 分配开始时间 (格式: yyyy-MM-dd)
)