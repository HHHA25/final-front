package com.property.propertymanagement.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar  // 导入 Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.property.propertymanagement.R
import com.property.propertymanagement.network.ApiResult
import com.property.propertymanagement.network.ApiService
import com.property.propertymanagement.network.BatchFeeAddRequest
import com.property.propertymanagement.network.HouseResponse
import com.property.propertymanagement.network.HousePageResponse
import com.property.propertymanagement.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BatchAddFeeActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar  // 声明 Toolbar
    private lateinit var etMonth: TextInputEditText
    private lateinit var rvHouses: RecyclerView
    private lateinit var btnSubmit: Button
    private lateinit var tvEmpty: TextView
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeselectAll: Button

    private lateinit var apiService: ApiService
    private lateinit var houseAdapter: HouseCheckAdapter
    private var allHouses = mutableListOf<HouseResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_add_fee)

        // 初始化视图
        toolbar = findViewById(R.id.toolbar)
        etMonth = findViewById(R.id.et_month)
        rvHouses = findViewById(R.id.rv_houses)
        btnSubmit = findViewById(R.id.btn_submit)
        tvEmpty = findViewById(R.id.tv_empty)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnDeselectAll = findViewById(R.id.btn_deselect_all)

        // 设置 Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        apiService = RetrofitClient.createApiService(this)

        setupRecyclerView()
        setupClickListeners()
        loadHouses()
    }

    private fun setupRecyclerView() {
        rvHouses.layoutManager = LinearLayoutManager(this)
        houseAdapter = HouseCheckAdapter(allHouses) { house, isChecked ->
            // 可选：记录选中状态变化
        }
        rvHouses.adapter = houseAdapter
    }

    private fun setupClickListeners() {
        btnSubmit.setOnClickListener { submitBatchAdd() }

        btnSelectAll.setOnClickListener {
            houseAdapter.selectAll()
        }

        btnDeselectAll.setOnClickListener {
            houseAdapter.deselectAll()
        }
    }

    private fun loadHouses() {
        apiService.getAllHouses().enqueue(object : Callback<ApiResult<HousePageResponse>> {
            override fun onResponse(
                call: Call<ApiResult<HousePageResponse>>,
                response: Response<ApiResult<HousePageResponse>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data?.records ?: emptyList()
                    allHouses.clear()
                    allHouses.addAll(data)
                    allHouses.sortWith(compareBy { it.houseNumber.naturalOrder() })
                    houseAdapter.updateData(allHouses)
                    updateEmptyView()
                } else {
                    Toast.makeText(this@BatchAddFeeActivity, "加载房屋列表失败", Toast.LENGTH_SHORT).show()
                }
            }


            override fun onFailure(call: Call<ApiResult<HousePageResponse>>, t: Throwable) {
                Toast.makeText(this@BatchAddFeeActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 在 BatchAddFeeActivity.kt 文件末尾添加
    private fun String.naturalOrder(): ComparablePair<String, Int> {
        // 匹配字母前缀和数字部分，例如 "A101" -> ("A", 101)
        val prefix = this.takeWhile { it.isLetter() }
        val numberStr = this.dropWhile { it.isLetter() }
        val number = numberStr.toIntOrNull() ?: 0
        return ComparablePair(prefix, number)
    }

    private data class ComparablePair<A : Comparable<A>, B : Comparable<B>>(
        val first: A,
        val second: B
    ) : Comparable<ComparablePair<A, B>> {
        override fun compareTo(other: ComparablePair<A, B>): Int {
            val firstCompare = first.compareTo(other.first)
            return if (firstCompare != 0) firstCompare else second.compareTo(other.second)
        }
    }

    private fun submitBatchAdd() {
        val month = etMonth.text.toString().trim()
        if (month.isEmpty()) {
            Toast.makeText(this, "请输入月份", Toast.LENGTH_SHORT).show()
            return
        }
        if (!month.matches(Regex("\\d{4}-\\d{2}"))) {
            Toast.makeText(this, "月份格式应为 yyyy-MM", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedHouseNumbers = houseAdapter.getSelectedHouseNumbers()
        if (selectedHouseNumbers.isEmpty()) {
            Toast.makeText(this, "请至少选择一个房号", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("批量添加")
            .setMessage("确定为选中的 ${selectedHouseNumbers.size} 户添加 $month 物业费吗？")
            .setPositiveButton("确定") { _, _ ->
                doBatchAdd(month, selectedHouseNumbers)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doBatchAdd(month: String, houseNumbers: List<String>) {
        val request = BatchFeeAddRequest(month, houseNumbers)
        apiService.batchAddFees(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(
                call: Call<ApiResult<Void>>,
                response: Response<ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@BatchAddFeeActivity, "批量添加成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.body()?.msg ?: "添加失败"
                    Toast.makeText(this@BatchAddFeeActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@BatchAddFeeActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateEmptyView() {
        tvEmpty.visibility = if (allHouses.isEmpty()) View.VISIBLE else View.GONE
        rvHouses.visibility = if (allHouses.isEmpty()) View.GONE else View.VISIBLE
    }

    // 处理返回按钮点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 房屋列表适配器（带复选框）
     */
    inner class HouseCheckAdapter(
        private var houses: List<HouseResponse>,
        private val onCheckedChange: (HouseResponse, Boolean) -> Unit
    ) : RecyclerView.Adapter<HouseCheckAdapter.ViewHolder>() {

        private val checkedMap = mutableMapOf<Long, Boolean>()

        fun updateData(newHouses: List<HouseResponse>) {
            houses = newHouses
            checkedMap.clear()
            houses.forEach { checkedMap[it.id] = true } // 默认全选
            notifyDataSetChanged()
        }

        fun selectAll() {
            houses.forEach { checkedMap[it.id] = true }
            notifyDataSetChanged()
        }

        fun deselectAll() {
            houses.forEach { checkedMap[it.id] = false }
            notifyDataSetChanged()
        }

        fun getSelectedHouseNumbers(): List<String> {
            return houses.filter { checkedMap[it.id] == true }
                .map { it.houseNumber }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cbSelect: android.widget.CheckBox = itemView.findViewById(R.id.cb_select)
            val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
            val tvResidentName: TextView = itemView.findViewById(R.id.tv_resident_name)
            val tvOwnerName: TextView = itemView.findViewById(R.id.tv_owner_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_house_check, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val house = houses[position]
            holder.tvHouseNumber.text = "房号: ${house.houseNumber}"
            holder.tvResidentName.text = "住户: ${house.residentName ?: "无"}"
            holder.tvOwnerName.text = "业主: ${house.ownerName ?: "无"}"

            holder.cbSelect.isChecked = checkedMap[house.id] ?: true
            holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                checkedMap[house.id] = isChecked
                onCheckedChange(house, isChecked)
            }
        }

        override fun getItemCount() = houses.size
    }
}