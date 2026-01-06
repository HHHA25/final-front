package com.property.propertymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.BuildingResponse

class BuildingAdapter(
    private val buildings: List<BuildingResponse>,
    private val onItemClick: (BuildingResponse) -> Unit,
    private val onItemLongClick: (BuildingResponse) -> Boolean
) : RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {

    inner class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBuildingNumber: TextView = itemView.findViewById(R.id.tv_building_number)
        val tvBuildingName: TextView = itemView.findViewById(R.id.tv_building_name)
        val tvFloors: TextView = itemView.findViewById(R.id.tv_floors)
        val tvUnits: TextView = itemView.findViewById(R.id.tv_units)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_building, parent, false)
        return BuildingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val building = buildings[position]

        holder.tvBuildingNumber.text = "楼栋编号: ${building.buildingNumber}"
        holder.tvBuildingName.text = building.buildingName
        holder.tvFloors.text = "楼层: ${building.totalFloors}"
        holder.tvUnits.text = "户数: ${building.totalUnits}"
        holder.tvStatus.text = if (building.status == "ACTIVE") "启用" else "停用"
        holder.tvType.text = "类型: ${building.buildingType ?: "未设置"}"

        // 设置状态颜色
        if (building.status == "ACTIVE") {
            holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg)
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(building)
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(building)
        }
    }

    override fun getItemCount(): Int = buildings.size
}