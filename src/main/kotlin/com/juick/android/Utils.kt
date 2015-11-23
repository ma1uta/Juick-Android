/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.juick.R
import com.neovisionaries.ws.client.WebSocketFactory

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**

 * @author Ugnich Anton
 */
object Utils {

    fun hasAuth(context: Context): Boolean {
        val am = AccountManager.get(context)
        val accs = am.getAccountsByType(context.getString(R.string.com_juick))
        return accs.size() > 0
    }

    fun getBasicAuthString(context: Context): String {
        val am = AccountManager.get(context)
        val accs = am.getAccountsByType(context.getString(R.string.com_juick))
        if (accs.size() > 0) {
            var b: Bundle? = null
            try {
                b = am.getAuthToken(accs[0], "", false, null, null).result
            } catch (e: Exception) {
                Log.e("getBasicAuthString", Log.getStackTraceString(e))
            }

            if (b != null) {
                val authStr = b.getString(AccountManager.KEY_ACCOUNT_NAME) + ":" + b.getString(AccountManager.KEY_AUTHTOKEN)
                return "Basic " + Base64.encodeToString(authStr.toByteArray(), Base64.NO_WRAP)
            }
        }
        return ""
    }

    fun getJSON(context: Context, url: String): String? {
        var ret: String? = null
        try {
            val jsonURL = URL(url)
            val conn = jsonURL.openConnection() as HttpURLConnection

            val basicAuth = getBasicAuthString(context)
            if (basicAuth.length > 0) {
                conn.setRequestProperty("Authorization", basicAuth)
            }

            conn.useCaches = false
            conn.doInput = true
            conn.connect()
            if (conn.responseCode == 200) {
                ret = streamToString(conn.inputStream)
            }

            conn.disconnect()
        } catch (e: Exception) {
            Log.e("getJSON", e.toString())
        }

        return ret
    }

    fun postJSON(context: Context, url: String, data: String): String? {
        var ret: String? = null
        try {
            val jsonURL = URL(url)
            val conn = jsonURL.openConnection() as HttpURLConnection

            val basicAuth = getBasicAuthString(context)
            if (basicAuth.length > 0) {
                conn.setRequestProperty("Authorization", basicAuth)
            }

            conn.useCaches = false
            conn.doInput = true
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.connect()

            val wr = OutputStreamWriter(conn.outputStream)
            wr.write(data)
            wr.close()

            if (conn.responseCode == 200) {
                ret = streamToString(conn.inputStream)
            }

            conn.disconnect()
        } catch (e: Exception) {
            Log.e("getJSON", e.toString())
        }

        return ret
    }

    fun streamToString(`is`: InputStream): String? {
        try {
            val buf = BufferedReader(InputStreamReader(`is`))
            val str = StringBuilder()
            var line: String?
            do {
                line = buf.readLine()
                str.append(line).append("\n")
            } while (line != null)
            return str.toString()
        } catch (e: Exception) {
            Log.e("streamReader", e.toString())
        }

        return null
    }

    fun downloadImage(url: String): Bitmap? {
        try {
            val imgURL = URL(url)
            val conn = imgURL.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            return BitmapFactory.decodeStream(conn.inputStream)
        } catch (e: Exception) {
            Log.e("downloadImage", e.toString())
        }

        return null
    }

    fun isWiFiConnected(context: Context): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWiFi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return mWiFi.isConnected
    }

    private var WSFactoryInstance: WebSocketFactory? = null

    val wsFactory: WebSocketFactory?
        get() {
            if (WSFactoryInstance == null) {
                WSFactoryInstance = WebSocketFactory()
            }
            return WSFactoryInstance
        }
}
