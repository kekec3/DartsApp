package com.example.darts.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

class NearbyManager(context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.example.darts.NEARBY_SERVICE"

    // 1. ADVERTISING (Sender broadcasts using the generated Token as its name)
    fun startAdvertising(token: String, payload: String, onStatus: (String) -> Unit) {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        val lifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                // Automatically accept connection
                connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                    override fun onPayloadReceived(endpointId: String, payload: Payload) {}
                    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
                })
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    val bytes = payload.toByteArray(StandardCharsets.UTF_8)
                    connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
                    onStatus("Connected! Sending match data...")
                } else {
                    onStatus("Connection Failed: ${result.status.statusMessage}")
                }
            }

            override fun onDisconnected(endpointId: String) {
                onStatus("Receiver Disconnected")
            }
        }

        // We use the unique token as the 'User Name' so discoverers can filter by it!
        connectionsClient.startAdvertising(token, SERVICE_ID, lifecycleCallback, options)
            .addOnSuccessListener { onStatus("Advertising active with token: $token") }
            .addOnFailureListener { e -> onStatus("Advertising Failed: ${e.localizedMessage}") }
    }

    // 2. DISCOVERY (Receiver only connects if endpoint name matches the QR token)
    fun startDiscovery(targetToken: String, onPayloadReceived: (String) -> Unit, onStatus: (String) -> Unit) {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        val discoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // CRITICAL: Verify if this is the device we scanned!
                if (info.endpointName == targetToken) {
                    onStatus("Found matching host! Requesting connection...")

                    connectionsClient.requestConnection("ReceiverName", endpointId, object : ConnectionLifecycleCallback() {
                        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                            connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                    if (payload.type == Payload.Type.BYTES) {
                                        payload.asBytes()?.let { bytes ->
                                            val receivedData = String(bytes, StandardCharsets.UTF_8)
                                            onPayloadReceived(receivedData)
                                        }
                                    }
                                }

                                override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                                    if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                                        onStatus("Import Complete!")
                                    }
                                }
                            })
                        }

                        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                            if (result.status.isSuccess) {
                                onStatus("Connected")
                            } else {
                                onStatus("Connection Denied")
                            }
                        }

                        override fun onDisconnected(endpointId: String) {
                            onStatus("Sender Disconnected")
                        }
                    })
                } else {
                    onStatus("Found another device (${info.endpointName}), ignoring...")
                }
            }

            override fun onEndpointLost(endpointId: String) {
                onStatus("Host Lost")
            }
        }

        connectionsClient.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            .addOnSuccessListener { onStatus("Searching for QR target...") }
            .addOnFailureListener { e -> onStatus("Discovery Failed: ${e.localizedMessage}") }
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}