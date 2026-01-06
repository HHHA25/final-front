package com.property.propertymanagement.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.ParkingResponse
import com.property.propertymanagement.util.PermissionUtil

class ParkingAdapter(
    private val context: Context,
    private val parkings: List<ParkingResponse>,
    private val onItemClick: (ParkingResponse) -> Unit,
    private val onItemLongClick: (ParkingResponse) -> Boolean
) : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

    inner class ParkingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvParkingNumber: TextView = itemView.findViewById(R.id.tv_parking_number)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvResidentName: TextView = itemView.findViewById(R.id.tv_resident_name)
        val tvCarPlate: TextView = itemView.findViewById(R.id.tv_car_plate)
        val tvStartTime: TextView = itemView.findViewById(R.id.tv_start_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_parking, parent, false)
        return ParkingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkings[position]

        holder.tvParkingNumber.text = "车位号: ${parking.parkingNumber}"
        holder.tvHouseNumber.text = "房号: ${parking.houseNumber ?: "未分配"}"
        holder.tvResidentName.text = "住户: ${parking.residentName ?: "未分配"}"
        holder.tvCarPlate.text = "车牌号: ${parking.carPlate ?: "未分配"}"
        holder.tvStatus.text = parking.status
        holder.tvStartTime.text = "分配时间: ${parking.startTime ?: "未分配"}"

        // 设置状态颜色
        when (parking.status) {
            "ASSIGNED" -> holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
            "FREE" -> holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.status_bg)
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(parking)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(parking)
        }
    }

    override fun getItemCount(): Int = parkings.size
}