// RepairFragment.kt
package com.property.propertymanagement.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.RepairAdapter
import com.property.propertymanagement.network.RepairSubmitRequest
import com.property.propertymanagement.network.RepairUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepairFragment : Fragment() {
    private lateinit var rvRepairRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repairAdapter: RepairAdapter
    private var repairList = mutableListOf<com.property.propertymanagement.network.RepairResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_repair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.createApiService(requireContext())

        initViews(view)
        initRecyclerView()
        loadRepairData()

        fabAdd.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadRepairData()
    }

    private fun initViews(view: View) {
        rvRepairRecords = view.findViewById(R.id.rv_repair_records)
        fabAdd = view.findViewById(R.id.fab_add)
        fabAdd.setOnClickListener { showAddRepairDialog() }
    }

    private fun initRecyclerView() {
        repairAdapter = RepairAdapter(repairList,
            onItemClick = { repair ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showUpdateRepairDialog(repair)
                }
            },
            onItemLongClick = { repair ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showDeleteConfirmationDialog(repair.id)
                    true
                } else {
                    false
                }
            }
        )
        rvRepairRecords.layoutManager = LinearLayoutManager(requireContext())
        rvRepairRecords.adapter = repairAdapter
    }

    fun loadRepairData() {
        if (PermissionUtil.isAdmin(requireContext())) {
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
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyRepairs() {
        apiService.getMyRepairs().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>> {
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
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.RepairPageResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
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

        if (!PermissionUtil.isAdmin(requireContext())) {
            val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
            val residentName = PermissionUtil.getCurrentUserName(requireContext())
            etHouseNumber.setText(houseNumber)
            etResidentName.setText(residentName)
            etHouseNumber.isEnabled = false
        }

        AlertDialog.Builder(requireContext())
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

    private fun validateRepairInput(houseNumber: String, residentName: String, phone: String, type: String): Boolean {
        return when {
            houseNumber.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            residentName.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入住户姓名", Toast.LENGTH_SHORT).show()
                false
            }
            phone.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入联系电话", Toast.LENGTH_SHORT).show()
                false
            }
            type.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入维修类型", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun submitRepair(houseNumber: String, residentName: String, phone: String, type: String, description: String) {
        val userId = PermissionUtil.getCurrentUserId(requireContext())
        if (userId == 0L) {
            Toast.makeText(requireContext(), "无法获取用户信息", Toast.LENGTH_SHORT).show()
            return
        }

        val request = RepairSubmitRequest(userId, houseNumber, residentName, phone, type, description)

        apiService.submitRepair(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(requireContext(), "提交成功", Toast.LENGTH_SHORT).show()
                    loadRepairData()
                } else {
                    val errorMsg = response.body()?.msg ?: "提交失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUpdateRepairDialog(repair: com.property.propertymanagement.network.RepairResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_repair_update, null)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)
        val etFeedback = dialogView.findViewById<TextInputEditText>(R.id.et_feedback)

        etStatus.setText(repair.status)

        AlertDialog.Builder(requireContext())
            .setTitle("更新维修状态")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val status = etStatus.text.toString().trim()
                val feedback = etFeedback.text.toString().trim()

                if (status.isNotEmpty()) {
                    updateRepair(repair.id, status, feedback)
                } else {
                    Toast.makeText(requireContext(), "请输入状态", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                    loadRepairData()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteConfirmationDialog(repairId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                Toast.makeText(requireContext(), "删除功能暂未实现", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        view?.findViewById<TextView>(R.id.tv_empty)?.visibility =
            if (repairList.isEmpty()) View.VISIBLE else View.GONE
    }
}