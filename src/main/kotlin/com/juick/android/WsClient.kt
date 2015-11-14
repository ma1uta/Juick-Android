/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer

/**

 * @author Ugnich Anton
 */

interface WsClientListener {

    // @Throws(IOException::class)
    fun onWebSocketTextFrame(data: String)
}

class WsClient(val listener: WsClientListener) {

    internal var sock: Socket? = null
    internal var `is`: InputStream? = null
    internal var os: OutputStream? = null


    fun connect(host: String, port: Int, location: String, headers: String?): Boolean {
        try {
            sock = Socket(host, port)
            `is` = sock!!.inputStream
            os = sock!!.outputStream

            var handshake = "GET $location HTTP/1.1\r\nHost: $host\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nOrigin: http://juick.com/\r\nUser-Agent: JuickAndroid\r\nSec-WebSocket-Key: SomeKey\r\nSec-WebSocket-Version: 13\r\nPragma: no-cache\r\nCache-Control: no-cache\r\n"
            if (headers != null) {
                handshake += headers
            }
            handshake += "\r\n"
            os!!.write(handshake.toByteArray())

            return true
        } catch (e: Exception) {
            System.err.println(e)
            //e.printStackTrace();
            return false
        }

    }

    // val isConnected: Boolean
    //     get() = sock!!.isConnected

    fun readLoop() {
        try {
            val buf = ByteBuffer.allocate(160)
            var flagInside = false
            var byteCnt = 0
            var PacketLength = 0
            var bigPacket = false
            var b: Int = `is`!!.read()
            while (b != -1) {
                if (flagInside) {
                    byteCnt++

                    if (byteCnt == 1) {
                        if (b < 126) {
                            PacketLength = b + 1
                            bigPacket = false
                        } else {
                            bigPacket = true
                        }
                    } else {
                        if (byteCnt == 2 && bigPacket) {
                            PacketLength = b shl 8
                        }
                        if (byteCnt == 3 && bigPacket) {
                            PacketLength = PacketLength or b
                            PacketLength += 3
                        }

                        if (byteCnt > 3 || !bigPacket) {
                            buf.put(b.toByte())
                        }
                    }

                    if (byteCnt == PacketLength) {
                        if (PacketLength > 2) {
                            listener.onWebSocketTextFrame(String(buf.array(), "utf-8"))
                        } else {
                            os!!.write(keepAlive)
                            os!!.flush()
                        }
                        flagInside = false
                    }
                } else if (b == 129) {
                    buf.clear()
                    flagInside = true
                    byteCnt = 0
                }
                b = `is`!!.read()
            }
        } catch (e: Exception) {
            System.err.println(e)
        }

    }

    fun disconnect() {
        try {
            os!!.write(closeConnection)
            os!!.flush()
            `is`!!.close()
            os!!.close()
            sock!!.close()
        } catch (e: Exception) {
            System.err.println(e)
        }

    }

    companion object {

        internal val keepAlive = byteArrayOf(129.toByte(), 1.toByte(), 32.toByte())
        internal val closeConnection = byteArrayOf(136.toByte(), 0.toByte())
    }
}
