package com.property.propertymanagement.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.RepairAdapter
import com.property.propertymanagement.network.RepairSubmitRequest
import com.property.propertymanagement.network.RepairUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepairActivity : AppCompatActivity() {
    private lateinit var rvRepairRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repairAdapter: RepairAdapter
    private var repairList = mutableListOf<com.property.propertymanagement.network.RepairResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    // 在 onCreate 方法中修改权限控制
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repair) // 或 activity_complaint

        apiService = RetrofitClient.createApiService(this)

        initViews()
        initRecyclerView()
        loadRepairData() // 或 loadComplaintData()

        // 修复：管理员和居民都可以看到添加按钮，但点击后的逻辑不同
        fabAdd.visibility = View.VISIBLE

        // 如果需要根据角色显示不同的提示，可以这样：
        if (PermissionUtil.isAdmin(this)) {
            fabAdd.contentDescription = "管理员添加记录"
        } else {
            fabAdd.contentDescription = "提交申请"
        }
    }

    private fun initViews() {
        rvRepairRecords = findViewById(R.id.rv_repair_records)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener {
            showAddRepairDialog()
        }
    }

    private fun initRecyclerView() {
        repairAdapter = RepairAdapter(repairList,
            onItemClick = { repair ->
                // 管理员可以处理维修申请
                if (PermissionUtil.isAdmin(this)) {
                    showUpdateRepairDialog(repair)
                }
            },
            onItemLongClick = { repair ->
                // 管理员可以删除维修记录
                if (PermissionUtil.isAdmin(this)) {
                    showDeleteConfirmationDialog(repair.id)
                    true
                } else {
                    false
                }
            }
        )

        rvRepairRecords.layoutManager = LinearLayoutManager(this)
        rvRepairRecords.adapter = repairAdapter
    }

    private fun loadRepairData() {
        if (PermissionUtil.isAdmin(this)) {
            loadAllRepairs()
        } else {
            loadMyRepairs()
        }
    }

    private fun loadAllRepairs() {
        apiService.getAllRepairs().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    repairList.clear()
                    repairList.addAll(data)
                    repairAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@RepairActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>, t: Throwable) {
                Toast.makeText(this@RepairActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyRepairs() {
        apiService.getMyRepairs().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse >> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    repairList.clear()
                    repairList.addAll(data)
                    repairAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@RepairActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>, t: Throwable) {
                Toast.makeText(this@RepairActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddRepairDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_repair, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_phone)
        val etType = dialogView.findViewById<TextInputEditText>(R.id.et_type)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_description)

        // 如果是居民，自动填充房号和姓名
        if (!PermissionUtil.isAdmin(this)) {
            val houseNumber = PermissionUtil.getCurrentHouseNumber(this)
            val residentName = PermissionUtil.getCurrentUserName(this)
            etHouseNumber.setText(houseNumber)
            etResidentName.setText(residentName)
            etHouseNumber.isEnabled = false // 居民不能修改房号
        }

        AlertDialog.Builder(this)
            .setTitle("提交维修申请")
            .setView(dialogView)
            .setPositiveButton("提交") { _, _ ->
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val type = etType.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (validateRepairInput(houseNumber, residentName, phone, type)) {
                    submitRepair(houseNumber, residentName, phone, type, description)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateRepairInput(
        houseNumber: String,
        residentName: String,
        phone: String,
        type: String
    ): Boolean {
        return when {
            houseNumber.isEmpty() -> {
                Toast.makeText(this, "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            residentName.isEmpty() -> {
                Toast.makeText(this, "请输入住户姓名", Toast.LENGTH_SHORT).show()
                false
            }
            phone.isEmpty() -> {
                Toast.makeText(this, "请输入联系电话", Toast.LENGTH_SHORT).show()
                false
            }
            type.isEmpty() -> {
                Toast.makeText(this, "请输入维修类型", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun submitRepair(houseNumber: String, residentName: String, phone: String, type: String, description: String) {
        val userId = PermissionUtil.getCurrentUserId(this)
        if (userId == 0L) {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show()
            return
        }

        val request = RepairSubmitRequest(userId, houseNumber, residentName, phone, type, description)

        apiService.submitRepair(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@RepairActivity, "提交成功", Toast.LENGTH_SHORT).show()
                    loadRepairData()
                } else {
                    val errorMsg = response.body()?.msg ?: "提交失败"
                    Toast.makeText(this@RepairActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@RepairActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUpdateRepairDialog(repair: com.property.propertymanagement.network.RepairResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_repair_update, null)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)
        val etFeedback = dialogView.findViewById<TextInputEditText>(R.id.et_feedback)

        // 设置默认值
        etStatus.setText(repair.status)

        AlertDialog.Builder(this)
            .setTitle("更新维修状态")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val status = etStatus.text.toString().trim()
                val feedback = etFeedback.text.toString().trim()

                if (status.isNotEmpty()) {
                    updateRepair(repair.id, status, feedback)
                } else {
                    Toast.makeText(this, "请输入状态", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateRepair(repairId: Long, status: String, feedback: String) {
        val request = RepairUpdateRequest(repairId, status, feedback)

        apiService.updateRepair(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@RepairActivity, "更新成功", Toast.LENGTH_SHORT).show()
                    loadRepairData()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(this@RepairActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@RepairActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteConfirmationDialog(repairId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                // 注意：后端可能没有提供删除维修的接口
                Toast.makeText(this, "删除功能暂未实现", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        findViewById<TextView>(R.id.tv_empty).visibility =
            if (repairList.isEmpty()) View.VISIBLE else View.GONE
    }
}