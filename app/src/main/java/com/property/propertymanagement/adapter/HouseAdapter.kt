package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.HouseResponse

class HouseAdapter(
    private val houses: List<HouseResponse>,
    private val onItemClick: (HouseResponse) -> Unit,
    private val onItemLongClick: (HouseResponse) -> Boolean
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    inner class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tv_house_number)
        val tvBuilding: TextView = itemView.findViewById(R.id.tv_building)
        val tvFloor: TextView = itemView.findViewById(R.id.tv_floor)
        val tvOwner: TextView = itemView.findViewById(R.id.tv_owner)
        val tvResident: TextView = itemView.findViewById(R.id.tv_resident)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvArea: TextView = itemView.findViewById(R.id.tv_area)
        val tvUnitType: TextView = itemView.findViewById(R.id.tv_unit_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_house, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houses[position]

        holder.tvHouseNumber.text = "房号: ${house.houseNumber}"
        holder.tvBuilding.text = "楼栋: ${house.buildingName ?: house.buildingNumber}"
        holder.tvFloor.text = "楼层: ${house.floor}F"
        holder.tvOwner.text = "业主: ${house.ownerName ?: "未登记"}"
        holder.tvResident.text = "住户: ${house.residentName ?: "暂无"}"
        holder.tvArea.text = "面积: ${house.area ?: "未设置"}㎡"
        holder.tvUnitType.text = "户型: ${house.unitType ?: "未设置"}"
        holder.tvStatus.text = when (house.houseStatus) {
            "OCCUPIED" -> "已入住"
            "VACANT" -> "空置"
            "RENTED" -> "出租"
            else -> house.houseStatus
        }

        // 设置状态颜色
        when (house.houseStatus) {
            "OCCUPIED" -> holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
            "VACANT" -> holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
            "RENTED" -> holder.tvStatus.setBackgroundResource(R.drawable.status_bg)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(house)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(house)
        }
    }

    override fun getItemCount(): Int = houses.size
}