package com.property.propertymanagement.activity

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R

// 功能项数据类
data class FunctionItem(
    val title: String,
    val desc: String,
    val iconRes: Int,
    val targetActivity: Class<*>
)

class FunctionAdapter(
    private val context: Context,
    private val items: List<FunctionItem>
) : RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {

    // 视图持有者
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
    }

    // 创建视图
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_function_card, parent, false)
        return ViewHolder(view)
    }

    // 绑定数据
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivIcon.setImageResource(item.iconRes)
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.desc

        // 点击卡片跳转
        holder.itemView.setOnClickListener {
            context.startActivity(Intent(context, item.targetActivity))
        }
    }

    // 条目数量
    override fun getItemCount() = items.size
}