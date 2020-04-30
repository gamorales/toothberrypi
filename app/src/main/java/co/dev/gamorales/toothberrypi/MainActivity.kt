package co.dev.gamorales.toothberrypi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.dev.gamorales.toothberrypi.devices.controllers.DevicesAdapter
import co.dev.gamorales.toothberrypi.devices.models.DevicesDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT: Int = 1

    lateinit var mDevicesList: RecyclerView
    private val bAdapter = BluetoothAdapter.getDefaultAdapter()
    private val devices: MutableList<DevicesDTO> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verifyBluetooth(bAdapter)
        setupRecyclerView()

        ivBluetoothStatus.setOnClickListener {
            verifyBluetooth(bAdapter)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==REQUEST_ENABLE_BT) {
            if (resultCode==0) {
                Log.i("INFO", "Deshabilitado")
                ivBluetoothStatus.setBackgroundResource(R.drawable.not_connected)
                tvDeviceMessage.text = getString(R.string.bluetooth_off)
            } else {
                Log.i("INFO", "Habilitado")
                ivBluetoothStatus.setBackgroundResource(R.drawable.connected)
            }
            setupRecyclerView()
        }

        if (bAdapter.isDiscovering) {
            bAdapter.cancelDiscovery()
        }
    }

    fun verifyBluetooth(mBluetoothAdapter: BluetoothAdapter) {
        // Si el Bluetooth está apagado, se solicita permiso al usuario para iniciarlo
        if (!mBluetoothAdapter.isEnabled) {
            // mBluetoothAdapter.enable()
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            Log.i("INFO", "Habilitado")
            ivBluetoothStatus.setBackgroundResource(R.drawable.connected)
            setupRecyclerView()
        }
    }

    fun setupRecyclerView() {
        pbDevices.visibility = View.VISIBLE

        val mAdapter = DevicesAdapter(getDevices(), this)
        mDevicesList = findViewById(R.id.rvDevices) as RecyclerView
        mDevicesList.setHasFixedSize(true)
        mDevicesList.layoutManager = LinearLayoutManager(this)
        mDevicesList.adapter = mAdapter

        pbDevices.visibility = View.INVISIBLE
    }

    fun getDevices(mac: String=""): MutableList<DevicesDTO> {
        if(bAdapter==null){
            Toast.makeText(applicationContext,getString(R.string.bluetooth_not_supported), Toast.LENGTH_SHORT).show();
        } else {
            // Get paired devices.
            val pairedDevices: Set<BluetoothDevice> = bAdapter.bondedDevices
            // There are paired devices. Get the name and address of each paired device.
            if (pairedDevices.isNotEmpty()) {
                rvDevices.visibility = View.VISIBLE
                tvDeviceMessage.text = getString(R.string.paired_devices)
                devices.clear()

                for (device in pairedDevices) {
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    val deviceConnected = device.bondState

                    if (mac.isEmpty()) {
                        devices.add(
                            DevicesDTO(deviceName, deviceHardwareAddress, false, device)
                        )
                    } else {
                        devices.add(
                            DevicesDTO(deviceName, deviceHardwareAddress, true, device)
                        )
                    }

                    Log.i(
                        "INFO",
                        "DISPOSITIVOS: ${deviceName} --> ${deviceHardwareAddress}, ${deviceConnected}"
                    )
                }
            } else {
                rvDevices.visibility = View.GONE
                tvDeviceMessage.text= getString(R.string.devices_scan)
            }
        }

        return devices
    }

}