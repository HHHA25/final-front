package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.FeeResponse

class FeeAdapter(
    private val fees: List<FeeResponse>,
    private val onItemClick: (FeeResponse) -> Unit,
    private val onItemLongClick: (FeeResponse) -> Boolean
) : RecyclerView.Adapter<FeeAdapter.FeeViewHolder>() {

    inner class FeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvMonth: TextView = itemView.findViewById(R.id.tv_month)
        val tvResidentName: TextView = itemView.findViewById(R.id.tv_resident_name)
        val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tv_payment_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fee, parent, false)
        return FeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeeViewHolder, position: Int) {
        val fee = fees[position]

        holder.tvHouseNumber.text = "房号: ${fee.houseNumber}"
        holder.tvMonth.text = fee.month
        holder.tvResidentName.text = "住户: ${fee.residentName}"
        holder.tvAmount.text = "¥${fee.amount}"
        holder.tvStatus.text = fee.status

        // 设置状态颜色
        if (fee.status == "PAID") {
            holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
        }

        // 显示缴费日期（如果有）
        fee.paymentDate?.let {
            holder.tvPaymentDate.text = "缴费日期: $it"
            holder.tvPaymentDate.visibility = View.VISIBLE
        } ?: run {
            holder.tvPaymentDate.visibility = View.GONE
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(fee)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(fee)
        }
    }

    override fun getItemCount(): Int = fees.size
}