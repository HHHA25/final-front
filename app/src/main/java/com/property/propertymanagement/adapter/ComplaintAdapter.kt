package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.ComplaintResponse

class ComplaintAdapter(
    private val complaints: List<ComplaintResponse>,
    private val onItemClick: (ComplaintResponse) -> Unit,
    private val onItemLongClick: (ComplaintResponse) -> Boolean
) : RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder>() {

    inner class ComplaintViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvResidentName: TextView = itemView.findViewById(R.id.tv_resident_name)
        val tvPhone: TextView = itemView.findViewById(R.id.tv_phone)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvSubmitTime: TextView = itemView.findViewById(R.id.tv_submit_time)
        val tvHandleResult: TextView = itemView.findViewById(R.id.tv_handle_result)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_complaint, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val complaint = complaints[position]

        holder.tvHouseNumber.text = "房号: ${complaint.houseNumber}"
        holder.tvType.text = "类型: ${complaint.type}"
        holder.tvResidentName.text = "住户: ${complaint.residentName}"
        holder.tvPhone.text = "电话: ${complaint.phone}"
        holder.tvContent.text = "投诉内容: ${complaint.content}"
        holder.tvStatus.text = complaint.status
        holder.tvSubmitTime.text = "提交: ${complaint.submitTime}"

        // 设置状态颜色
        when (complaint.status) {
            "RESOLVED" -> holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
            "PROCESSING" -> holder.tvStatus.setBackgroundResource(R.drawable.status_bg)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
        }

        // 显示处理结果（如果有）
        complaint.handleResult?.let {
            holder.tvHandleResult.text = "处理结果: $it"
            holder.tvHandleResult.visibility = View.VISIBLE
        } ?: run {
            holder.tvHandleResult.visibility = View.GONE
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(complaint)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(complaint)
        }
    }

    override fun getItemCount(): Int = complaints.size
}