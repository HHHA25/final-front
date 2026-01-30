package com.property.propertymanagement.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.property.propertymanagement.R
import com.property.propertymanagement.adapter.ComplaintAdapter
import com.property.propertymanagement.network.ComplaintSubmitRequest
import com.property.propertymanagement.network.ComplaintUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComplaintFragment : Fragment() {
    private lateinit var rvComplaintRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var complaintAdapter: ComplaintAdapter
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvEmpty: TextView

    private var complaintList = mutableListOf<com.property.propertymanagement.network.ComplaintResponse>()
    private var allComplaintList = mutableListOf<com.property.propertymanagement.network.ComplaintResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var isLastPage = false
    private var currentSearchKeyword = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_complaint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.createApiService(requireContext())

        initViews(view)
        initRecyclerView()
        setupSearch()

        loadComplaintData(true)

        fabAdd.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun initViews(view: View) {
        rvComplaintRecords = view.findViewById(R.id.rv_complaint_records)
        fabAdd = view.findViewById(R.id.fab_add)
        etSearch = view.findViewById(R.id.et_search)
        ivClearSearch = view.findViewById(R.id.iv_clear_search)
        tvEmpty = view.findViewById(R.id.tv_empty)

        fabAdd.setOnClickListener { showAddComplaintDialog() }
    }

    private fun initRecyclerView() {
        complaintAdapter = ComplaintAdapter(complaintList,
            onItemClick = { complaint ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showUpdateComplaintDialog(complaint)
                }
            },
            onItemLongClick = { complaint ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showDeleteConfirmationDialog(complaint.id)
                    true
                } else {
                    false
                }
            }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvComplaintRecords.layoutManager = layoutManager
        rvComplaintRecords.adapter = complaintAdapter

        // 添加滚动监听实现分页加载
        rvComplaintRecords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                        loadMoreData()
                    }
                }
            }
        })
    }

    private fun setupSearch() {
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {
                currentSearchKeyword = s.toString().trim()
                if (currentSearchKeyword.isEmpty()) {
                    // 清空搜索，显示所有数据
                    complaintList.clear()
                    complaintList.addAll(allComplaintList)
                    complaintAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    // 执行搜索
                    performSearch(currentSearchKeyword)
                }
            }
        })

        // 清空搜索按钮点击
        ivClearSearch.setOnClickListener {
            etSearch.setText("")
            currentSearchKeyword = ""
            complaintList.clear()
            complaintList.addAll(allComplaintList)
            complaintAdapter.notifyDataSetChanged()
            updateEmptyView()
        }

        // 搜索按钮点击（软键盘上的搜索键）
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(currentSearchKeyword)
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(keyword: String) {
        if (keyword.isEmpty()) return

        if (PermissionUtil.isAdmin(requireContext())) {
            searchAllComplaints(keyword)
        } else {
            searchMyComplaints(keyword)
        }
    }

    fun loadComplaintData(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            isLastPage = false
            complaintList.clear()
            complaintAdapter.notifyDataSetChanged()
        }

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllComplaints()
        } else {
            loadMyComplaints()
        }
    }

    private fun loadMoreData() {
        if (isLoading || isLastPage) return

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllComplaints()
        } else {
            loadMyComplaints()
        }
    }

    private fun loadAllComplaints() {
        if (currentSearchKeyword.isNotEmpty()) {
            searchAllComplaints(currentSearchKeyword)
            return
        }

        apiService.getAllComplaints(currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        allComplaintList.clear()
                        complaintList.clear()
                        allComplaintList.addAll(data)
                    }

                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyComplaints() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        if (currentSearchKeyword.isNotEmpty()) {
            searchMyComplaints(currentSearchKeyword)
            return
        }

        apiService.getMyComplaints(houseNumber, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        allComplaintList.clear()
                        complaintList.clear()
                        allComplaintList.addAll(data)
                    }

                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchAllComplaints(keyword: String) {
        apiService.searchComplaints(keyword, keyword, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        complaintList.clear()
                    }

                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchMyComplaints(keyword: String) {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        apiService.searchMyComplaints(houseNumber, keyword, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        complaintList.clear()
                    }

                    complaintList.addAll(data)
                    complaintAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ComplaintPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
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

        if (!PermissionUtil.isAdmin(requireContext())) {
            val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
            val residentName = PermissionUtil.getCurrentUserName(requireContext())
            etHouseNumber.setText(houseNumber)
            etResidentName.setText(residentName)
            etHouseNumber.isEnabled = false
        }

        AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "请输入投诉类型", Toast.LENGTH_SHORT).show()
                false
            }
            content.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入投诉内容", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "提交成功", Toast.LENGTH_SHORT).show()
                    refreshData()
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

    private fun showUpdateComplaintDialog(complaint: com.property.propertymanagement.network.ComplaintResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complaint_update, null)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)
        val etHandleResult = dialogView.findViewById<TextInputEditText>(R.id.et_handle_result)

        etStatus.setText(complaint.status)
        etHandleResult.setText(complaint.handleResult ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("更新投诉状态")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val status = etStatus.text.toString().trim()
                val handleResult = etHandleResult.text.toString().trim()

                if (status.isNotEmpty()) {
                    updateComplaint(complaint.id, status, handleResult)
                } else {
                    Toast.makeText(requireContext(), "请输入状态", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                    refreshData()
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

    private fun showDeleteConfirmationDialog(complaintId: Long) {
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
        if (complaintList.isEmpty()) {
            if (currentSearchKeyword.isNotEmpty()) {
                tvEmpty.text = "未找到相关结果\n请尝试其他关键词"
            } else {
                tvEmpty.text = "暂无投诉记录\n点击下方按钮添加"
            }
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun refreshData() {
        etSearch.setText("")
        currentSearchKeyword = ""
        loadComplaintData(true)
    }
}