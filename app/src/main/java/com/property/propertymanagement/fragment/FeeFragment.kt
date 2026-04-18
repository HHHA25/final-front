// 路径：com/property/propertymanagement/fragment/FeeFragment.kt
package com.property.propertymanagement.fragment

import android.content.Intent
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
import com.property.propertymanagement.activity.BatchAddFeeActivity
import com.property.propertymanagement.adapter.FeeAdapter
import com.property.propertymanagement.network.ApiResult
import com.property.propertymanagement.network.FeeAddRequest
import com.property.propertymanagement.network.FeeUpdateRequest
import com.property.propertymanagement.network.PayRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeeFragment : Fragment() {
    private lateinit var rvFeeRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var feeAdapter: FeeAdapter
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvEmpty: TextView

    private var feeList = mutableListOf<com.property.propertymanagement.network.FeeResponse>()
    private var allFeeList = mutableListOf<com.property.propertymanagement.network.FeeResponse>() // 保存所有数据用于搜索
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var isLastPage = false
    private var currentSearchKeyword = ""
    private var currentHouseNumber = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fee, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.createApiService(requireContext())

        initViews(view)
        initRecyclerView()
        setupSearch()

        loadFeeData(true) // 初次加载数据

        fabAdd.visibility = if (PermissionUtil.isAdmin(requireContext())) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun initViews(view: View) {
        rvFeeRecords = view.findViewById(R.id.rv_fee_records)
        fabAdd = view.findViewById(R.id.fab_add)
        etSearch = view.findViewById(R.id.et_search)
        ivClearSearch = view.findViewById(R.id.iv_clear_search)
        tvEmpty = view.findViewById(R.id.tv_empty)

        fabAdd.setOnClickListener { showAddOptionsDialog() }
    }

    /**
     * 显示添加选项：单个添加 / 批量添加
     */
    private fun showAddOptionsDialog() {
        val options = arrayOf("单个添加", "批量添加")
        AlertDialog.Builder(requireContext())
            .setTitle("选择添加方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddFeeDialog()            // 单个添加（原有）
                    1 -> navigateToBatchAddFee()       // 批量添加
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 跳转到批量添加页面
     */
    private fun navigateToBatchAddFee() {
        val intent = Intent(requireContext(), BatchAddFeeActivity::class.java)
        startActivity(intent)
    }

    private fun initRecyclerView() {
        feeAdapter = FeeAdapter(feeList,
            onItemClick = { fee ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showEditFeeDialog(fee)
                } else {
                    if (fee.status == "UNPAID") {
                        showPayFeeDialog(fee.id)
                    }
                }
            },
            onItemLongClick = { fee ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showDeleteConfirmationDialog(fee.id)
                    true
                } else {
                    false
                }
            }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvFeeRecords.layoutManager = layoutManager
        rvFeeRecords.adapter = feeAdapter

        // 添加滚动监听实现分页加载
        rvFeeRecords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                    feeList.clear()
                    feeList.addAll(allFeeList)
                    feeAdapter.notifyDataSetChanged()
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
            feeList.clear()
            feeList.addAll(allFeeList)
            feeAdapter.notifyDataSetChanged()
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
            searchAllFees(keyword)
        } else {
            searchMyFees(keyword)
        }
    }

    fun loadFeeData(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            isLastPage = false
            feeList.clear()
            feeAdapter.notifyDataSetChanged()
        }

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllFees()
        } else {
            loadMyFees()
        }
    }

    private fun loadMoreData() {
        if (isLoading || isLastPage) return

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllFees()
        } else {
            loadMyFees()
        }
    }

    private fun loadAllFees() {
        if (currentSearchKeyword.isNotEmpty()) {
            searchAllFees(currentSearchKeyword)
            return
        }

        apiService.getAllFees(currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    // 在 loadAllFees 的 onResponse 中
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0
                    val sortedData = data.sortedBy { it.status == "PAID" } // UNPAID (false) 在前，PAID (true) 在后
                    if (currentPage == 1) {
                        feeList.clear()
                        allFeeList.clear()
                        allFeeList.addAll(sortedData)
                    }
                    feeList.addAll(sortedData)
                    feeAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyFees() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        if (currentSearchKeyword.isNotEmpty()) {
            searchMyFees(currentSearchKeyword)
            return
        }

        apiService.getMyFees(houseNumber, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    // 在 loadMyFees 的 onResponse 中
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0
                    val sortedData = data.sortedBy { it.status == "PAID" }
                    if (currentPage == 1) {
                        feeList.clear()
                        allFeeList.clear()
                        allFeeList.addAll(sortedData)
                    }
                    feeList.addAll(sortedData)
                    feeAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchAllFees(keyword: String) {
        apiService.searchFees(
            keyword = keyword,
            houseNumber = "",
            currentPage, pageSize
        ).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        feeList.clear()
                    }

                    feeList.addAll(data)
                    feeAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchMyFees(keyword: String) {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        apiService.searchMyFees(houseNumber, keyword, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        feeList.clear()
                    }

                    feeList.addAll(data)
                    feeAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.FeePageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddFeeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fee_add, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val etMonth = dialogView.findViewById<TextInputEditText>(R.id.et_month)

        AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            residentName.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入住户姓名", Toast.LENGTH_SHORT).show()
                false
            }
            amountStr.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                false
            }
            month.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入月份", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditFeeDialog(fee: com.property.propertymanagement.network.FeeResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fee_add, null)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val etMonth = dialogView.findViewById<TextInputEditText>(R.id.et_month)

        etHouseNumber.setText(fee.houseNumber)
        etResidentName.setText(fee.residentName)
        etAmount.setText(fee.amount.toString())
        etMonth.setText(fee.month)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑物业费（请谨慎修改）")
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
        val request = FeeUpdateRequest(feeId, houseNumber, residentName, amount, month)
        apiService.updateFee(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(call: Call<ApiResult<Void>>, response: Response<ApiResult<Void>>) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "更新失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPayFeeDialog(feeId: Long) {
        AlertDialog.Builder(requireContext())
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
                        Toast.makeText(requireContext(), "缴纳成功", Toast.LENGTH_SHORT).show()
                        refreshData()
                    } else {
                        Toast.makeText(requireContext(), "缴纳失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDeleteConfirmationDialog(feeId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条物业费记录吗？")
            .setPositiveButton("删除") { _, _ ->
                apiService.deleteFee(feeId).enqueue(object : Callback<ApiResult<Void>> {
                    override fun onResponse(
                        call: Call<ApiResult<Void>>,
                        response: Response<ApiResult<Void>>
                    ) {
                        if (response.isSuccessful && response.body()?.code == 200) {
                            Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                            refreshData()
                        } else {
                            val errorMsg = response.body()?.msg ?: "删除失败"
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                        Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyView() {
        if (feeList.isEmpty()) {
            if (currentSearchKeyword.isNotEmpty()) {
                tvEmpty.text = "未找到相关结果\n请尝试其他关键词"
            } else {
                tvEmpty.text = "暂无物业费记录\n点击下方按钮添加"
            }
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun refreshData() {
        etSearch.setText("")
        currentSearchKeyword = ""
        loadFeeData(true)
    }
}