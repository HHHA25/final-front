package com.property.propertymanagement.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.FeeAdapter
import com.property.propertymanagement.network.FeeAddRequest
import com.property.propertymanagement.network.PayRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeeManagementActivity : AppCompatActivity() {
    private lateinit var rvFeeRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var feeAdapter: FeeAdapter
    private var feeList = mutableListOf<com.property.propertymanagement.network.FeeResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fee_management)

        apiService = RetrofitClient.createApiService(this)

        initViews()
        initRecyclerView()
        loadFeeData()

        // 根据角色控制"添加"按钮显示
        fabAdd.visibility = if (PermissionUtil.isAdmin(this)) View.VISIBLE else View.GONE
    }

    private fun initViews() {
        rvFeeRecords = findViewById(R.id.rv_fee_records)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener {
            showAddFeeDialog()
        }
    }

    private fun initRecyclerView() {
        feeAdapter = FeeAdapter(feeList,
            onItemClick = { fee ->
                if (PermissionUtil.isAdmin(this)) {
                    // 管理员可以编辑
                    showEditFeeDialog(fee)
                } else {
                    // 居民只能查看，可以缴纳
                    if (fee.status == "UNPAID") {
                        showPayFeeDialog(fee.id)
                    }
                }
            },
            onItemLongClick = { fee ->
                if (PermissionUtil.isAdmin(this)) {
                    showDeleteConfirmationDialog(fee.id)
                    true
                } else {
                    false
                }
            }
        )

        rvFeeRecords.layoutManager = LinearLayoutManager(this)
        rvFeeRecords.adapter = feeAdapter
    }

    private fun loadFeeData() {
        if (PermissionUtil.isAdmin(this)) {
            loadAllFees()
        } else {
            loadMyFees()
        }
    }

    private fun loadAllFees() {
        apiService.getAllFees().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    feeList.clear()
                    feeList.addAll(data)
                    feeAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@FeeManagementActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                Toast.makeText(this@FeeManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyFees() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(this)
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(this, "无法获取房号信息", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.getMyFees(houseNumber).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    feeList.clear()
                    feeList.addAll(data)
                    feeAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@FeeManagementActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                Toast.makeText(this@FeeManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddFeeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fee_add, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val etMonth = dialogView.findViewById<TextInputEditText>(R.id.et_month)

        AlertDialog.Builder(this)
            .setTitle("添加物业费")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val month = etMonth.text.toString().trim()

                if (validateFeeInput(houseNumber, residentName, amountStr, month)) {
                    val amount = amountStr.toDouble()
                    addFee(houseNumber, residentName, amount, month)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateFeeInput(houseNumber: String, residentName: String, amountStr: String, month: String): Boolean {
        return when {
            houseNumber.isEmpty() -> {
                Toast.makeText(this, "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            residentName.isEmpty() -> {
                Toast.makeText(this, "请输入住户姓名", Toast.LENGTH_SHORT).show()
                false
            }
            amountStr.isEmpty() -> {
                Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show()
                false
            }
            month.isEmpty() -> {
                Toast.makeText(this, "请输入月份", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addFee(houseNumber: String, residentName: String, amount: Double, month: String) {
        val request = FeeAddRequest(houseNumber, residentName, amount, month)

        apiService.addFee(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@FeeManagementActivity, "添加成功", Toast.LENGTH_SHORT).show()
                    loadFeeData()
                } else {
                    Toast.makeText(this@FeeManagementActivity, "添加失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@FeeManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditFeeDialog(fee: com.property.propertymanagement.network.FeeResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fee_add, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val etMonth = dialogView.findViewById<TextInputEditText>(R.id.et_month)

        // 填充现有数据
        etHouseNumber.setText(fee.houseNumber)
        etResidentName.setText(fee.residentName)
        etAmount.setText(fee.amount.toString())
        etMonth.setText(fee.month)

        AlertDialog.Builder(this)
            .setTitle("编辑物业费")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val month = etMonth.text.toString().trim()

                if (validateFeeInput(houseNumber, residentName, amountStr, month)) {
                    val amount = amountStr.toDouble()
                    updateFee(fee.id, houseNumber, residentName, amount, month)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateFee(feeId: Long, houseNumber: String, residentName: String, amount: Double, month: String) {
        // 注意：后端可能没有提供更新物业费的接口，这里需要调用删除再添加，或者提示用户
        Toast.makeText(this, "更新功能暂未实现，请先删除再重新添加", Toast.LENGTH_SHORT).show()

        // 如果有更新接口，可以这样调用：
        // val request = FeeUpdateRequest(feeId, houseNumber, residentName, amount, month)
        // apiService.updateFee(request).enqueue(...)
    }

    private fun showPayFeeDialog(feeId: Long) {
        AlertDialog.Builder(this)
            .setTitle("缴纳物业费")
            .setMessage("确定要缴纳这笔物业费吗？")
            .setPositiveButton("缴纳") { _, _ ->
                payFee(feeId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun payFee(feeId: Long) {
        apiService.payFee(PayRequest(feeId))
            .enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
                override fun onResponse(
                    call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                    response: Response<com.property.propertymanagement.network.ApiResult<Void>>
                ) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        Toast.makeText(this@FeeManagementActivity, "缴纳成功", Toast.LENGTH_SHORT).show()
                        loadFeeData()
                    } else {
                        Toast.makeText(this@FeeManagementActivity, "缴纳失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                    Toast.makeText(this@FeeManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDeleteConfirmationDialog(feeId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                // 注意：后端可能没有提供删除物业费的接口
                Toast.makeText(this, "删除功能暂未实现", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        findViewById<TextView>(R.id.tv_empty).visibility =
            if (feeList.isEmpty()) View.VISIBLE else View.GONE
    }
}