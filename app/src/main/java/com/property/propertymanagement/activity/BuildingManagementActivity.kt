package com.property.propertymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.BuildingAdapter
import com.property.propertymanagement.network.*
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BuildingManagementActivity : AppCompatActivity() {
    private lateinit var rvBuildings: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var buildingAdapter: BuildingAdapter
    private var buildingList = mutableListOf<BuildingResponse>()
    private lateinit var apiService: ApiService

    // 分页相关变量
    private var currentPage = 1
    private val pageSize = 10
    private var isLoading = false
    private var isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building_management)

        apiService = RetrofitClient.createApiService(this)

        initViews()
        initRecyclerView()
        loadBuildings(true) // 初次加载，刷新模式

        // 只有管理员显示添加按钮
        fabAdd.visibility = if (PermissionUtil.isAdmin(this)) View.VISIBLE else View.GONE
    }

    private fun initViews() {
        rvBuildings = findViewById(R.id.rv_buildings)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener {
            showAddBuildingDialog()
        }
    }

    private fun initRecyclerView() {
        buildingAdapter = BuildingAdapter(buildingList,
            onItemClick = { building ->
                showBuildingDetail(building)
            },
            onItemLongClick = { building ->
                if (PermissionUtil.isAdmin(this)) {
                    showBuildingOptionsDialog(building)
                    true
                } else {
                    false
                }
            }
        )

        val layoutManager = LinearLayoutManager(this)
        rvBuildings.layoutManager = layoutManager
        rvBuildings.adapter = buildingAdapter

        // 添加滚动监听实现分页加载
        rvBuildings.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount
                        && firstVisibleItem >= 0
                        && totalItemCount >= pageSize) {

                        currentPage++
                        loadBuildings()
                    }
                }
            }
        })
    }

    private fun loadBuildings(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            isLastPage = false
            buildingList.clear()
            buildingAdapter.notifyDataSetChanged()
        }

        if (isLoading || isLastPage) return

        isLoading = true
        apiService.getAllBuildings(currentPage, pageSize).enqueue(object : Callback<ApiResult<BuildingPageResponse>> {
            override fun onResponse(
                call: Call<ApiResult<BuildingPageResponse>>,
                response: Response<ApiResult<BuildingPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        buildingList.clear()
                    }
                    buildingList.addAll(data)
                    buildingAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(this@BuildingManagementActivity, "加载楼栋数据失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<BuildingPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(this@BuildingManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun refreshData() {
        loadBuildings(true)
    }

    private fun showAddBuildingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_building_add, null)
        val etBuildingNumber = dialogView.findViewById<TextInputEditText>(R.id.et_building_number)
        val etBuildingName = dialogView.findViewById<TextInputEditText>(R.id.et_building_name)
        val etTotalFloors = dialogView.findViewById<TextInputEditText>(R.id.et_total_floors)
        val etTotalUnits = dialogView.findViewById<TextInputEditText>(R.id.et_total_units)
        val etBuildingType = dialogView.findViewById<TextInputEditText>(R.id.et_building_type)
        val etCompletionDate = dialogView.findViewById<TextInputEditText>(R.id.et_completion_date)

        AlertDialog.Builder(this)
            .setTitle("添加楼栋")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val buildingNumber = etBuildingNumber.text.toString().trim()
                val buildingName = etBuildingName.text.toString().trim()
                val totalFloorsStr = etTotalFloors.text.toString().trim()
                val totalUnitsStr = etTotalUnits.text.toString().trim()
                val buildingType = etBuildingType.text.toString().trim()
                val completionDate = etCompletionDate.text.toString().trim()

                if (validateBuildingInput(buildingNumber, buildingName, totalFloorsStr, totalUnitsStr)) {
                    val totalFloors = totalFloorsStr.toInt()
                    val totalUnits = totalUnitsStr.toInt()
                    addBuilding(buildingNumber, buildingName, totalFloors, totalUnits, buildingType, completionDate)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateBuildingInput(
        buildingNumber: String,
        buildingName: String,
        totalFloorsStr: String,
        totalUnitsStr: String
    ): Boolean {
        return when {
            buildingNumber.isEmpty() -> {
                Toast.makeText(this, "请输入楼栋编号", Toast.LENGTH_SHORT).show()
                false
            }
            buildingName.isEmpty() -> {
                Toast.makeText(this, "请输入楼栋名称", Toast.LENGTH_SHORT).show()
                false
            }
            totalFloorsStr.isEmpty() -> {
                Toast.makeText(this, "请输入总层数", Toast.LENGTH_SHORT).show()
                false
            }
            totalUnitsStr.isEmpty() -> {
                Toast.makeText(this, "请输入总户数", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addBuilding(buildingNumber: String, buildingName: String, totalFloors: Int, totalUnits: Int, buildingType: String, completionDate: String) {
        val request = BuildingAddRequest(buildingNumber, buildingName, totalFloors, totalUnits, buildingType, completionDate)

        apiService.addBuilding(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(
                call: Call<ApiResult<Void>>,
                response: Response<ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@BuildingManagementActivity, "添加成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "添加失败"
                    Toast.makeText(this@BuildingManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@BuildingManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showBuildingOptionsDialog(building: BuildingResponse) {
        val options = arrayOf("编辑楼栋", "查看房屋", "删除楼栋")

        AlertDialog.Builder(this)
            .setTitle("操作选项")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditBuildingDialog(building)
                    1 -> {
                        val intent = Intent(this, HouseManagementActivity::class.java)
                        intent.putExtra("buildingId", building.id)
                        intent.putExtra("buildingName", building.buildingName)
                        startActivity(intent)
                    }
                    2 -> showDeleteBuildingDialog(building.id)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditBuildingDialog(building: BuildingResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_building_add, null)
        val etBuildingNumber = dialogView.findViewById<TextInputEditText>(R.id.et_building_number)
        val etBuildingName = dialogView.findViewById<TextInputEditText>(R.id.et_building_name)
        val etTotalFloors = dialogView.findViewById<TextInputEditText>(R.id.et_total_floors)
        val etTotalUnits = dialogView.findViewById<TextInputEditText>(R.id.et_total_units)
        val etBuildingType = dialogView.findViewById<TextInputEditText>(R.id.et_building_type)
        val etCompletionDate = dialogView.findViewById<TextInputEditText>(R.id.et_completion_date)

        etBuildingNumber.setText(building.buildingNumber)
        etBuildingName.setText(building.buildingName)
        etTotalFloors.setText(building.totalFloors.toString())
        etTotalUnits.setText(building.totalUnits.toString())
        etBuildingType.setText(building.buildingType ?: "")

        AlertDialog.Builder(this)
            .setTitle("编辑楼栋")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val buildingNumber = etBuildingNumber.text.toString().trim()
                val buildingName = etBuildingName.text.toString().trim()
                val totalFloorsStr = etTotalFloors.text.toString().trim()
                val totalUnitsStr = etTotalUnits.text.toString().trim()
                val buildingType = etBuildingType.text.toString().trim()
                val completionDate = etCompletionDate.text.toString().trim()

                if (validateBuildingInput(buildingNumber, buildingName, totalFloorsStr, totalUnitsStr)) {
                    val totalFloors = totalFloorsStr.toInt()
                    val totalUnits = totalUnitsStr.toInt()
                    updateBuilding(building.id, buildingNumber, buildingName, totalFloors, totalUnits, buildingType, completionDate)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateBuilding(buildingId: Long, buildingNumber: String, buildingName: String, totalFloors: Int, totalUnits: Int, buildingType: String, completionDate: String) {
        val request = BuildingUpdateRequest(buildingId, buildingNumber, buildingName, totalFloors, totalUnits, buildingType, completionDate, "ACTIVE")

        apiService.updateBuilding(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(
                call: Call<ApiResult<Void>>,
                response: Response<ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@BuildingManagementActivity, "更新成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(this@BuildingManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@BuildingManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteBuildingDialog(buildingId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除楼栋")
            .setMessage("确定要删除这个楼栋吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteBuilding(buildingId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteBuilding(buildingId: Long) {
        apiService.deleteBuilding(buildingId).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(
                call: Call<ApiResult<Void>>,
                response: Response<ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@BuildingManagementActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "删除失败"
                    Toast.makeText(this@BuildingManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@BuildingManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showBuildingDetail(building: BuildingResponse) {
        val message = """
            楼栋编号：${building.buildingNumber}
            楼栋名称：${building.buildingName}
            总层数：${building.totalFloors}
            总户数：${building.totalUnits}
            楼栋类型：${building.buildingType ?: "未设置"}
            建成日期：${building.completionDate ?: "未设置"}
            状态：${if (building.status == "ACTIVE") "启用" else "停用"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("楼栋详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun updateEmptyView() {
        findViewById<TextView>(R.id.tv_empty).visibility =
            if (buildingList.isEmpty()) View.VISIBLE else View.GONE
    }
}