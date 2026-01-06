package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.RepairResponse

class RepairAdapter(
    private val repairs: List<RepairResponse>,
    private val onItemClick: (RepairResponse) -> Unit,
    private val onItemLongClick: (RepairResponse) -> Boolean
) : RecyclerView.Adapter<RepairAdapter.RepairViewHolder>() {

    inner class RepairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvResidentName: TextView = itemView.findViewById(R.id.tv_resident_name)
        val tvPhone: TextView = itemView.findViewById(R.id.tv_phone)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvSubmitTime: TextView = itemView.findViewById(R.id.tv_submit_time)
        val tvHandleTime: TextView = itemView.findViewById(R.id.tv_handle_time) // 注意：这里使用handleTime而不是completeTime
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repair, parent, false)
        return RepairViewHolder(view)
    }

    override fun onBindViewHolder(holder: RepairViewHolder, position: Int) {
        val repair = repairs[position]

        holder.tvHouseNumber.text = "房号: ${repair.houseNumber}"
        holder.tvType.text = "类型: ${repair.type}"
        holder.tvResidentName.text = "住户: ${repair.residentName}"
        holder.tvPhone.text = "电话: ${repair.phone}"
        holder.tvDescription.text = "问题描述: ${repair.description}"
        holder.tvStatus.text = repair.status
        holder.tvSubmitTime.text = "提交: ${repair.submitTime}"

        // 设置状态颜色
        when (repair.status) {
            "COMPLETED" -> holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
            "PROCESSING" -> holder.tvStatus.setBackgroundResource(R.drawable.status_bg)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
        }

        // 显示处理时间（如果有）
        repair.handleTime?.let {
            holder.tvHandleTime.text = "处理时间: $it"
            holder.tvHandleTime.visibility = View.VISIBLE
        } ?: run {
            holder.tvHandleTime.visibility = View.GONE
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(repair)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(repair)
        }
    }

    override fun getItemCount(): Int = repairs.size
}