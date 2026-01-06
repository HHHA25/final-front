package com.property.propertymanagement.util

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.property.propertymanagement.R
import com.property.propertymanagement.model.Complaint
import com.google.android.material.textfield.TextInputEditText

object ComplaintDialogUtil {
    fun showComplaintDialog(
        activity: Activity,
        existingComplaint: Complaint? = null,
        onSaveClicked: (Complaint) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_complaint)

        // 获取控件
        val etHouseNumber = dialog.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialog.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etPhone = dialog.findViewById<TextInputEditText>(R.id.et_phone)
        val etType = dialog.findViewById<TextInputEditText>(R.id.et_type)
        val etContent = dialog.findViewById<TextInputEditText>(R.id.et_content)
        val etStatus = dialog.findViewById<TextInputEditText>(R.id.et_status)
        val etSubmitTime = dialog.findViewById<TextInputEditText>(R.id.et_submit_time)
        val etHandleResult = dialog.findViewById<TextInputEditText>(R.id.et_handle_result)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save)

        // 如果是编辑模式，填充已有数据
        existingComplaint?.let {
            etHouseNumber.setText(it.houseNumber)
            etResidentName.setText(it.residentName)
            etPhone.setText(it.phone)
            etType.setText(it.type)
            etContent.setText(it.content)
            etStatus.setText(it.status)
            etSubmitTime.setText(it.submitTime)
            etHandleResult.setText(it.handleResult)
        }

        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 保存按钮
        btnSave.setOnClickListener {
            val houseNumber = etHouseNumber.text.toString().trim()
            val residentName = etResidentName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val type = etType.text.toString().trim()
            val content = etContent.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val submitTime = etSubmitTime.text.toString().trim()
            val handleResult = etHandleResult.text.toString().trim().ifEmpty { null }

            // 验证输入
            if (houseNumber.isEmpty() || residentName.isEmpty() ||
                phone.isEmpty() || type.isEmpty() ||
                content.isEmpty() || status.isEmpty() || submitTime.isEmpty()) {
                Toast.makeText(activity, "请填写必填字段", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 创建或更新投诉对象
            val complaint = existingComplaint?.copy(
                houseNumber = houseNumber,
                residentName = residentName,
                phone = phone,
                type = type,
                content = content,
                status = status,
                submitTime = submitTime,
                handleResult = handleResult
            ) ?: Complaint(
                id = 0, // 新增时ID为0，数据库会自动生成
                houseNumber = houseNumber,
                residentName = residentName,
                phone = phone,
                type = type,
                content = content,
                status = status,
                submitTime = submitTime,
                handleResult = handleResult
            )

            onSaveClicked(complaint)
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
