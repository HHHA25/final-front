package com.property.propertymanagement.network

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // 用户登录
    @POST("api/user/login")
    fun login(@Body loginRequest: LoginRequest): Call<ApiResult<String>>

    // 用户注册
    @POST("api/user/register")
    fun register(@Body registerRequest: RegisterRequest): Call<ApiResult<Void>>

    // 获取我的物业费
    @GET("api/fee/my")
    fun getMyFees(
        @Query("houseNumber") houseNumber: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<FeePageResponse>>

    // 获取所有物业费（管理员）
    @GET("api/fee/admin/all")
    fun getAllFees(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<FeePageResponse>>

    // 添加物业费（管理员）
    @POST("api/fee/admin/add")
    fun addFee(@Body fee: FeeAddRequest): Call<ApiResult<Void>>

    // 缴纳物业费
    @PUT("api/fee/pay")
    fun payFee(@Body payRequest: PayRequest): Call<ApiResult<Void>>

    // 获取我的维修记录 - 修改：使用userId
    @GET("api/repair/my")
    fun getMyRepairs(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<RepairPageResponse>>

    // 提交维修申请
    @POST("api/repair/submit")
    fun submitRepair(@Body repair: RepairSubmitRequest): Call<ApiResult<Void>>

    // 获取所有维修记录（管理员）
    @GET("api/repair/admin/all")
    fun getAllRepairs(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<RepairPageResponse>>

    // 更新维修状态（管理员）
    @PUT("api/repair/admin/update")
    fun updateRepair(@Body updateRequest: RepairUpdateRequest): Call<ApiResult<Void>>

    // 获取我的投诉
    @GET("api/complaint/my")
    fun getMyComplaints(
        @Query("houseNumber") houseNumber: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<ComplaintPageResponse>>

    // 提交投诉
    @POST("api/complaint/submit")
    fun submitComplaint(@Body complaint: ComplaintSubmitRequest): Call<ApiResult<Void>>

    // 获取所有投诉（管理员）
    @GET("api/complaint/admin/all")
    fun getAllComplaints(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<ComplaintPageResponse>>

    // 更新投诉状态（管理员）
    @PUT("api/complaint/admin/update")
    fun updateComplaint(@Body updateRequest: ComplaintUpdateRequest): Call<ApiResult<Void>>

    // 获取我的车位
    @GET("api/parking/my")
    fun getMyParkings(
        @Query("houseNumber") houseNumber: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<ParkingPageResponse>>

    // 获取所有车位（管理员）
    @GET("api/parking/admin/all")
    fun getAllParkings(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<ParkingPageResponse>>

    // 添加车位（管理员）
    @POST("api/parking/admin/add")
    fun addParking(@Body parking: ParkingAddRequest): Call<ApiResult<Void>>

    // 更新车位（管理员）
    @PUT("api/parking/admin/update")
    fun updateParking(@Body updateRequest: ParkingUpdateRequest): Call<ApiResult<Void>>

    // 删除车位（管理员）
    @DELETE("api/parking/admin/delete/{parkingId}")
    fun deleteParking(@Path("parkingId") parkingId: Long): Call<ApiResult<Void>>

    // 获取待审批注册请求数量（管理员）
    @GET("api/registration/admin/pending-count")
    fun getPendingCount(): Call<ApiResult<Int>>

    // 获取待审批注册请求列表（管理员）
    @GET("api/registration/admin/pending")
    fun getPendingRegistrations(): Call<ApiResult<List<RegistrationResponse>>>

    // 批准注册请求（管理员）
    @POST("api/registration/admin/approve/{requestId}")
    fun approveRegistration(@Path("requestId") requestId: Int): Call<ApiResult<Void>>

    // 拒绝注册请求（管理员）
    @POST("api/registration/admin/reject/{requestId}")
    fun rejectRegistration(@Path("requestId") requestId: Int): Call<ApiResult<Void>>

    // 获取所有用户（管理员）
    @GET("api/user/admin/all")
    fun getAllUsers(): Call<ApiResult<List<UserResponse>>>

    // 删除用户（管理员）
    @DELETE("api/user/admin/delete/{userId}")
    fun deleteUser(@Path("userId") userId: Long): Call<ApiResult<Void>>

    // 楼栋管理
    @GET("api/building/admin/all")
    fun getAllBuildings(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<BuildingPageResponse>>

    @POST("api/building/admin/add")
    fun addBuilding(@Body request: BuildingAddRequest): Call<ApiResult<Void>>

    @PUT("api/building/admin/update")
    fun updateBuilding(@Body request: BuildingUpdateRequest): Call<ApiResult<Void>>

    @DELETE("api/building/admin/delete/{buildingId}")
    fun deleteBuilding(@Path("buildingId") buildingId: Long): Call<ApiResult<Void>>

    @GET("api/building/{buildingId}")
    fun getBuildingDetail(@Path("buildingId") buildingId: Long): Call<ApiResult<BuildingResponse>>

    // 房屋管理
    @GET("api/house/building/{buildingId}")
    fun getHousesByBuilding(
        @Path("buildingId") buildingId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<HousePageResponse>>

    @GET("api/house/admin/all")
    fun getAllHouses(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<HousePageResponse>>

    @POST("api/house/admin/add")
    fun addHouse(@Body request: HouseAddRequest): Call<ApiResult<Void>>

    @PUT("api/house/admin/update")
    fun updateHouse(@Body request: HouseUpdateRequest): Call<ApiResult<Void>>

    @DELETE("api/house/admin/delete/{houseId}")
    fun deleteHouse(@Path("houseId") houseId: Long): Call<ApiResult<Void>>

    @GET("api/house/search")
    fun searchHouses(
        @Query("keyword") keyword: String,
        @Query("houseNumber") houseNumber: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Call<ApiResult<HousePageResponse>>

    @GET("api/house/{houseId}")
    fun getHouseDetail(@Path("houseId") houseId: Long): Call<ApiResult<HouseResponse>>

    // 直接创建用户（管理员）
    @POST("api/user/admin/create")
    fun createUser(@Body request: UserCreateRequest): Call<ApiResult<Void>>
}


// 分页响应数据类
data class FeePageResponse(
    val records: List<FeeResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

data class RepairPageResponse(
    val records: List<RepairResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

data class ComplaintPageResponse(
    val records: List<ComplaintResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

data class ParkingPageResponse(
    val records: List<ParkingResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

// 请求数据类
data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val name: String,
    val houseNumber: String,
    val phone: String? = null
)

data class PayRequest(
    val feeId: Long
)

data class RepairSubmitRequest(
    val userId: Long,
    val houseNumber: String,
    val residentName: String,
    val phone: String,
    val type: String,
    val description: String? = null,
    val imageUrl: String? = null
)

data class RepairUpdateRequest(
    val repairId: Long,
    val status: String,
    val feedback: String? = null
)

data class ComplaintSubmitRequest(
    val houseNumber: String,
    val residentName: String,
    val phone: String,
    val type: String,
    val content: String
)

data class ComplaintUpdateRequest(
    val complaintId: Long,
    val status: String,
    val handleResult: String? = null
)

data class FeeAddRequest(
    val houseNumber: String,
    val residentName: String,
    val amount: Double,
    val month: String
)

data class ParkingAddRequest(
    val parkingNumber: String,
    val houseNumber: String,
    val residentName: String,
    val carPlate: String,
    val status: String
)

data class ParkingUpdateRequest(
    val parkingId: Long,
    val parkingNumber: String,
    val houseNumber: String,
    val residentName: String,
    val carPlate: String,
    val status: String
)

// 响应数据类
data class FeeResponse(
    val id: Long,
    val houseNumber: String,
    val residentName: String,
    val amount: Double,
    val month: String,
    val status: String,
    val paymentDate: String?
)

data class RepairResponse(
    val id: Long,
    val userId: Long,
    val houseNumber: String,
    val residentName: String,
    val phone: String,
    val type: String,
    val description: String?,
    val status: String,
    val submitTime: String,
    val handleTime: String?,
    val feedback: String?
)

data class ComplaintResponse(
    val id: Long,
    val houseNumber: String,
    val residentName: String,
    val phone: String,
    val type: String,
    val content: String,
    val status: String,
    val submitTime: String,
    val handleResult: String?
)

data class ParkingResponse(
    val id: Long,
    val parkingNumber: String,
    val houseNumber: String?,
    val residentName: String?,
    val carPlate: String?,
    val status: String,
    val startTime: String?
)

data class RegistrationResponse(
    val id: Long,
    val username: String,
    val houseNumber: String,
    val status: String,
    val submitTime: String
)

data class UserResponse(
    val id: Long,
    val username: String,
    val name: String,
    val houseNumber: String,
    val phone: String?,
    val role: String,
    val status: Int,
    val createTime: String
)

// 统一响应格式
data class ApiResult<T>(
    val code: Int,
    val msg: String,
    val data: T?
)

data class BuildingPageResponse(
    val records: List<BuildingResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

data class BuildingResponse(
    val id: Long,
    val buildingNumber: String,
    val buildingName: String,
    val totalFloors: Int,
    val totalUnits: Int,
    val buildingType: String?,
    val completionDate: String?,
    val status: String,
    val createTime: String,
    val updateTime: String?
)

data class BuildingAddRequest(
    val buildingNumber: String,
    val buildingName: String,
    val totalFloors: Int,
    val totalUnits: Int,
    val buildingType: String?,
    val completionDate: String?
)

data class BuildingUpdateRequest(
    val id: Long,
    val buildingNumber: String,
    val buildingName: String,
    val totalFloors: Int,
    val totalUnits: Int,
    val buildingType: String?,
    val completionDate: String?,
    val status: String
)

data class HousePageResponse(
    val records: List<HouseResponse>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)

data class HouseResponse(
    val id: Long,
    val buildingId: Long,
    val buildingNumber: String?,
    val buildingName: String?,
    val houseNumber: String,
    val floor: Int,
    val unitType: String?,
    val area: Double?,
    val roomCount: Int?,
    val livingRoomCount: Int?,
    val bathroomCount: Int?,
    val orientation: String?,
    val houseStatus: String,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerIdCard: String?,
    val residentName: String?,
    val residentPhone: String?,
    val residentType: String?,
    val contractStartDate: String?,
    val contractEndDate: String?,
    val createTime: String,
    val updateTime: String?
)

data class HouseAddRequest(
    val buildingId: Long,
    val houseNumber: String,
    val floor: Int,
    val unitType: String?,
    val area: Double?,
    val roomCount: Int?,
    val livingRoomCount: Int?,
    val bathroomCount: Int?,
    val orientation: String?,
    val houseStatus: String?,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerIdCard: String?,
    val residentName: String?,
    val residentPhone: String?,
    val residentType: String?,
    val contractStartDate: String?,
    val contractEndDate: String?
)

data class HouseUpdateRequest(
    val id: Long,
    val buildingId: Long,
    val houseNumber: String,
    val floor: Int,
    val unitType: String?,
    val area: Double?,
    val roomCount: Int?,
    val livingRoomCount: Int?,
    val bathroomCount: Int?,
    val orientation: String?,
    val houseStatus: String?,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerIdCard: String?,
    val residentName: String?,
    val residentPhone: String?,
    val residentType: String?,
    val contractStartDate: String?,
    val contractEndDate: String?
)
// 添加用户创建请求类
data class UserCreateRequest(
    val username: String,
    val password: String,
    val name: String,
    val houseNumber: String?,
    val phone: String?,
    val role: String
)