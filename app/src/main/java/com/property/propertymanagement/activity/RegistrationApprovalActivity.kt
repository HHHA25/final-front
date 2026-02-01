package com.property.propertymanagement.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.RegistrationRequestAdapter
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationApprovalActivity : AppCompatActivity() {
    private lateinit var apiService: com.property.propertymanagement.network.ApiService
    private lateinit var rvRequests: RecyclerView
    private lateinit var requestAdapter: RegistrationRequestAdapter
    private lateinit var tvEmpty: TextView

    // 分开存储待审批和已处理请求
    private var pendingRequests = mutableListOf<com.property.propertymanagement.network.RegistrationResponse>()
    private var processedRequests = mutableListOf<com.property.propertymanagement.network.RegistrationResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PermissionUtil.isAdmin(this)) {
            Toast.makeText(this, "无权限访问", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_registration_approval)
        apiService = RetrofitClient.createApiService(this)

        initViews()

        // 默认加载待审批请求
        loadPendingRequests()
    }

    private fun initViews() {
        rvRequests = findViewById(R.id.rv_registration_requests)
        tvEmpty = findViewById(R.id.tv_empty)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        requestAdapter = RegistrationRequestAdapter(
            requests = mutableListOf(),
            onApproveClick = { request -> approveRequest(request.id) },
            onRejectClick = { request -> rejectRequest(request.id) }
        )
        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = requestAdapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showPendingRequests()
                    1 -> showProcessedRequests()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadPendingRequests() {
        apiService.getPendingRegistrations().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>,
                response: Response<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    pendingRequests.clear()
                    pendingRequests.addAll(response.body()?.data ?: emptyList())
                    showPendingRequests()
                } else {
                    Toast.makeText(this@RegistrationApprovalActivity, "加载待审批请求失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>, t: Throwable) {
                Toast.makeText(this@RegistrationApprovalActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProcessedRequests() {
        apiService.getProcessedRegistrations().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>,
                response: Response<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    processedRequests.clear()
                    processedRequests.addAll(response.body()?.data ?: emptyList())
                    showProcessedRequests()
                } else {
                    Toast.makeText(this@RegistrationApprovalActivity, "加载已处理请求失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.RegistrationResponse>>>, t: Throwable) {
                Toast.makeText(this@RegistrationApprovalActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPendingRequests() {
        requestAdapter.updateData(pendingRequests.map {
            com.property.propertymanagement.model.RegistrationRequest(
                id = it.id.toInt(),
                username = it.username,
                password = "", // 密码不显示
                houseNumber = it.houseNumber,
                status = when (it.status) {
                    "PENDING" -> com.property.propertymanagement.model.RequestStatus.PENDING
                    "APPROVED" -> com.property.propertymanagement.model.RequestStatus.APPROVED
                    "REJECTED" -> com.property.propertymanagement.model.RequestStatus.REJECTED
                    else -> com.property.propertymanagement.model.RequestStatus.PENDING
                },
                submitTime = it.submitTime
            )
        })
        updateEmptyView()
    }

    private fun showProcessedRequests() {
        // 如果已处理请求列表为空，则重新加载
        if (processedRequests.isEmpty()) {
            loadProcessedRequests()
        } else {
            requestAdapter.updateData(processedRequests.map {
                com.property.propertymanagement.model.RegistrationRequest(
                    id = it.id.toInt(),
                    username = it.username,
                    password = "",
                    houseNumber = it.houseNumber,
                    status = when (it.status) {
                        "PENDING" -> com.property.propertymanagement.model.RequestStatus.PENDING
                        "APPROVED" -> com.property.propertymanagement.model.RequestStatus.APPROVED
                        "REJECTED" -> com.property.propertymanagement.model.RequestStatus.REJECTED
                        else -> com.property.propertymanagement.model.RequestStatus.PENDING
                    },
                    submitTime = it.submitTime
                )
            })
            updateEmptyView()
        }
    }

    private fun approveRequest(requestId: Int) {
        AlertDialog.Builder(this)
            .setTitle("批准注册")
            .setMessage("确定要批准该用户的注册请求吗？")
            .setPositiveButton("批准") { _, _ ->
                apiService.approveRegistration(requestId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
                    override fun onResponse(
                        call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                        response: Response<com.property.propertymanagement.network.ApiResult<Void>>
                    ) {
                        if (response.isSuccessful && response.body()?.code == 200) {
                            Toast.makeText(this@RegistrationApprovalActivity, "批准成功", Toast.LENGTH_SHORT).show()
                            // 重新加载数据
                            loadPendingRequests()
                            // 如果当前显示的是已处理Tab，也重新加载已处理请求
                            val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                            if (tabLayout.selectedTabPosition == 1) {
                                loadProcessedRequests()
                            }
                        } else {
                            val errorMsg = response.body()?.msg ?: "批准失败"
                            Toast.makeText(this@RegistrationApprovalActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                        Toast.makeText(this@RegistrationApprovalActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun rejectRequest(requestId: Int) {
        AlertDialog.Builder(this)
            .setTitle("拒绝注册")
            .setMessage("确定要拒绝该用户的注册请求吗？")
            .setPositiveButton("拒绝") { _, _ ->
                apiService.rejectRegistration(requestId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
                    override fun onResponse(
                        call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                        response: Response<com.property.propertymanagement.network.ApiResult<Void>>
                    ) {
                        if (response.isSuccessful && response.body()?.code == 200) {
                            Toast.makeText(this@RegistrationApprovalActivity, "拒绝成功", Toast.LENGTH_SHORT).show()
                            // 重新加载数据
                            loadPendingRequests()
                            // 如果当前显示的是已处理Tab，也重新加载已处理请求
                            val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                            if (tabLayout.selectedTabPosition == 1) {
                                loadProcessedRequests()
                            }
                        } else {
                            val errorMsg = response.body()?.msg ?: "拒绝失败"
                            Toast.makeText(this@RegistrationApprovalActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                        Toast.makeText(this@RegistrationApprovalActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        tvEmpty.visibility = if (requestAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }
}