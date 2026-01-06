package com.property.propertymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.HouseAdapter
import com.property.propertymanagement.network.HouseAddRequest
import com.property.propertymanagement.network.HouseUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HouseManagementActivity : AppCompatActivity() {
    private lateinit var rvHouses: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var houseAdapter: HouseAdapter
    private var houseList = mutableListOf<com.property.propertymanagement.network.HouseResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService
    private var buildingId: Long = 0
    private var buildingName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_house_management)

        // 获取传递的参数
        buildingId = intent.getLongExtra("buildingId", 0)
        buildingName = intent.getStringExtra("buildingName") ?: ""

        apiService = RetrofitClient.createApiService(this)

        initViews()
        initRecyclerView()
        loadHouses()

        // 只有管理员显示添加按钮
        fabAdd.visibility = if (PermissionUtil.isAdmin(this)) View.VISIBLE else View.GONE

        // 设置标题
        supportActionBar?.title = if (buildingId > 0) "$buildingName - 房屋列表" else "房屋管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViews() {
        rvHouses = findViewById(R.id.rv_houses)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener {
            showAddHouseDialog()
        }
    }

    private fun initRecyclerView() {
        houseAdapter = HouseAdapter(houseList,
            onItemClick = { house ->
                showHouseDetail(house)
            },
            onItemLongClick = { house ->
                if (PermissionUtil.isAdmin(this)) {
                    showHouseOptionsDialog(house)
                    true
                } else {
                    false
                }
            }
        )

        rvHouses.layoutManager = LinearLayoutManager(this)
        rvHouses.adapter = houseAdapter
    }

    private fun loadHouses() {
        if (buildingId > 0) {
            loadHousesByBuilding()
        } else {
            loadAllHouses()
        }
    }

    private fun loadHousesByBuilding() {
        apiService.getHousesByBuilding(buildingId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    houseList.clear()
                    houseList.addAll(data)
                    houseAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@HouseManagementActivity, "加载房屋数据失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllHouses() {
        apiService.getAllHouses().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    houseList.clear()
                    houseList.addAll(data)
                    houseAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@HouseManagementActivity, "加载房屋数据失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddHouseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_house_add, null)
        val etBuildingId = dialogView.findViewById<TextInputEditText>(R.id.et_building_id)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etFloor = dialogView.findViewById<TextInputEditText>(R.id.et_floor)
        val etUnitType = dialogView.findViewById<TextInputEditText>(R.id.et_unit_type)
        val etArea = dialogView.findViewById<TextInputEditText>(R.id.et_area)
        val etRoomCount = dialogView.findViewById<TextInputEditText>(R.id.et_room_count)
        val etLivingRoomCount = dialogView.findViewById<TextInputEditText>(R.id.et_living_room_count)
        val etBathroomCount = dialogView.findViewById<TextInputEditText>(R.id.et_bathroom_count)
        val etOrientation = dialogView.findViewById<TextInputEditText>(R.id.et_orientation)
        val etHouseStatus = dialogView.findViewById<TextInputEditText>(R.id.et_house_status)
        val etOwnerName = dialogView.findViewById<TextInputEditText>(R.id.et_owner_name)
        val etOwnerPhone = dialogView.findViewById<TextInputEditText>(R.id.et_owner_phone)
        val etOwnerIdCard = dialogView.findViewById<TextInputEditText>(R.id.et_owner_id_card)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etResidentPhone = dialogView.findViewById<TextInputEditText>(R.id.et_resident_phone)
        val etResidentType = dialogView.findViewById<TextInputEditText>(R.id.et_resident_type)
        val etContractStartDate = dialogView.findViewById<TextInputEditText>(R.id.et_contract_start_date)
        val etContractEndDate = dialogView.findViewById<TextInputEditText>(R.id.et_contract_end_date)

        // 如果是从楼栋页面跳转过来的，自动填充楼栋ID
        if (buildingId > 0) {
            etBuildingId.setText(buildingId.toString())
            etBuildingId.isEnabled = false
        }

        AlertDialog.Builder(this)
            .setTitle("添加房屋")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val buildingIdStr = etBuildingId.text.toString().trim()
                val houseNumber = etHouseNumber.text.toString().trim()
                val floorStr = etFloor.text.toString().trim()
                val unitType = etUnitType.text.toString().trim()
                val areaStr = etArea.text.toString().trim()
                val roomCountStr = etRoomCount.text.toString().trim()
                val livingRoomCountStr = etLivingRoomCount.text.toString().trim()
                val bathroomCountStr = etBathroomCount.text.toString().trim()
                val orientation = etOrientation.text.toString().trim()
                val houseStatus = etHouseStatus.text.toString().trim()
                val ownerName = etOwnerName.text.toString().trim()
                val ownerPhone = etOwnerPhone.text.toString().trim()
                val ownerIdCard = etOwnerIdCard.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val residentPhone = etResidentPhone.text.toString().trim()
                val residentType = etResidentType.text.toString().trim()
                val contractStartDate = etContractStartDate.text.toString().trim()
                val contractEndDate = etContractEndDate.text.toString().trim()

                if (validateHouseInput(buildingIdStr, houseNumber, floorStr)) {
                    val buildingId = buildingIdStr.toLong()
                    val floor = floorStr.toInt()
                    val area = if (areaStr.isNotEmpty()) areaStr.toDouble() else null
                    val roomCount = if (roomCountStr.isNotEmpty()) roomCountStr.toInt() else null
                    val livingRoomCount = if (livingRoomCountStr.isNotEmpty()) livingRoomCountStr.toInt() else null
                    val bathroomCount = if (bathroomCountStr.isNotEmpty()) bathroomCountStr.toInt() else null

                    addHouse(buildingId, houseNumber, floor, unitType, area, roomCount,
                        livingRoomCount, bathroomCount, orientation, houseStatus,
                        ownerName, ownerPhone, ownerIdCard, residentName,
                        residentPhone, residentType, contractStartDate, contractEndDate)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateHouseInput(buildingIdStr: String, houseNumber: String, floorStr: String): Boolean {
        return when {
            buildingIdStr.isEmpty() -> {
                Toast.makeText(this, "请输入楼栋ID", Toast.LENGTH_SHORT).show()
                false
            }
            houseNumber.isEmpty() -> {
                Toast.makeText(this, "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            floorStr.isEmpty() -> {
                Toast.makeText(this, "请输入楼层", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addHouse(buildingId: Long, houseNumber: String, floor: Int,
                         unitType: String?, area: Double?, roomCount: Int?,
                         livingRoomCount: Int?, bathroomCount: Int?, orientation: String?,
                         houseStatus: String?, ownerName: String?, ownerPhone: String?,
                         ownerIdCard: String?, residentName: String?, residentPhone: String?,
                         residentType: String?, contractStartDate: String?, contractEndDate: String?) {
        val request = HouseAddRequest(
            buildingId, houseNumber, floor, unitType, area, roomCount,
            livingRoomCount, bathroomCount, orientation, houseStatus,
            ownerName, ownerPhone, ownerIdCard, residentName,
            residentPhone, residentType, contractStartDate, contractEndDate
        )

        apiService.addHouse(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@HouseManagementActivity, "添加成功", Toast.LENGTH_SHORT).show()
                    loadHouses()
                } else {
                    val errorMsg = response.body()?.msg ?: "添加失败"
                    Toast.makeText(this@HouseManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showHouseOptionsDialog(house: com.property.propertymanagement.network.HouseResponse) {
        val options = arrayOf("编辑房屋", "删除房屋")

        AlertDialog.Builder(this)
            .setTitle("操作选项")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditHouseDialog(house)
                    1 -> showDeleteHouseDialog(house.id)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditHouseDialog(house: com.property.propertymanagement.network.HouseResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_house_add, null)
        val etBuildingId = dialogView.findViewById<TextInputEditText>(R.id.et_building_id)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etFloor = dialogView.findViewById<TextInputEditText>(R.id.et_floor)
        val etUnitType = dialogView.findViewById<TextInputEditText>(R.id.et_unit_type)
        val etArea = dialogView.findViewById<TextInputEditText>(R.id.et_area)
        val etRoomCount = dialogView.findViewById<TextInputEditText>(R.id.et_room_count)
        val etLivingRoomCount = dialogView.findViewById<TextInputEditText>(R.id.et_living_room_count)
        val etBathroomCount = dialogView.findViewById<TextInputEditText>(R.id.et_bathroom_count)
        val etOrientation = dialogView.findViewById<TextInputEditText>(R.id.et_orientation)
        val etHouseStatus = dialogView.findViewById<TextInputEditText>(R.id.et_house_status)
        val etOwnerName = dialogView.findViewById<TextInputEditText>(R.id.et_owner_name)
        val etOwnerPhone = dialogView.findViewById<TextInputEditText>(R.id.et_owner_phone)
        val etOwnerIdCard = dialogView.findViewById<TextInputEditText>(R.id.et_owner_id_card)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etResidentPhone = dialogView.findViewById<TextInputEditText>(R.id.et_resident_phone)
        val etResidentType = dialogView.findViewById<TextInputEditText>(R.id.et_resident_type)
        val etContractStartDate = dialogView.findViewById<TextInputEditText>(R.id.et_contract_start_date)
        val etContractEndDate = dialogView.findViewById<TextInputEditText>(R.id.et_contract_end_date)

        // 填充现有数据
        etBuildingId.setText(house.buildingId.toString())
        etHouseNumber.setText(house.houseNumber)
        etFloor.setText(house.floor.toString())
        etUnitType.setText(house.unitType ?: "")
        etArea.setText(house.area?.toString() ?: "")
        etRoomCount.setText(house.roomCount?.toString() ?: "")
        etLivingRoomCount.setText(house.livingRoomCount?.toString() ?: "")
        etBathroomCount.setText(house.bathroomCount?.toString() ?: "")
        etOrientation.setText(house.orientation ?: "")
        etHouseStatus.setText(house.houseStatus ?: "")
        etOwnerName.setText(house.ownerName ?: "")
        etOwnerPhone.setText(house.ownerPhone ?: "")
        etOwnerIdCard.setText(house.ownerIdCard ?: "")
        etResidentName.setText(house.residentName ?: "")
        etResidentPhone.setText(house.residentPhone ?: "")
        etResidentType.setText(house.residentType ?: "")
        etContractStartDate.setText(house.contractStartDate ?: "")
        etContractEndDate.setText(house.contractEndDate ?: "")

        // 禁止修改楼栋ID
        etBuildingId.isEnabled = false

        AlertDialog.Builder(this)
            .setTitle("编辑房屋")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val buildingIdStr = etBuildingId.text.toString().trim()
                val houseNumber = etHouseNumber.text.toString().trim()
                val floorStr = etFloor.text.toString().trim()
                val unitType = etUnitType.text.toString().trim()
                val areaStr = etArea.text.toString().trim()
                val roomCountStr = etRoomCount.text.toString().trim()
                val livingRoomCountStr = etLivingRoomCount.text.toString().trim()
                val bathroomCountStr = etBathroomCount.text.toString().trim()
                val orientation = etOrientation.text.toString().trim()
                val houseStatus = etHouseStatus.text.toString().trim()
                val ownerName = etOwnerName.text.toString().trim()
                val ownerPhone = etOwnerPhone.text.toString().trim()
                val ownerIdCard = etOwnerIdCard.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val residentPhone = etResidentPhone.text.toString().trim()
                val residentType = etResidentType.text.toString().trim()
                val contractStartDate = etContractStartDate.text.toString().trim()
                val contractEndDate = etContractEndDate.text.toString().trim()

                if (validateHouseInput(buildingIdStr, houseNumber, floorStr)) {
                    val buildingId = buildingIdStr.toLong()
                    val floor = floorStr.toInt()
                    val area = if (areaStr.isNotEmpty()) areaStr.toDouble() else null
                    val roomCount = if (roomCountStr.isNotEmpty()) roomCountStr.toInt() else null
                    val livingRoomCount = if (livingRoomCountStr.isNotEmpty()) livingRoomCountStr.toInt() else null
                    val bathroomCount = if (bathroomCountStr.isNotEmpty()) bathroomCountStr.toInt() else null

                    updateHouse(house.id, buildingId, houseNumber, floor, unitType, area, roomCount,
                        livingRoomCount, bathroomCount, orientation, houseStatus,
                        ownerName, ownerPhone, ownerIdCard, residentName,
                        residentPhone, residentType, contractStartDate, contractEndDate)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateHouse(houseId: Long, buildingId: Long, houseNumber: String, floor: Int,
                            unitType: String?, area: Double?, roomCount: Int?,
                            livingRoomCount: Int?, bathroomCount: Int?, orientation: String?,
                            houseStatus: String?, ownerName: String?, ownerPhone: String?,
                            ownerIdCard: String?, residentName: String?, residentPhone: String?,
                            residentType: String?, contractStartDate: String?, contractEndDate: String?) {
        val request = HouseUpdateRequest(
            houseId, buildingId, houseNumber, floor, unitType, area, roomCount,
            livingRoomCount, bathroomCount, orientation, houseStatus,
            ownerName, ownerPhone, ownerIdCard, residentName,
            residentPhone, residentType, contractStartDate, contractEndDate
        )

        apiService.updateHouse(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@HouseManagementActivity, "更新成功", Toast.LENGTH_SHORT).show()
                    loadHouses()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(this@HouseManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteHouseDialog(houseId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除房屋")
            .setMessage("确定要删除这个房屋吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteHouse(houseId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteHouse(houseId: Long) {
        apiService.deleteHouse(houseId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@HouseManagementActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    loadHouses()
                } else {
                    val errorMsg = response.body()?.msg ?: "删除失败"
                    Toast.makeText(this@HouseManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showHouseDetail(house: com.property.propertymanagement.network.HouseResponse) {
        val message = """
            房号：${house.houseNumber}
            楼栋：${house.buildingName ?: house.buildingNumber}
            楼层：${house.floor}
            户型：${house.unitType ?: "未设置"}
            面积：${house.area ?: "未设置"}㎡
            朝向：${house.orientation ?: "未设置"}
            房间数：${house.roomCount ?: "未设置"}
            客厅数：${house.livingRoomCount ?: "未设置"}
            卫生间数：${house.bathroomCount ?: "未设置"}
            房屋状态：${getHouseStatusText(house.houseStatus)}
            
            业主信息：
            姓名：${house.ownerName ?: "未登记"}
            电话：${house.ownerPhone ?: "未登记"}
            身份证：${house.ownerIdCard ?: "未登记"}
            
            住户信息：
            姓名：${house.residentName ?: "暂无"}
            电话：${house.residentPhone ?: "暂无"}
            类型：${getResidentTypeText(house.residentType)}
            租约开始：${house.contractStartDate ?: "无"}
            租约结束：${house.contractEndDate ?: "无"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("房屋详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getHouseStatusText(status: String?): String {
        return when (status) {
            "OCCUPIED" -> "已入住"
            "VACANT" -> "空置"
            "RENTED" -> "出租"
            else -> status ?: "未知"
        }
    }

    private fun getResidentTypeText(type: String?): String {
        return when (type) {
            "OWNER" -> "业主自住"
            "TENANT" -> "租客"
            else -> type ?: "未知"
        }
    }

    private fun updateEmptyView() {
        findViewById<TextView>(R.id.tv_empty).visibility =
            if (houseList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_house_management_menu, menu)

        // 设置搜索功能
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchHouses(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadHouses()
                }
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_search -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun searchHouses(keyword: String) {
        apiService.searchHouses(keyword, keyword).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    houseList.clear()
                    houseList.addAll(data)
                    houseAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@HouseManagementActivity, "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.HousePageResponse>>, t: Throwable) {
                Toast.makeText(this@HouseManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}