package com.example.soundtt

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearBy(private val context: Context) {
    var SERVICE_ID = "atuo.nearby"
    var nickname = "atuo"
    val TAG = "myapp"
    var rally_flag = 0
    var count = 5
    var startflag = 0
    var connectionflag = 0

    private lateinit var playAudio: PlayAudio

    // 接続されたエンドポイントIDを保存するリスト
    private val connectedEndpoints = mutableListOf<String>()

    fun advertise() {
        Log.d(TAG, "advertiseをタップ")
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                nickname,
                SERVICE_ID,
                mConnectionLifecycleCallback,
                AdvertisingOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Advertise開始した")
            }
            .addOnFailureListener {
                Log.d(TAG, "Advertiseできなかった")
            }
    }

    fun discovery() {
        Log.d(TAG, "Discoveryをタップ")
        Nearby.getConnectionsClient(context)
            .startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Discovery開始した")
            }
            .addOnFailureListener {
                Log.d(TAG, "Discovery開始できなかった")
            }
    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.d(TAG, "Advertise側を発見した")
            Nearby.getConnectionsClient(context)
                .requestConnection(nickname, endpointId, mConnectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "見つけたエンドポイントを見失った")
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "他の端末からコネクションのリクエストを受け取った")
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "コネクションリクエストの結果を受け取った時")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "コネクションが確立した。今後通信が可能。")
                    connectedEndpoints.add(endpointId)
                    Log.d(TAG, "通信成功")
                    connectionflag = 1
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "コネクションが拒否された時。通信はできない。")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "エラーでコネクションが確立できない時。通信はできない。")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "コネクションが切断された")
            connectedEndpoints.remove(endpointId)
        }
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = payload.asBytes()!!
                    val countString = String(data)
                    count = countString.toInt()
                    rally_flag = 0
                    startflag = 1
                    Log.d(TAG, data.toString())
                    Log.d(TAG, "バイト配列を受け取った")

                    // 音を出す処理
                    playAudio = PlayAudio()
                    playAudio.playAudio("count$count", context)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // 転送状態が更新された時の詳細は省略
        }
    }

    fun date_push() {
        Log.d(TAG, "date_pushをタップ")

        if (startflag == 1) {
            count -= 1
        } else {
            startflag = 1
        }

        if (count != 0) {
            playAudio = PlayAudio()
            playAudio.playAudio("count$count", context)
        }

        val data = count.toString().toByteArray()
        val payload = Payload.fromBytes(data)

        // 全ての接続されたエンドポイントにデータを送信
        for (endpointId in connectedEndpoints) {
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
        }
        Log.d(TAG, "データを送った")

        rally_flag = 1
    }
}
