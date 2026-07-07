package com.example.darts.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyManager(context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.example.darts.NEARBY_SERVICE"

    // 1. ADVERTISING (Sender)
    fun startAdvertising(payload: String, onStatus: (String) -> Unit) {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        val lifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                // Automatically accept the connection
                connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                    override fun onPayloadReceived(endpointId: String, payload: Payload) {}
                    override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {}
                })
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    connectionsClient.sendPayload(endpointId, Payload.fromBytes(payload.toByteArray()))
                    onStatus("Connection Success: Data Sent")
                } else {
                    onStatus("Connection Failed")
                }
            }

            override fun onDisconnected(endpointId: String) {}
        }

        connectionsClient.startAdvertising("DartsPlayer", SERVICE_ID, lifecycleCallback, options)
            .addOnFailureListener { onStatus("Advertising Failed") }
    }

    // 2. DISCOVERY (Receiver)
    fun startDiscovery(onPayloadReceived: (String) -> Unit, onStatus: (String) -> Unit) {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        val discoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // Request connection to the advertiser
                connectionsClient.requestConnection("ReceiverName", endpointId, object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                        connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                val receivedData = String(payload.asBytes()!!)
                                onPayloadReceived(receivedData)
                            }
                            override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {}
                        })
                    }
                    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                        if (result.status.isSuccess) onStatus("Connected to Sender")
                    }
                    override fun onDisconnected(endpointId: String) {}
                })
            }
            override fun onEndpointLost(endpointId: String) {}
        }

        connectionsClient.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            .addOnFailureListener { onStatus("Discovery Failed") }
    }

    // 3. CLEANUP
    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}