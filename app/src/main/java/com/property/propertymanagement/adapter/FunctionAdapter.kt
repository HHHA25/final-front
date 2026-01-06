package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R

class FunctionAdapter(
    private val items: List<FunctionItem>,
    private val pendingCount: Int, // 新增：待审批数量
    private val onItemClick: (FunctionItem) -> Unit
) : RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        val tvBadge: TextView = itemView.findViewById(R.id.tv_badge) // 角标
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_function_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivIcon.setImageResource(item.iconRes)
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.desc

        // 仅对"用户管理"卡片显示角标
        if (item.title == "用户管理" && pendingCount > 0) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = pendingCount.toString() // 显示数量
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}

// 功能项数据类
data class FunctionItem(
    val title: String,
    val desc: String,
    val iconRes: Int,
    val targetClass: Class<*>
)