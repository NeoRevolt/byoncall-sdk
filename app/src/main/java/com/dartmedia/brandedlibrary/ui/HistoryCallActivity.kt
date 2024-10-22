package com.dartmedia.brandedlibrary.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dartmedia.brandedlibrary.R
import com.dartmedia.brandedlibrary.adapter.CallLogAdapter
import com.dartmedia.brandedlibrary.databinding.ActivityHistoryCallBinding
import com.dartmedia.brandedlibrary.ui.viewmodel.HistoryCallViewModel
import com.dartmedia.brandedlibrary.ui.viewmodel.ViewModelFactory
import com.dartmedia.brandedlibrary.utils.image.WhiteBackgroundTransformation
import com.dartmedia.brandedsdk.model.SocketDataModel
import com.dartmedia.brandedsdk.model.SocketDataTypeEnum
import com.dartmedia.brandedsdk.repository.WebRTCRepository
import com.dartmedia.brandedsdk.service.MainService
import com.dartmedia.brandedsdk.service.MainServiceRepository
import com.dartmedia.brandedsdk.socket.SocketClientSdk
import com.dartmedia.network.CallHistoryData
import com.dartmedia.network.CallHistoryResponse
import com.dartmedia.network.RetrofitClient
import com.dartmedia.network.TokenUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class HistoryCallActivity : AppCompatActivity(), MainService.Listener {

    @Inject
    lateinit var socketClient: SocketClientSdk

    @Inject
    lateinit var mainRepository: WebRTCRepository

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    private lateinit var binding: ActivityHistoryCallBinding
    private lateinit var callLogAdapter: CallLogAdapter

    private var myPhone = ""

    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMySession()
        initRv()
        observeLogsFromServer()
        getLogsFromDb()
        onClickListener()


    }

    private fun initMySession() {
        myPhone = intent.getStringExtra(MY_PHONE) ?: ""

        Log.d(TAG, "phoneNumber : $myPhone")

        if (myPhone.isEmpty() || myPhone == "") {
            finish()
        } else {
            mainRepository.connectSocket(socketUrl = SOCKET_URL, myPhone)
            MainService.listener = this
            mainServiceRepository.startService(myPhone)
        }
    }

    private fun onClickListener() {
        binding.apply {

            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    p0?.let {
                        searchLogByName(it.toString())
                    }
                }

                override fun afterTextChanged(p0: Editable?) {}

            })
        }
    }

    private fun initRv() {
        callLogAdapter = CallLogAdapter()
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.setHasFixedSize(true)
        binding.rvLogs.adapter = callLogAdapter
    }


    private fun observeLogsFromServer() {
        RetrofitClient.instance.getCallHistoryById(
            TokenUtils.HEADER_TOKEN, myPhone
        )
            .enqueue(object : Callback<CallHistoryResponse> {
                override fun onResponse(
                    call: Call<CallHistoryResponse>,
                    response: Response<CallHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        val callHistoryList = response.body()?.data
                        if (callHistoryList != null) {
                            insertLogsToDb(callHistoryList)
                            Log.i(TAG, "observeLogsFromServer: $callHistoryList ")
                        }
                    } else {
                        Log.e(TAG, "observeLogsFromServer: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CallHistoryResponse>, t: Throwable) {
                    Log.e(TAG, "observeLogsFromServer: ${t.message}}")
                }

            })
    }

    private fun insertLogsToDb(serverLogList: List<CallHistoryData>) {
        val historyCallViewModel = obtainViewModel(this@HistoryCallActivity)
        historyCallViewModel.syncCallLogs(serverLogList)
    }

    private fun getLogsFromDb() {
        val historyCallViewModel = obtainViewModel(this@HistoryCallActivity)
        historyCallViewModel.getAllLogs().observe(this) { logList ->
            if (logList != null) {
                callLogAdapter.setListLogs(logList)
            }
        }
    }


    private fun searchLogByName(name: String) {
        val historyCallViewModel = obtainViewModel(this@HistoryCallActivity)
        historyCallViewModel.getLogByName(name).observe(this) { log ->
            if (log != null) {
                callLogAdapter.setListLogs(log)
            }
        }
    }


    private fun obtainViewModel(activity: AppCompatActivity): HistoryCallViewModel {
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProvider(activity, factory).get(HistoryCallViewModel::class.java)
    }

    override fun onCallReceived(data: SocketDataModel) {
        Log.d(TAG, "onCallReceived : $data")
        runOnUiThread {
            binding.apply {
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
                    Glide.with(this@HistoryCallActivity)
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
                    Glide.with(this@HistoryCallActivity)
                        .load(R.drawable.asset_person_ic)
                        .apply(RequestOptions.circleCropTransform())
                        .into(incomingCallCallerPicture)
                }
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    startActivity(Intent(this@HistoryCallActivity, CallActivity::class.java).apply {
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
                        mainRepository.sendRejectCall(data)
                        mainRepository.recordCallLog()//TODO (Zal): Record call log to DB
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(ChatActivity.TAG, "${ChatActivity.TAG} Exception : ${e.message}")
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

    private fun showDatePickerDialog(data: SocketDataModel) {
        val calendar = java.util.Calendar.getInstance() // Get the current date
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Save the selected date in Calendar
                selectedDateTime.set(java.util.Calendar.YEAR, selectedYear)
                selectedDateTime.set(
                    java.util.Calendar.MONTH,
                    selectedMonth
                ) // Months are 0-indexed
                selectedDateTime.set(java.util.Calendar.DAY_OF_MONTH, selectedDay)

                // After selecting the date, show the time picker dialog
                showTimePickerDialog(data)
            }, year, month, day)

        // Show the DatePickerDialog
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(data: SocketDataModel) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedDateTime.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
            selectedDateTime.set(java.util.Calendar.MINUTE, selectedMinute)

            val formattedDateTime = formatDateTime(selectedDateTime)

            if (formattedDateTime.isNotEmpty()) {
                try {
                    mainRepository.sendRejectCall(data)
                    mainRepository.recordCallLog()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        // TODO send chat or make alarm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(ChatActivity.TAG, "${ChatActivity.TAG} Exception : ${e.message}")
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
            if (selectedItem.isNotEmpty()) {
                try {
                    mainRepository.sendRejectCall(data)
                    mainRepository.recordCallLog()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        // TODO send chat or make alarm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(ChatActivity.TAG, "${ChatActivity.TAG} Exception : ${e.message}")
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

    override fun onRestart() {
        super.onRestart()
        observeLogsFromServer()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        socketClient.disconnectSocket()
        super.onDestroy()
    }

    companion object {
        var TAG = HistoryCallActivity::class.java.simpleName
        const val MY_PHONE = "myPhoneNumber"
        const val SOCKET_URL = "http://103.39.68.184:8901/socket/private"
    }
}