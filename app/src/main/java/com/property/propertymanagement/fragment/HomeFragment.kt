package com.property.propertymanagement.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.activity.*
import com.property.propertymanagement.util.PermissionUtil
import com.property.propertymanagement.MainActivity

class HomeFragment : Fragment() {

    private lateinit var rvFunctions: RecyclerView
    private var pendingCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFunctions = view.findViewById(R.id.rv_functions)
        rvFunctions.layoutManager = GridLayoutManager(requireContext(), 2)

        loadFunctionCards()
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtil.isAdmin(requireContext())) {
            // 加载待审批数量
            loadPendingCount()
        }
    }

    private fun loadFunctionCards() {
        val functionItems = createFunctionItems()
        rvFunctions.adapter = FunctionAdapter(requireContext(), functionItems, pendingCount) { item ->
            startActivity(android.content.Intent(requireContext(), item.targetClass))
        }
    }

    private fun loadPendingCount() {
        // 这里调用API获取待审批数量
        // 暂时使用模拟数据
        pendingCount = 3
        updateBadge()
    }

    fun updatePendingCount(count: Int) {
        pendingCount = count
        updateBadge()
    }

    private fun updateBadge() {
        val adapter = rvFunctions.adapter as? FunctionAdapter
        adapter?.updatePendingCount(pendingCount)
    }

    fun refreshData() {
        loadPendingCount()
        loadFunctionCards()
    }

    private fun createFunctionItems(): List<FunctionItem> {
        val items = mutableListOf<FunctionItem>()
        items.add(FunctionItem("楼栋管理", "管理小区楼栋和房屋信息", R.drawable.ic_building, BuildingManagementActivity::class.java))
        items.add(FunctionItem("管理费管理", "记录与管理小区管理费", R.drawable.ic_fee, FeeManagementActivity::class.java))
        items.add(FunctionItem("车位管理", "管理小区车位分配与使用", R.drawable.ic_parking, ParkingActivity::class.java))
        items.add(FunctionItem("维修管理", "处理业主维修申请与记录", R.drawable.ic_repair, RepairActivity::class.java))
        items.add(FunctionItem("投诉管理", "记录与跟进业主投诉", R.drawable.ic_complaint, ComplaintActivity::class.java))

        if (PermissionUtil.isAdmin(requireContext())) {
            items.add(FunctionItem("用户管理", "管理用户和注册审批", R.drawable.ic_user, UserManagementActivity::class.java))
        }

        return items
    }

    // 数据类和适配器（从原来的MainActivity复制）
    data class FunctionItem(
        val title: String,
        val desc: String,
        val iconRes: Int,
        val targetClass: Class<*>
    )

    class FunctionAdapter(
        private val context: android.content.Context,
        private val items: List<FunctionItem>,
        private var pendingCount: Int,
        private val onItemClick: (FunctionItem) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val ivIcon: android.widget.ImageView = itemView.findViewById(R.id.iv_icon)
            val tvTitle: android.widget.TextView = itemView.findViewById(R.id.tv_title)
            val tvDesc: android.widget.TextView = itemView.findViewById(R.id.tv_desc)
            val tvBadge: android.widget.TextView = itemView.findViewById(R.id.tv_badge)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_function_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageResource(item.iconRes)
            holder.tvTitle.text = item.title
            holder.tvDesc.text = item.desc

            // 显示待审批数量角标
            if (item.title == "用户管理" && pendingCount > 0) {
                holder.tvBadge.visibility = View.VISIBLE
                holder.tvBadge.text = if (pendingCount > 99) "99+" else pendingCount.toString()
            } else {
                holder.tvBadge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size

        fun updatePendingCount(count: Int) {
            pendingCount = count
            notifyDataSetChanged()
        }
    }
}