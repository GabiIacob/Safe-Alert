package com.example.safealert

import android.content.Context
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class SendScheduledMessageWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val phone = inputData.getString("phone")
        val message = inputData.getString("message")

        if (!phone.isNullOrEmpty() && !message.isNullOrEmpty()) {
            try {
                SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
                Log.d("ScheduledMessage", "Mesaj trimis catre $phone")

                return Result.success()
            } catch (e: Exception) {
                Log.e("ScheduledMessage", "Eroare la trimiterea mesajului: ${e.localizedMessage}")
                return Result.failure()
            }
        }

        return Result.failure()
    }
}
