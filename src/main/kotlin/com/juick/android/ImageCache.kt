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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.support.v4.util.LruCache
import android.util.Log
import com.jakewharton.DiskLruCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
// import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**

 * @author Ugnich Anton
 */
class ImageCache(context: Context, uniqueName: String, diskCacheSize: Long) {
    private var mDiskCache: DiskLruCache? = null
    private var mMemoryCache: LruCache<String, Bitmap>? = null

    init {
        try {
            val cachePath = if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable)
                getExternalCacheDir(context).path
            else
                context.cacheDir.path
            val diskCacheDir = File(cachePath + File.separator + uniqueName)
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize)

            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            val cacheSize = maxMemory / 8

            mMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {

                override protected fun sizeOf(key: String, bitmap: Bitmap): Int {
                    return bitmap.rowBytes * bitmap.height / 1024
                }
            }

        } catch (e: IOException) {
            Log.e("ImageCache.ImageCache", e.toString())
        }

    }

    fun getImageMemory(key: String): Bitmap? {
        return mMemoryCache?.get(key)
    }

    fun getImageDisk(key: String): Bitmap? {
        var bitmap: Bitmap? = null
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = mDiskCache!!.get(key)
            if (snapshot != null) {
                val `in` = snapshot.getInputStream(0)
                if (`in` != null) {
                    val buffIn = BufferedInputStream(`in`, IO_BUFFER_SIZE)
                    bitmap = BitmapFactory.decodeStream(buffIn)
                    if (bitmap != null) {
                        mMemoryCache!!.put(key, bitmap)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("ImageCache.get", e.toString())
        } finally {
            if (snapshot != null) {
                snapshot.close()
            }
        }
        return bitmap
    }

    fun getImageNetwork(key: String, url: String): Boolean {
        var ret = false
        var editor: DiskLruCache.Editor? = null
        var `in`: BufferedInputStream? = null
        var out: OutputStream? = null
        try {
            editor = mDiskCache!!.edit(key)
            if (editor == null) {
                return false
            }

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            `in` = BufferedInputStream(conn.inputStream, IO_BUFFER_SIZE)

            out = BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)
            val buffer = ByteArray(IO_BUFFER_SIZE)
            var len: Int = `in`.read(buffer)
            while (len != -1) {
                out.write(buffer, 0, len)
                len = `in`.read(buffer)
            }

            mDiskCache!!.flush()
            editor.commit()
            ret = true
        } catch (e: IOException) {
            try {
                if (editor != null) {
                    editor.abort()
                }
            } catch (ignored: IOException) {
            }

        } finally {
            try {
                if (out != null) {
                    out.close()
                }
            } catch (ignored: IOException) {
            }

            try {
                if (`in` != null) {
                    `in`.close()
                }
            } catch (ignored: IOException) {
            }

        }
        return ret
    }

    companion object {

        val IO_BUFFER_SIZE = 8 * 1024
        private val APP_VERSION = 1
        private val VALUE_COUNT = 1

        val isExternalStorageRemovable: Boolean
            get() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    return Environment.isExternalStorageRemovable()
                }
                return true
            }

        fun getExternalCacheDir(context: Context): File {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                return context.externalCacheDir
            }

            // Before Froyo we need to construct the external cache dir ourselves
            val cacheDir = "/Android/data/" + context.packageName + "/cache/"
            return File(Environment.getExternalStorageDirectory().path + cacheDir)
        }
    }
}