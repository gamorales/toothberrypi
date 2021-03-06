package co.dev.gamorales.toothberrypi.devices.models

import android.bluetooth.BluetoothDevice

data class DevicesDTO (
    val deviceName: String, val deviceMAC: String,
    val deviceStatus: Boolean, val device: BluetoothDevice
)