package com.dartmedia.brandedsdk.socket

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dartmedia.brandedsdk.model.SocketDataModel
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket


object BrandedSocketClient {

    private val TAG = BrandedSocketClient::class.java.simpleName.toString()
    private var gson = Gson()

    private var socket: Socket? = null
    private var myPhone: String? = null

    private val _onLatestChat = MutableLiveData<SocketDataModel>()
    val onLatestChat: LiveData<SocketDataModel> get() = _onLatestChat

    fun connectSocket(myPhone: String, socketUrl: String) {
        try {
            socket = IO.socket(socketUrl)
            socket?.connect()
            this.myPhone = myPhone
            observeChatFromSocket()
            Log.i(TAG, "Connected to socket with Id : $myPhone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect socket : $e")
            e.printStackTrace()
        }
    }

    private fun observeChatFromSocket() {
        if (myPhone == null) {
            Log.e(
                TAG,
                "observeChatFromSocket : Error myPhone is null. Ensure connectSocket() is called before this."
            )
            return
        }
        try {
            socket?.on(myPhone) { args ->
                args?.let { d ->
                    if (d.isNotEmpty()) {
                        val data = d[0]
                        if (data.toString().isNotEmpty()) {
                            val dataFromSocket =
                                gson.fromJson(data.toString(), SocketDataModel::class.java)
                            _onLatestChat.postValue(dataFromSocket)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "observeChatFromChat Exception : $e")
        }
    }

    fun observeSocketEvent(socketListener: SocketListener) {
        if (myPhone == null) {
            Log.e(
                TAG,
                "observeSocketEvent : Error myPhone is null. Ensure connectSocket() is called before this."
            )
            return
        }

        try {
            socket?.on(myPhone) { args ->
                args?.let { d ->
                    if (d.isNotEmpty()) {
                        val data = d[0]
                        if (data.toString().isNotEmpty()) {
                            val dataFromSocket =
                                Gson().fromJson(
                                    data.toString(),
                                    SocketDataModel::class.java
                                )
                            socketListener.onLatestSocketEvent(dataFromSocket)
                            Log.d(TAG, "observeSocketEvent: $data")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "observeSocketEvent Exception : $e")
            e.printStackTrace()
        }
    }

    fun sendEventToSocket(socketDataModel: SocketDataModel) {
        try {
            val jsonStr = Gson().toJson(socketDataModel, SocketDataModel::class.java)
            socket?.emit("privateMessage", jsonStr)
            _onLatestChat.postValue(socketDataModel)
            Log.d(TAG, "sendEventToSocket : $jsonStr")
        } catch (e: Exception) {
            Log.e(TAG, "sendEventToSocket Exception : $e")
            e.printStackTrace()
        }
    }

    fun disconnectSocket() {
        try {
            socket?.disconnect()
            socket?.off()
            Log.e(TAG, "Disconnected from socket as $myPhone")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect socket : $e")
            e.printStackTrace()
        }
    }


    interface SocketListener {
        fun onLatestSocketEvent(event: SocketDataModel)
    }


}
