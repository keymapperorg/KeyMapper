package io.github.sds100.keymapper.sysbridge.adb

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.ADB_AUTH_RSAPUBLICKEY
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.ADB_AUTH_SIGNATURE
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_AUTH
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_CLSE
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_CNXN
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_MAXDATA
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_OKAY
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_OPEN
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_STLS
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_STLS_VERSION
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_VERSION
import io.github.sds100.keymapper.sysbridge.adb.AdbProtocol.A_WRTE
import timber.log.Timber
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

@RequiresApi(Build.VERSION_CODES.M)
internal class AdbClient(private val host: String, private val port: Int, private val key: AdbKey) :
    Closeable {

    private lateinit var socket: Socket
    private lateinit var plainInputStream: DataInputStream
    private lateinit var plainOutputStream: DataOutputStream

    private var useTls = false

    private lateinit var tlsSocket: SSLSocket
    private lateinit var tlsInputStream: DataInputStream
    private lateinit var tlsOutputStream: DataOutputStream

    private val inputStream get() = if (useTls) tlsInputStream else plainInputStream
    private val outputStream get() = if (useTls) tlsOutputStream else plainOutputStream

    fun connect() {
        socket = Socket(host, port)
        socket.tcpNoDelay = true
        plainInputStream = DataInputStream(socket.getInputStream())
        plainOutputStream = DataOutputStream(socket.getOutputStream())

        write(A_CNXN, A_VERSION, A_MAXDATA, "host::")

        var message = read()
        if (message.command == A_STLS) {
            write(A_STLS, A_STLS_VERSION, 0)

            val sslContext = key.sslContext
            tlsSocket = sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
            tlsSocket.startHandshake()
            Timber.d("Handshake succeeded.")

            tlsInputStream = DataInputStream(tlsSocket.inputStream)
            tlsOutputStream = DataOutputStream(tlsSocket.outputStream)
            useTls = true

            message = read()
        } else if (message.command == A_AUTH) {
            write(A_AUTH, ADB_AUTH_SIGNATURE, 0, key.sign(message.data))

            message = read()
            if (message.command != A_CNXN) {
                write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, key.adbPublicKey)
                message = read()
            }
        }

        if (message.command != A_CNXN) {
            error("not A_CNXN")
        }
    }

    fun shellCommand(command: String): ByteArray {
        val localId = 1
        write(A_OPEN, localId, 0, "shell:$command")

        var message = read()
        when (message.command) {
            A_OKAY -> {
                while (true) {
                    message = read()
                    val remoteId = message.arg0
                    if (message.command == A_WRTE) {
                        if (message.data_length > 0) {
                            return message.data!!
                        }
                        write(A_OKAY, localId, remoteId)
                    } else if (message.command == A_CLSE) {
                        write(A_CLSE, localId, remoteId)
                        break
                    } else {
                        error("not A_WRTE or A_CLSE")
                    }
                }
            }

            A_CLSE -> {
                val remoteId = message.arg0
                write(A_CLSE, localId, remoteId)
            }

            else -> {
                error("not A_OKAY or A_CLSE")
            }
        }

        error("No response from adb?")
    }

    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) = write(
        AdbMessage(command, arg0, arg1, data)
    )

    private fun write(command: Int, arg0: Int, arg1: Int, data: String) = write(
        AdbMessage(
            command,
            arg0,
            arg1,
            data
        )
    )

    private fun write(message: AdbMessage) {
        outputStream.write(message.toByteArray())
        outputStream.flush()
        Timber.d("write ${message.toStringShort()}")
    }

    private fun read(): AdbMessage {
        val buffer =
            ByteBuffer.allocate(AdbMessage.Companion.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

        inputStream.readFully(buffer.array(), 0, 24)

        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val checksum = buffer.int
        val magic = buffer.int
        val data: ByteArray?
        if (dataLength >= 0) {
            data = ByteArray(dataLength)
            inputStream.readFully(data, 0, dataLength)
        } else {
            data = null
        }
        val message = AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
        message.validateOrThrow()
        Timber.d("read ${message.toStringShort()}")
        return message
    }

    override fun close() {
        try {
            plainInputStream.close()
        } catch (e: Throwable) {
        }
        try {
            plainOutputStream.close()
        } catch (e: Throwable) {
        }
        try {
            socket.close()
        } catch (e: Exception) {
        }

        if (useTls) {
            try {
                tlsInputStream.close()
            } catch (e: Throwable) {
            }
            try {
                tlsOutputStream.close()
            } catch (e: Throwable) {
            }
            try {
                tlsSocket.close()
            } catch (e: Exception) {
            }
        }
    }
}
