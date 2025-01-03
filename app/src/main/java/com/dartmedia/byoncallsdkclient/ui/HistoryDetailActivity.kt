package com.dartmedia.byoncallsdkclient.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dartmedia.byoncallsdkclient.R
import com.dartmedia.byoncallsdkclient.databinding.ActivityHistoryDetailBinding
import com.dartmedia.byoncallsdk.utils.image.WhiteBackgroundTransformation

class HistoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryDetailBinding

    private var callType = ""
    private var callDuration = ""
    private var callerName = ""
    private var date = ""
    private var message = ""
    private var callerNumber = ""
    private var callerImage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        callType = intent.getStringExtra(CALL_TYPE) ?: ""
        callDuration = intent.getStringExtra(CALL_DURATION) ?: ""
        callerName = intent.getStringExtra(CALLER_NAME) ?: ""
        date = intent.getStringExtra(DATE) ?: ""
        message = intent.getStringExtra(MESSAGE) ?: ""
        callerNumber = intent.getStringExtra(CALLER_NUMBER) ?: ""
        callerImage = intent.getStringExtra(CALLER_IMAGE) ?: ""

        if (callerImage != "") {
            Glide.with(this@HistoryDetailActivity)
                .load(callerImage)
                .transform(WhiteBackgroundTransformation())
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.rounded_message_gray)
                        .error(R.drawable.asset_person_ic)
                        .circleCrop()
                )
                .into(binding.voiceCallCallerImage)
        } else {
            Glide.with(binding.root.context)
                .load(R.drawable.asset_person_ic)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.voiceCallCallerImage)
        }
        binding.callType.text = callType
        binding.callDurationTv.text = callDuration + " seconds"
        binding.callerName.text = callerName
        binding.dateTv.text = date
        binding.messageTv.text = message
        binding.numberTv.text = callerNumber
    }

    companion object {
        const val CALL_TYPE = "callType"
        const val CALL_DURATION = "callDuration"
        const val CALLER_NAME = "callerName"
        const val DATE = "date"
        const val MESSAGE = "message"
        const val CALLER_NUMBER = "callerNumber"
        const val CALLER_IMAGE = "callerImage"
    }
}