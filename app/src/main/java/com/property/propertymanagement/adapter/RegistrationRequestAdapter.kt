package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.model.RegistrationRequest
import com.property.propertymanagement.model.RequestStatus

class RegistrationRequestAdapter(
    private var requests: List<RegistrationRequest>,
    private val onApproveClick: (RegistrationRequest) -> Unit,
    private val onRejectClick: (RegistrationRequest) -> Unit
) : RecyclerView.Adapter<RegistrationRequestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvSubmitTime: TextView = itemView.findViewById(R.id.tv_submit_time)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val btnApprove: Button = itemView.findViewById(R.id.btn_approve)
        val btnReject: Button = itemView.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_registration_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvUsername.text = "用户名: ${request.username}"
        holder.tvHouseNumber.text = "房号: ${request.houseNumber}"
        holder.tvSubmitTime.text = "提交时间: ${request.submitTime}"

        holder.tvStatus.text = when (request.status) {
            RequestStatus.PENDING -> "状态: 待审批"
            RequestStatus.APPROVED -> "状态: 已批准"
            RequestStatus.REJECTED -> "状态: 已拒绝"
        }

        // 只有待审批的请求才显示操作按钮
        if (request.status == RequestStatus.PENDING) {
            holder.btnApprove.visibility = View.VISIBLE
            holder.btnReject.visibility = View.VISIBLE
            holder.btnApprove.setOnClickListener { onApproveClick(request) }
            holder.btnReject.setOnClickListener { onRejectClick(request) }
        } else {
            holder.btnApprove.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
        }
    }

    override fun getItemCount() = requests.size

    // 更新列表数据
    fun updateData(newRequests: List<RegistrationRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}