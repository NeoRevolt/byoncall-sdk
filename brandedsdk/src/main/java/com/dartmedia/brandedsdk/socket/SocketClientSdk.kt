package com.dartmedia.brandedsdk.socket

import android.util.Log
import com.dartmedia.brandedsdk.model.SocketDataModel
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketClientSdk @Inject constructor(
    private val gson: Gson
) {

    private var socket: Socket? = null
    private var myPhone: String? = null

    fun connectSocket(myPhone: String, socketUrl: String) {
        try {
            socket = IO.socket(socketUrl)
            socket?.connect()
            this.myPhone = myPhone
            Log.i(TAG, "Connected to socket with Id : $myPhone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect socket : $e")
            e.printStackTrace()
        }
    }

    fun observeSocketEvent(socketListener: SocketListener) {
        if (myPhone != null) {
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

        } else {
            Log.e(TAG, "observeSocketEvent : Error myPhone is null")
        }
    }

    fun sendEventToSocket(socketDataModel: SocketDataModel) {
        try {
            val jsonStr = Gson().toJson(socketDataModel, SocketDataModel::class.java)
            socket?.emit("privateMessage", jsonStr)
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


    companion object {
        private val TAG = SocketClientSdk::class.java.toString()
    }

}
