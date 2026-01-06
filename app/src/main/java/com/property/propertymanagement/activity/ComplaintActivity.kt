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
import com.property.propertymanagement.adapter.ComplaintAdapter
import com.property.propertymanagement.network.ComplaintSubmitRequest
import com.property.propertymanagement.network.ComplaintUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComplaintActivity : AppCompatActivity() {
    private lateinit var rvComplaintRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmpty: TextView
    private lateinit var complaintAdapter: ComplaintAdapter
    private var complaintList = mutableListOf<com.property.propertymanagement.network.ComplaintResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint)

        apiService = RetrofitClient.createApiService(this)

        initViews()
        initRecyclerView()
        loadComplaintData()

        // 所有人都可以看到添加按钮
        fabAdd.visibility = View.VISIBLE
    }

    private fun initViews() {
        // 使用安全的方式查找视图
        rvComplaintRecords = findViewById(R.id.rv_complaint_records)
        fabAdd = findViewById(R.id.fab_add)
        tvEmpty = findViewById(R.id.tv_empty)

        fabAdd.setOnClickListener {
            showAddComplaintDialog()
        }
    }

    private fun initRecyclerView() {
        complaintAdapter = ComplaintAdapter(complaintList,
            onItemClick = { complaint ->
                // 管理员可以处理投诉
                if (PermissionUtil.isAdmin(this)) {
                    showUpdateComplaintDialog(complaint)
                }
            },
            onItemLongClick = { complaint ->
                // 管理员可以删除投诉
                if (PermissionUtil.isAdmin(this)) {
                    showDeleteConfirmationDialog(complaint.id)
                    true
                } else {
                    false
                }
            }
        )

        rvComplaintRecords.layoutManager = LinearLayoutManager(this)
        rvComplaintRecords.adapter = complaintAdapter
    }

    private fun loadComplaintData() {
        if (PermissionUtil.isAdmin(this)) {
            loadAllComplaints()
        } else {
            loadMyComplaints()
        }
    }

    private fun loadAllComplaints() {
        apiService.getAllComplaints().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    complaintList.clear()
                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@ComplaintActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                Toast.makeText(this@ComplaintActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyComplaints() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(this)
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(this, "无法获取房号信息", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.getMyComplaints(houseNumber).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    complaintList.clear()
                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@ComplaintActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                Toast.makeText(this@ComplaintActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddComplaintDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complaint, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_phone)
        val etType = dialogView.findViewById<TextInputEditText>(R.id.et_type)
        val etContent = dialogView.findViewById<TextInputEditText>(R.id.et_content)

        // 如果是居民，自动填充房号和姓名
        if (!PermissionUtil.isAdmin(this)) {
            val houseNumber = PermissionUtil.getCurrentHouseNumber(this)
            val residentName = PermissionUtil.getCurrentUserName(this)
            etHouseNumber.setText(houseNumber)
            etResidentName.setText(residentName)
            etHouseNumber.isEnabled = false // 居民不能修改房号
        }

        AlertDialog.Builder(this)
            .setTitle("提交投诉")
            .setView(dialogView)
            .setPositiveButton("提交") { _, _ ->
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val type = etType.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (validateComplaintInput(houseNumber, residentName, phone, type, content)) {
                    submitComplaint(houseNumber, residentName, phone, type, content)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateComplaintInput(
        houseNumber: String,
        residentName: String,
        phone: String,
        type: String,
        content: String
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
                Toast.makeText(this, "请输入投诉类型", Toast.LENGTH_SHORT).show()
                false
            }
            content.isEmpty() -> {
                Toast.makeText(this, "请输入投诉内容", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun submitComplaint(houseNumber: String, residentName: String, phone: String, type: String, content: String) {
        val request = ComplaintSubmitRequest(houseNumber, residentName, phone, type, content)

        apiService.submitComplaint(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@ComplaintActivity, "提交成功", Toast.LENGTH_SHORT).show()
                    loadComplaintData()
                } else {
                    val errorMsg = response.body()?.msg ?: "提交失败"
                    Toast.makeText(this@ComplaintActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@ComplaintActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUpdateComplaintDialog(complaint: com.property.propertymanagement.network.ComplaintResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complaint_update, null)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)
        val etHandleResult = dialogView.findViewById<TextInputEditText>(R.id.et_handle_result)

        // 设置默认值
        etStatus.setText(complaint.status)
        etHandleResult.setText(complaint.handleResult ?: "")

        AlertDialog.Builder(this)
            .setTitle("更新投诉状态")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val status = etStatus.text.toString().trim()
                val handleResult = etHandleResult.text.toString().trim()

                if (status.isNotEmpty()) {
                    updateComplaint(complaint.id, status, handleResult)
                } else {
                    Toast.makeText(this, "请输入状态", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateComplaint(complaintId: Long, status: String, handleResult: String) {
        val request = ComplaintUpdateRequest(complaintId, status, handleResult)

        apiService.updateComplaint(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@ComplaintActivity, "更新成功", Toast.LENGTH_SHORT).show()
                    loadComplaintData()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(this@ComplaintActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@ComplaintActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteConfirmationDialog(complaintId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                // 注意：后端可能没有提供删除投诉的接口
                Toast.makeText(this, "删除功能暂未实现", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        tvEmpty.visibility = if (complaintList.isEmpty()) View.VISIBLE else View.GONE
    }
}