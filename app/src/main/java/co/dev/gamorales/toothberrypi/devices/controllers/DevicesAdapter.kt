package co.dev.gamorales.toothberrypi.devices.controllers

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import co.dev.gamorales.toothberrypi.R
import co.dev.gamorales.toothberrypi.devices.models.DevicesDTO
import kotlinx.android.synthetic.main.data_dialog.view.*
import kotlinx.android.synthetic.main.devices_list.view.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class DevicesAdapter(var devices: List<DevicesDTO>, var context: Context):
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    var sendReceive: SendReceive? = null
    var status: Boolean = false
    lateinit var tvStatus: TextView
    lateinit var tvData: TextView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesAdapter.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.devices_list, parent, false))
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: DevicesAdapter.ViewHolder, position: Int) {
        val item = devices.get(position)

        holder.cvDevice.setOnClickListener {
            Log.i("INFO", "El id ${item.deviceMAC}")
            showDialog(context,"${item.deviceName} - ${item.deviceMAC}", item.device)

            holder.cvDevice.ivDeviceStatus.setBackgroundResource(R.drawable.connected)
            holder.cvDevice.vDeviceBottomColor.setBackgroundColor(Color.parseColor("#77F077"))
        }

        holder.bind(item, context)
    }

    fun showDialog(context: Context, deviceName: String, device: BluetoothDevice?) {
        val mDialogView = LayoutInflater.from(context).inflate(R.layout.data_dialog, null)
        //AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(context)
            .setView(mDialogView)
            .setTitle(deviceName)
        //show dialog
        val  mAlertDialog = mBuilder.show()

        tvStatus = mDialogView.tvStatus
        tvData = mDialogView.tvData

        if (!status) {
            val clientClass = ClientClass(device, context)
            clientClass.start()

            val serverClass = ServerClass()
            serverClass.start()
        } else {
            tvStatus.text = context.getString(R.string.device_connected)
        }

        mDialogView.btnEnviar.isEnabled = status

        //login button click of custom layout
        mDialogView.btnEnviar.setOnClickListener {
            //get text from EditTexts of custom layout
            val red = mDialogView.etRed.text.toString()
            val password = mDialogView.etPassword.text.toString()
            val uuid = mDialogView.etUUID.text.toString()

            if (red.trim().isEmpty() || password.trim().isEmpty() || uuid.trim().isEmpty()) {
                Toast.makeText(context, context.getString(R.string.dialog_not_empty), Toast.LENGTH_LONG).show()
            } else {
                Log.i("INFO", "${red} - ${password} - ${uuid}")

                var data = "${red}||${password}||${uuid}"
                sendReceive!!.write(data.toByteArray(), true)

                // mAlertDialog.dismiss()
            }
        }
        mDialogView.btnCancelar.setOnClickListener {
            mAlertDialog.dismiss()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvDevice = view.findViewById(R.id.cvDevice) as CardView
        val tvDevice = view.findViewById(R.id.tvDevice) as TextView
        val tvDeviceMAC = view.findViewById(R.id.tvDeviceMAC) as TextView
        val ivDeviceStatus = view.findViewById(R.id.ivDeviceStatus) as ImageView
        val deviceBottomColor = view.findViewById(R.id.vDeviceBottomColor) as View

        fun bind(device: DevicesDTO, context: Context) {
            tvDevice.text = device.deviceName
            tvDeviceMAC.text = device.deviceMAC

            if (device.deviceStatus) {
                ivDeviceStatus.setBackgroundResource(R.drawable.connected)
                deviceBottomColor.setBackgroundColor(Color.parseColor("#77F077"))
            } else {
                ivDeviceStatus.setBackgroundResource(R.drawable.not_connected)
                deviceBottomColor.setBackgroundColor(Color.parseColor("#FF0000"))
            }
        }
    }

    var THREAD_EXECUTION_TIME = 8000
    val handlerTimer = Handler()
    var handler = Handler(Handler.Callback { msg ->
        // Toast.makeText(context, "SIIII ${msg.what}", Toast.LENGTH_LONG).show()
        Log.i("INFO", "SIIII ${msg.what}")
        when (msg.what) {
            STATE_LISTENING -> tvStatus.text = "Listening..."
            STATE_CONNECTING -> tvStatus.text = "Connecting..."
            STATE_CONNECTED -> {
                tvStatus.text = "Connected!"
                Log.i("INFO", "SIIII ${msg.what}")
                Thread.sleep(4000)

                handlerTimer.postDelayed(object : Runnable {
                    override fun run() {
                        try {
                            sendReceive!!.write("data".toByteArray())
                            Log.i("INFO", "Enviado!")
                        } catch (e: IOException) {
                            Log.e("ERROR", "ERROR SENDING DATA STRING ${e.message}")
                        }
                        handlerTimer.postDelayed(this, THREAD_EXECUTION_TIME.toLong())
                    }
                }, 0)

            }
            STATE_CONNECTION_FAILED -> tvStatus.text = "Connection Failed!"
            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                tvData.text = tempMsg
                Log.i("INFO", "EL MENSAJE: ${tempMsg}")
            }
        }
        true
    })

    private inner class ServerClass: Thread() {
        private val bAdapter = BluetoothAdapter.getDefaultAdapter()
        private var serverSocket: BluetoothServerSocket? = null
        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)
                    sendReceive = SendReceive(socket)
                    sendReceive!!.start()
                    break
                }
            }
        }

        init {
            try {
                serverSocket = bAdapter!!.listenUsingRfcommWithServiceRecord(
                    APP_NAME,
                    MY_UUID
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ClientClass(private val device: BluetoothDevice?, context: Context) : Thread() {
        private var socket: BluetoothSocket? = null
        override fun run() {
            try {
                socket!!.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
                status = true
            } catch (e: IOException) {
                status = false
                Log.e("INFO", "EL ERROR: ${e.message}")
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }

        init {
            try {
                socket = device!!.createRfcommSocketToServiceRecord(MY_UUID)
                tvStatus.text = context.getString(R.string.device_connecting)
                status = true
            } catch (e: IOException) {
                tvStatus.text = context.getString(R.string.device_not_connected)
                status = false
                e.printStackTrace()
            }
        }
    }

    inner class SendReceive(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
//            Toast.makeText(context, "lerolero", Toast.LENGTH_LONG).show()
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    Log.i("INFO", "QUESO ${bytes}")
                    handler.obtainMessage(
                        STATE_MESSAGE_RECEIVED,
                        bytes,
                        -1,
                        buffer
                    ).sendToTarget()

                } catch (e: IOException) {
                    Log.i("INFO", "EL ERROR ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?, message: Boolean=false) {
            try {
                outputStream!!.write(bytes)
                Log.i("INFO", "LOS BYTES: ${bytes}")
                if (message) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_sent),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                Log.i("INFO", "EL ERROR ${e.message}")
                if (message) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_error),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_parameters_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket!!.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                Log.i("INFO", "EL ERROR ${e.message}")
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }
    }

    companion object {
        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_MESSAGE_RECEIVED = 5
        private const val APP_NAME = "ToothBerryPi"
        private val MY_UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66")
    }

}