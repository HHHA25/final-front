package com.property.propertymanagement.util

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.property.propertymanagement.R
import com.property.propertymanagement.model.Parking
import com.google.android.material.textfield.TextInputEditText

object ParkingDialogUtil {
    fun showParkingDialog(
        activity: Activity,
        existingParking: Parking? = null,
        onSaveClicked: (Parking) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_parking)

        // 获取控件
        val etParkingNumber = dialog.findViewById<TextInputEditText>(R.id.et_parking_number)
        val etHouseNumber = dialog.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialog.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etCarPlate = dialog.findViewById<TextInputEditText>(R.id.et_car_plate)
        val etStatus = dialog.findViewById<TextInputEditText>(R.id.et_status)
        val etStartTime = dialog.findViewById<TextInputEditText>(R.id.et_start_time)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save)

        // 如果是编辑模式，填充已有数据
        existingParking?.let {
            etParkingNumber.setText(it.parkingNumber)
            etHouseNumber.setText(it.houseNumber)
            etResidentName.setText(it.residentName)
            etCarPlate.setText(it.carPlate)
            etStatus.setText(it.status)
            etStartTime.setText(it.startTime)
        }

        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 保存按钮
        btnSave.setOnClickListener {
            val parkingNumber = etParkingNumber.text.toString().trim()
            val houseNumber = etHouseNumber.text.toString().trim()
            val residentName = etResidentName.text.toString().trim()
            val carPlate = etCarPlate.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val startTime = etStartTime.text.toString().trim()

            // 验证输入
            if (parkingNumber.isEmpty() || houseNumber.isEmpty() ||
                residentName.isEmpty() || carPlate.isEmpty() ||
                status.isEmpty() || startTime.isEmpty()) {
                Toast.makeText(activity, "请填写所有字段", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 创建或更新车位对象
            val parking = existingParking?.copy(
                parkingNumber = parkingNumber,
                houseNumber = houseNumber,
                residentName = residentName,
                carPlate = carPlate,
                status = status,
                startTime = startTime
            ) ?: Parking(
                id = 0, // 新增时ID为0，数据库会自动生成
                parkingNumber = parkingNumber,
                houseNumber = houseNumber,
                residentName = residentName,
                carPlate = carPlate,
                status = status,
                startTime = startTime
            )

            onSaveClicked(parking)
            dialog.dismiss()
        }

        // 设置对话框宽度为屏幕宽度的80%
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.8).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }
}
