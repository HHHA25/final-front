// ParkingFragment.kt
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
    private var parkingList = mutableListOf<com.property.propertymanagement.network.ParkingResponse>()
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

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
        loadParkingData()

        fabAdd.visibility = if (PermissionUtil.isAdmin(requireContext())) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadParkingData()
    }

    private fun initViews(view: View) {
        rvParkingRecords = view.findViewById(R.id.rv_parking_records)
        fabAdd = view.findViewById(R.id.fab_add)
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
        rvParkingRecords.layoutManager = LinearLayoutManager(requireContext())
        rvParkingRecords.adapter = parkingAdapter
    }

    fun loadParkingData() {
        if (PermissionUtil.isAdmin(requireContext())) {
            loadAllParkings()
        } else {
            loadMyParkings()
        }
    }

    private fun loadAllParkings() {
        apiService.getAllParkings().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    parkingList.clear()
                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMyParkings() {
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext())
        if (houseNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "无法获取房号信息", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.getMyParkings(houseNumber).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>,
                response: Response<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    parkingList.clear()
                    parkingList.addAll(data)
                    parkingAdapter.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<com.property.propertymanagement.network.ParkingPageResponse>>, t: Throwable) {
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
                    loadParkingData()
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
                    loadParkingData()
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
                    loadParkingData()
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
        view?.findViewById<TextView>(R.id.tv_empty)?.visibility =
            if (parkingList.isEmpty()) View.VISIBLE else View.GONE
    }
}