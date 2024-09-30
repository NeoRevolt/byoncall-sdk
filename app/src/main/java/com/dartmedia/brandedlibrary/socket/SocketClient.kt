package com.dartmedia.brandedlibrary.socket

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dartmedia.brandedlibrary.model.SocketDataModel
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketClient @Inject constructor(
    private val gson: Gson
) {

    private var socket: Socket? = null
    private var myUserId: String? = null

    private val _onLatestChat = MutableLiveData<SocketDataModel>()
    val onLatestChat: LiveData<SocketDataModel> get() = _onLatestChat


    fun connectSocket(myUserId: String) {
        try {
            socket = IO.socket(SOCKET_URL)
            socket?.connect()
            this.myUserId = myUserId
            observeChatFromSocket()
            Log.d(TAG, "Connected to socket with id: $myUserId")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun observeChatFromSocket() {
        if (myUserId != null) {
            socket?.on(myUserId) { args ->
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
        } else {
            Log.d(TAG, "observeChatFromSocket : Error myUserId is Null")
        }
    }


    fun observeSocketEvent(listener: Listener) {
        if (myUserId != null) {
            socket?.on(myUserId) { args ->
                args?.let { d ->
                    if (d.isNotEmpty()) {
                        val data = d[0]
                        if (data.toString().isNotEmpty()) {
                            val dataFromSocket =
                                gson.fromJson(data.toString(), SocketDataModel::class.java)
                            listener.onLatestEventReceived(dataFromSocket)
                            Log.d(TAG, "observeSocketEvent: $data")
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "observeSocketEvent : Error myUserId is Null")
        }
    }

    fun sendMessageToSocket(socketDataModel: SocketDataModel) {
        try {
            val jsonStr = gson.toJson(socketDataModel, SocketDataModel::class.java)
            socket?.emit(SocketTag.POST_MESSAGE, jsonStr)
            _onLatestChat.postValue(socketDataModel)
            Log.d(TAG, "sendMessageToSocket: $jsonStr")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnectSocket(function: () -> Unit) {
        Log.e(TAG, "Disconnected from socket")
        socket?.disconnect()
        socket?.off()
        function()
    }

    private object SocketTag {
        const val POST_MESSAGE = "privateMessage"
    }

    companion object {
        private const val TAG = "SocketClient"

        //        private const val SOCKET_URL = "http://10.0.2.2:8005/socket/private" // LOCAL EMULATOR
        private const val SOCKET_URL = "http://103.39.68.184:8901/socket/private" // LOCAL DEVICE
    }

    interface Listener {
        fun onLatestEventReceived(event: SocketDataModel)

    }

}