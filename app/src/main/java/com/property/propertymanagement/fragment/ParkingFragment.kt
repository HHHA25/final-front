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
import com.property.propertymanagement.adapter.ParkingAdapter
import com.property.propertymanagement.network.ParkingAddRequest
import com.property.propertymanagement.network.ParkingUpdateRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ParkingFragment : Fragment() {
    private lateinit var rvParkingRecords: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var parkingAdapter: ParkingAdapter
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvEmpty: TextView

    private var parkingList = mutableListOf<com.property.propertymanagement.network.ParkingResponse>()
    private var allParkingList = mutableListOf<com.property.propertymanagement.network.ParkingResponse>()
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
        return inflater.inflate(R.layout.fragment_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.createApiService(requireContext())

        initViews(view)
        initRecyclerView()
        setupSearch()

        loadParkingData(true)

        fabAdd.visibility = if (PermissionUtil.isAdmin(requireContext())) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun initViews(view: View) {
        rvParkingRecords = view.findViewById(R.id.rv_parking_records)
        fabAdd = view.findViewById(R.id.fab_add)
        etSearch = view.findViewById(R.id.et_search)
        ivClearSearch = view.findViewById(R.id.iv_clear_search)
        tvEmpty = view.findViewById(R.id.tv_empty)

        fabAdd.setOnClickListener { showAddParkingDialog() }
    }

    private fun initRecyclerView() {
        parkingAdapter = ParkingAdapter(requireContext(), parkingList,
            onItemClick = { parking ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showEditParkingDialog(parking)
                }
            },
            onItemLongClick = { parking ->
                if (PermissionUtil.isAdmin(requireContext())) {
                    showDeleteConfirmationDialog(parking.id)
                    true
                } else {
                    false
                }
            }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvParkingRecords.layoutManager = layoutManager
        rvParkingRecords.adapter = parkingAdapter

        // 添加滚动监听实现分页加载
        rvParkingRecords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                    parkingList.clear()
                    parkingList.addAll(allParkingList)
                    parkingAdapter.notifyDataSetChanged()
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
            parkingList.clear()
            parkingList.addAll(allParkingList)
            parkingAdapter.notifyDataSetChanged()
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
            searchAllParkings(keyword)
        } else {
            searchMyParkings(keyword)
        }
    }

    fun loadParkingData(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            isLastPage = false
            parkingList.clear()
            parkingAdapter.notifyDataSetChanged()
        }

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllParkings()
        } else {
            loadMyParkings()
        }
    }

    private fun loadMoreData() {
        if (isLoading || isLastPage) return

        isLoading = true
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllParkings()
        } else {
            loadMyParkings()
        }
    }

    private fun loadAllParkings() {
        if (currentSearchKeyword.isNotEmpty()) {
            searchAllParkings(currentSearchKeyword)
            return
        }

        apiService.getAllParkings(currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        allParkingList.clear()
                        parkingList.clear()
                        allParkingList.addAll(data)
                    }

                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyParkings() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        if (currentSearchKeyword.isNotEmpty()) {
            searchMyParkings(currentSearchKeyword)
            return
        }

        apiService.getMyParkings(houseNumber, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        allParkingList.clear()
                        parkingList.clear()
                        allParkingList.addAll(data)
                    }

                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchAllParkings(keyword: String) {
        apiService.searchParkings(
            keyword = keyword,
            houseNumber = "",
            currentPage, pageSize
        ).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        parkingList.clear()
                    }

                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchMyParkings(keyword: String) {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            isLoading = false
            return
        }

        apiService.searchMyParkings(houseNumber, keyword, currentPage, pageSize).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    val totalPages = response.body()?.data?.pages ?: 0

                    if (currentPage == 1) {
                        parkingList.clear()
                    }

                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= totalPages
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddParkingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_parking, null)
        val etParkingNumber = dialogView.findViewById<TextInputEditText>(R.id.et_parking_number)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etCarPlate = dialogView.findViewById<TextInputEditText>(R.id.et_car_plate)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)

        AlertDialog.Builder(requireContext())
            .setTitle("添加车位")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val parkingNumber = etParkingNumber.text.toString().trim()
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val carPlate = etCarPlate.text.toString().trim()
                val status = etStatus.text.toString().trim()

                if (validateParkingInput(parkingNumber, houseNumber, residentName, carPlate, status)) {
                    addParking(parkingNumber, houseNumber, residentName, carPlate, status)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateParkingInput(
        parkingNumber: String,
        houseNumber: String,
        residentName: String,
        carPlate: String,
        status: String
    ): Boolean {
        return when {
            parkingNumber.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入车位号", Toast.LENGTH_SHORT).show()
                false
            }
            houseNumber.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入房号", Toast.LENGTH_SHORT).show()
                false
            }
            residentName.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入住户姓名", Toast.LENGTH_SHORT).show()
                false
            }
            carPlate.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入车牌号", Toast.LENGTH_SHORT).show()
                false
            }
            status.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入状态", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addParking(parkingNumber: String, houseNumber: String, residentName: String, carPlate: String, status: String) {
        val request = ParkingAddRequest(parkingNumber, houseNumber, residentName, carPlate, status)

        apiService.addParking(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "添加失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditParkingDialog(parking: com.property.propertymanagement.network.ParkingResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_parking, null)
        val etParkingNumber = dialogView.findViewById<TextInputEditText>(R.id.et_parking_number)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialogView.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etCarPlate = dialogView.findViewById<TextInputEditText>(R.id.et_car_plate)
        val etStatus = dialogView.findViewById<TextInputEditText>(R.id.et_status)

        etParkingNumber.setText(parking.parkingNumber)
        etHouseNumber.setText(parking.houseNumber)
        etResidentName.setText(parking.residentName)
        etCarPlate.setText(parking.carPlate)
        etStatus.setText(parking.status)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑车位")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val parkingNumber = etParkingNumber.text.toString().trim()
                val houseNumber = etHouseNumber.text.toString().trim()
                val residentName = etResidentName.text.toString().trim()
                val carPlate = etCarPlate.text.toString().trim()
                val status = etStatus.text.toString().trim()

                if (validateParkingInput(parkingNumber, houseNumber, residentName, carPlate, status)) {
                    updateParking(parking.id, parkingNumber, houseNumber, residentName, carPlate, status)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateParking(parkingId: Long, parkingNumber: String, houseNumber: String, residentName: String, carPlate: String, status: String) {
        val request = ParkingUpdateRequest(parkingId, parkingNumber, houseNumber, residentName, carPlate, status)

        apiService.updateParking(request).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
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

    private fun showDeleteConfirmationDialog(parkingId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteParking(parkingId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteParking(parkingId: Long) {
        apiService.deleteParking(parkingId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                    refreshData()
                } else {
                    val errorMsg = response.body()?.msg ?: "删除失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateEmptyView() {
        if (parkingList.isEmpty()) {
            if (currentSearchKeyword.isNotEmpty()) {
                tvEmpty.text = "未找到相关结果\n请尝试其他关键词"
            } else {
                tvEmpty.text = "暂无车位记录\n点击下方按钮添加"
            }
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun refreshData() {
        etSearch.setText("")
        currentSearchKeyword = ""
        loadParkingData(true)
    }
}