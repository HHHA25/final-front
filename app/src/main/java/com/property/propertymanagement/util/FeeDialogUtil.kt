package com.property.propertymanagement.util

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.property.propertymanagement.R
import com.property.propertymanagement.model.Fee
import com.google.android.material.textfield.TextInputEditText

object FeeDialogUtil {
    fun showFeeDialog(
        activity: Activity,
        existingFee: Fee? = null,
        onSaveClicked: (Fee) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fee)

        // 获取控件
        val etHouseNumber = dialog.findViewById<TextInputEditText>(R.id.et_house_number)
        val etResidentName = dialog.findViewById<TextInputEditText>(R.id.et_resident_name)
        val etAmount = dialog.findViewById<TextInputEditText>(R.id.et_amount)
        val etMonth = dialog.findViewById<TextInputEditText>(R.id.et_month)
        val etStatus = dialog.findViewById<TextInputEditText>(R.id.et_status)
        val etPaymentDate = dialog.findViewById<TextInputEditText>(R.id.et_payment_date)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save)

        // 如果是编辑模式，填充已有数据
        existingFee?.let {
            etHouseNumber.setText(it.houseNumber)
            etResidentName.setText(it.residentName)
            etAmount.setText(it.amount.toString())
            etMonth.setText(it.month)
            etStatus.setText(it.status)
            etPaymentDate.setText(it.paymentDate)
        }

        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 保存按钮
        btnSave.setOnClickListener {
            val houseNumber = etHouseNumber.text.toString().trim()
            val residentName = etResidentName.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val month = etMonth.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val paymentDate = etPaymentDate.text.toString().trim().ifEmpty { null }

            // 验证输入
            if (houseNumber.isEmpty() || residentName.isEmpty() ||
                amountStr.isEmpty() || month.isEmpty() || status.isEmpty()) {
                Toast.makeText(activity, "请填写必填字段", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = try {
                amountStr.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(activity, "金额格式不正确", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 创建或更新费用对象
            val fee = existingFee?.copy(
                houseNumber = houseNumber,
                residentName = residentName,
                amount = amount,
                month = month,
                status = status,
                paymentDate = paymentDate
            ) ?: Fee(
                id = 0, // 新增时ID为0，数据库会自动生成
                houseNumber = houseNumber,
                residentName = residentName,
                amount = amount,
                month = month,
                status = status,
                paymentDate = paymentDate
            )

            onSaveClicked(fee)
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