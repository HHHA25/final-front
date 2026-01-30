package com.property.propertymanagement.util

import com.property.propertymanagement.network.*
import java.util.*

object SearchUtil {

    // 搜索楼栋
    fun filterBuildings(
        buildings: List<BuildingResponse>,
        query: String
    ): List<BuildingResponse> {
        if (query.isBlank()) return buildings

        val lowerQuery = query.lowercase(Locale.getDefault())
        return buildings.filter { building ->
            building.buildingNumber?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    building.buildingName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    building.buildingType?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    building.status?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true
        }
    }

    // 搜索房屋
    fun filterHouses(
        houses: List<HouseResponse>,
        query: String
    ): List<HouseResponse> {
        if (query.isBlank()) return houses

        val lowerQuery = query.lowercase(Locale.getDefault())
        return houses.filter { house ->
            house.houseNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    house.buildingName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    house.ownerName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    house.residentName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    house.unitType?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    house.houseStatus.lowercase(Locale.getDefault()).contains(lowerQuery)
        }
    }

    // 搜索投诉
    fun filterComplaints(
        complaints: List<ComplaintResponse>,
        query: String
    ): List<ComplaintResponse> {
        if (query.isBlank()) return complaints

        val lowerQuery = query.lowercase(Locale.getDefault())
        return complaints.filter { complaint ->
            complaint.houseNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    complaint.residentName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    complaint.type.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    complaint.content.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    complaint.status.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    complaint.handleResult?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true
        }
    }

    // 搜索物业费
    fun filterFees(
        fees: List<FeeResponse>,
        query: String
    ): List<FeeResponse> {
        if (query.isBlank()) return fees

        val lowerQuery = query.lowercase(Locale.getDefault())
        return fees.filter { fee ->
            fee.houseNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    fee.residentName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    fee.month.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    fee.status.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    fee.amount.toString().contains(query)
        }
    }

    // 搜索车位
    fun filterParkings(
        parkings: List<ParkingResponse>,
        query: String
    ): List<ParkingResponse> {
        if (query.isBlank()) return parkings

        val lowerQuery = query.lowercase(Locale.getDefault())
        return parkings.filter { parking ->
            parking.parkingNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    parking.houseNumber?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    parking.residentName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    parking.carPlate?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    parking.status.lowercase(Locale.getDefault()).contains(lowerQuery)
        }
    }

    // 搜索维修
    fun filterRepairs(
        repairs: List<RepairResponse>,
        query: String
    ): List<RepairResponse> {
        if (query.isBlank()) return repairs

        val lowerQuery = query.lowercase(Locale.getDefault())
        return repairs.filter { repair ->
            repair.houseNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    repair.residentName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    repair.type.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    repair.description?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    repair.status.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    repair.feedback?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true
        }
    }

    // 搜索用户
    fun filterUsers(
        users: List<UserResponse>,
        query: String
    ): List<UserResponse> {
        if (query.isBlank()) return users

        val lowerQuery = query.lowercase(Locale.getDefault())
        return users.filter { user ->
            user.username.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    user.name.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    user.houseNumber?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    user.phone?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true ||
                    user.role.lowercase(Locale.getDefault()).contains(lowerQuery)
        }
    }

    // 搜索注册请求
    fun filterRegistrationRequests(
        requests: List<RegistrationResponse>,
        query: String
    ): List<RegistrationResponse> {
        if (query.isBlank()) return requests

        val lowerQuery = query.lowercase(Locale.getDefault())
        return requests.filter { request ->
            request.username.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    request.houseNumber.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    request.status.lowercase(Locale.getDefault()).contains(lowerQuery)
        }
    }
}