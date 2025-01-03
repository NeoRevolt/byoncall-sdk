package com.dartmedia.byoncallsdkclient.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dartmedia.byoncallsdkclient.R
import com.dartmedia.byoncallsdkclient.adapter.ChatAdapter
import com.dartmedia.byoncallsdkclient.databinding.ActivityMainBinding
import com.dartmedia.byoncallsdkclient.model.ChatModel
import com.dartmedia.byoncallsdk.libraryapi.ByonCallSDK
import com.dartmedia.byoncallsdk.model.SocketDataModel
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum
import com.dartmedia.byoncallsdk.utils.image.WhiteBackgroundTransformation
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


class ChatActivity : AppCompatActivity(), ByonCallSDK.CallListener {

    private var byonCallSDK: ByonCallSDK? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter

    private val chatModelList = mutableListOf<ChatModel>()

    private var myPhone = ""
    private var myBrandName = ""
    private var myImageUrl = ""
    private var targetPhone = ""
    private var callMessage = ""

    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        onClickListeners()
    }

    private fun init() {
        myPhone = intent.getStringExtra(MY_PHONE) ?: ""
        targetPhone = intent.getStringExtra(TARGET_PHONE) ?: ""
        myBrandName = intent.getStringExtra(MY_BRAND_NAME) ?: ""
        myImageUrl = intent.getStringExtra(MY_IMAGE_URL) ?: ""
        callMessage = intent.getStringExtra(CALL_MESSAGE) ?: ""

        binding.usernameTv.text = targetPhone

        if (myPhone.isEmpty() || myPhone == "") {
            finish()
        } else {
            byonCallSDK = ByonCallSDK.initialize(
                this,
                socketUrl = SOCKET_URL,
                myPhone = myPhone
            )
            byonCallSDK?.callListener = this
            chatAdapter = ChatAdapter()
            binding.rvChat.apply {
                layoutManager = LinearLayoutManager(this@ChatActivity)
                adapter = chatAdapter
            }
            observeChatFromSocket(myPhone)
        }
    }

    private fun onClickListeners() {
        binding.apply {

            backBtn.setOnClickListener {
//                onBackPressedDispatcher.onBackPressed()
                byonCallSDK?.stopService()
                finish()
            }

            sendButton.setOnClickListener {
                val message = binding.messageEditText.text.toString()
                val generatedId = UUID.randomUUID().toString()
                if (message.isNotEmpty()) {
                    val chatModel = ChatModel(
                        id = generatedId,
                        senderId = myPhone,
                        conversationId = generatedId,
                        message = message,
                        attachment = "none",
                        createAt = Date().time,
                        receiverId = targetPhone,
                    )
                    sendChatMessage(chatModel)
                    binding.messageEditText.setText("")
                }
            }

            audioCallBtn.setOnClickListener {
                try {
                    byonCallSDK?.startCall(
                        SocketDataModel(
                            type = SocketDataTypeEnum.StartAudioCall,
                            senderId = myPhone,
                            receiverId = targetPhone,
                            senderName = myBrandName,
                            senderImage = myImageUrl,
                            callMessage = callMessage
                        )
                    )
                    startActivity(
                        Intent(
                            this@ChatActivity,
                            CallActivity::class.java
                        ).apply {
                            putExtra("target", targetPhone)
                            putExtra("sender", myPhone)
                            putExtra("message", callMessage)
                            putExtra("isVideoCall", false)
                            putExtra("isCaller", true)
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "voiceCall() e: $e")

                }
            }

            videoCallBtn.setOnClickListener {
                try {
                    byonCallSDK?.startCall(
                        SocketDataModel(
                            type = SocketDataTypeEnum.StartVideoCall,
                            senderId = myPhone,
                            receiverId = targetPhone,
                            senderName = myBrandName,
                            senderImage = myImageUrl,
                            callMessage = callMessage
                        )
                    )
                    startActivity(
                        Intent(
                            this@ChatActivity,
                            CallActivity::class.java
                        ).apply {
                            putExtra("target", targetPhone)
                            putExtra("sender", myPhone)
                            putExtra("message", callMessage)
                            putExtra("isVideoCall", true)
                            putExtra("isCaller", true)
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "videoCall() e: $e")
                }
            }

        }
    }

    private fun sendChatMessage(chatModel: ChatModel) {
        byonCallSDK?.sendChatToSocket(
            SocketDataModel(
                type = SocketDataTypeEnum.StartChatting,
                senderId = myPhone,
                receiverId = targetPhone,
                data = chatModel
            )
        )
    }

    private fun observeChatFromSocket(myUserId: String) {
        byonCallSDK?.observeChatFromSocket(this) { chatFromSocket ->
            when (chatFromSocket.type) {
                SocketDataTypeEnum.StartChatting -> {
                    Log.d(TAG, "observeChat : $chatFromSocket")
                    val chat =
                        Gson().fromJson(Gson().toJson(chatFromSocket.data), ChatModel::class.java)
                    val chatList = chat.copy(isSelf = chat.senderId == myUserId)

                    if (chatFromSocket.senderId == targetPhone || chatFromSocket.senderId == myUserId) {
                        chatModelList.add(chatList)
                        chatAdapter.submitChat(chatModelList)
                        binding.rvChat.scrollToPosition(chatModelList.size - 1)
                    } else {
                        Toast.makeText(
                            this,
                            "Theres another message from ${chatFromSocket.senderId}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                else -> Unit
            }

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCallReceived(data: SocketDataModel) {
        Log.d(TAG, "onCallReceived : $data")
        runOnUiThread {
            binding.apply {
                messageEditText.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(messageEditText.windowToken, 0)

                val isVideoCall = data.type == SocketDataTypeEnum.StartVideoCall
                if (isVideoCall) {
                    incomingCallTitleTv.text = "Branded Video Call"
                    callTypeIconIv.setImageResource(R.drawable.video_ic_filled_blue)

                } else {
                    incomingCallTitleTv.text = "Branded Voice Call"
                    callTypeIconIv.setImageResource(R.drawable.voice_icon_blue)
                }
                incomingCallCallerTv.text = "${data.senderName}"
                incomingCallIntentTv.text = "${data.callMessage}"
                val imageUrl = data.senderImage
                if (imageUrl != null) {
                    Glide.with(this@ChatActivity)
                        .load(imageUrl)
                        .transform(WhiteBackgroundTransformation())
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.rounded_message_gray)
                                .error(R.drawable.asset_person_ic)
                                .circleCrop()
                        )
                        .into(incomingCallCallerPicture)
                } else {
                    Glide.with(this@ChatActivity)
                        .load(R.drawable.asset_person_ic)
                        .apply(RequestOptions.circleCropTransform())
                        .into(incomingCallCallerPicture)
                }
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    startActivity(Intent(this@ChatActivity, CallActivity::class.java).apply {
                        putExtra("target", data.senderId)
                        putExtra("targetName", data.senderName)
                        putExtra("targetImg", data.senderImage)
                        putExtra("message", data.callMessage)
                        putExtra("sender", myPhone)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", false)
                    })
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        incomingCallLayout.isVisible = false
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    try {
                        byonCallSDK?.rejectCall(data)
                        byonCallSDK?.recordCallLog()//TODO (Zal): Record call log to DB
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(TAG, "$TAG Exception : ${e.message}")
                    }
                }
                callMeLaterButton.setOnClickListener {
                    showDatePickerDialog(data)
                }
                remindMeButton.setOnClickListener {
                    showRemindMeDialog(data)
                }
            }
        }
    }

    // Function to show DatePickerDialog
    private fun showDatePickerDialog(data: SocketDataModel) {
        val calendar = Calendar.getInstance() // Get the current date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Save the selected date in Calendar
                selectedDateTime.set(Calendar.YEAR, selectedYear)
                selectedDateTime.set(Calendar.MONTH, selectedMonth) // Months are 0-indexed
                selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDay)

                // After selecting the date, show the time picker dialog
                showTimePickerDialog(data)
            }, year, month, day)

        // Show the DatePickerDialog
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(data: SocketDataModel) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // Save the selected time in Calendar
            selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedDateTime.set(Calendar.MINUTE, selectedMinute)

            // Format the selected date and time and display it
            val formattedDateTime = formatDateTime(selectedDateTime)

            //Send message to chat and Send reject call
            val generatedId = UUID.randomUUID().toString()
            if (formattedDateTime.isNotEmpty()) {
                val chatModel = ChatModel(
                    id = generatedId,
                    senderId = myPhone,
                    conversationId = generatedId,
                    message = "Call me later at $formattedDateTime",
                    attachment = "none",
                    createAt = Date().time,
                    receiverId = targetPhone,
                )
                try {
                    byonCallSDK?.rejectCall(data)
                    byonCallSDK?.recordCallLog()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        sendChatMessage(chatModel)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "$TAG Exception : ${e.message}")
                }
                binding.incomingCallLayout.isVisible = false
            }

        }, hour, minute, true) // true for 24-hour format, false for 12-hour AM/PM format

        // Show the TimePickerDialog
        timePickerDialog.show()
    }

    private fun formatDateTime(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun showRemindMeDialog(data: SocketDataModel) {
        // Define the list items for the dialog
        val items = arrayOf("30 minutes", "60 minutes", "120 minutes")

        // Build and show the AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Remind me in")

        // Set the list items
        builder.setItems(items) { _, which ->
            // Get the selected item as a string
            val selectedItem = items[which]

            //Send message to chat and Send reject call
            val generatedId = UUID.randomUUID().toString()
            if (selectedItem.isNotEmpty()) {
                val chatModel = ChatModel(
                    id = generatedId,
                    senderId = myPhone,
                    conversationId = generatedId,
                    message = "I will call you in $selectedItem",
                    attachment = "none",
                    createAt = Date().time,
                    receiverId = targetPhone,
                )
                try {
                    byonCallSDK?.rejectCall(data)
                    byonCallSDK?.recordCallLog()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        sendChatMessage(chatModel)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "$TAG Exception : ${e.message}")
                }
                binding.incomingCallLayout.isVisible = false
            }
        }

        // Show the dialog
        builder.create().show()
    }

    override fun onCallDeclined(model: SocketDataModel) {
        Log.d(TAG, "onCallDeclined : $model")
        runOnUiThread {
            binding.incomingCallLayout.isVisible = false
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        byonCallSDK?.stopService()
    }

    override fun onDestroy() {
        byonCallSDK?.disconnectSocket()
        super.onDestroy()
    }

    companion object {
        const val MY_PHONE = "myPhone"
        const val MY_BRAND_NAME = "myBrandName"
        const val TARGET_PHONE = "targetPhone"
        const val CALL_MESSAGE = "callMessage"
        const val MY_IMAGE_URL = "myImageUrl"
        const val TAG = "ChatActivity"

        const val SOCKET_URL = "http://103.39.68.184:8901/socket/private"

    }


}