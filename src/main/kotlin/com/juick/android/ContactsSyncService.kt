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

import android.accounts.Account
import android.accounts.OperationCanceledException
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts
import com.juick.R
import com.juick.android.api.JuickUser
import java.io.ByteArrayOutputStream
import java.util.ArrayList
import java.util.HashMap
import org.json.JSONArray
import org.json.JSONException

/**

 * @author Ugnich Anton
 */
class ContactsSyncService : Service() {

    private class SyncAdapterImpl(private val mContext: Context) : AbstractThreadedSyncAdapter(mContext, true) {

        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            try {
                ContactsSyncService.performSync(mContext, account, extras, authority, provider, syncResult)
            } catch (e: OperationCanceledException) {
            }

        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return syncAdapter?.syncAdapterBinder
    }

    private val syncAdapter: SyncAdapterImpl?
        get() {
            if (sSyncAdapter == null) {
                sSyncAdapter = SyncAdapterImpl(this)
            }
            return sSyncAdapter
        }

    companion object {

        private var sSyncAdapter: SyncAdapterImpl? = null
        private var mContentResolver: ContentResolver? = null

        @Throws(OperationCanceledException::class)
        private fun performSync(context: Context, account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val localContacts = HashMap<String, Long>()
            mContentResolver = context.contentResolver

            // Load the local contacts
            val rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build()
            val c1 = mContentResolver!!.query(rawContactUri, arrayOf(BaseColumns._ID, RawContacts.SYNC1), null, null, null)
            while (c1.moveToNext()) {
                localContacts.put(c1.getString(1), c1.getLong(0))
            }

            val jsonStr = Utils.getJSON(context, "https://api.juick.com/users/friends")
            if (jsonStr != null && jsonStr.length > 4) {
                try {
                    val json = JSONArray(jsonStr)
                    val cnt = json.length()
                    for (i in 0..cnt - 1) {
                        val user = JuickUser.parseJSON(json.getJSONObject(i))
                        if (!localContacts.containsKey(user.UName)) {
                            addContact(context, account, user)
                        }
                    }
                } catch (e: JSONException) {
                }

            }
        }

        private fun addContact(context: Context, account: Account, user: JuickUser) {
            //     Log.i(TAG, "Adding contact: " + name);
            val operationList = ArrayList<ContentProviderOperation>()

            //Create our RawContact
            var builder: ContentProviderOperation.Builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            builder.withValue(RawContacts.ACCOUNT_NAME, account.name)
            builder.withValue(RawContacts.ACCOUNT_TYPE, account.type)
            builder.withValue(RawContacts.SYNC1, user.UName)
            operationList.add(builder.build())

            // Nickname
            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            builder.withValueBackReference(ContactsContract.CommonDataKinds.Nickname.RAW_CONTACT_ID, 0)
            builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
            builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.UName)
            operationList.add(builder.build())

            // StructuredName
            if (user.FullName != null) {
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0)
                builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.FullName)
                operationList.add(builder.build())
            }

            // Photo
            val photo = Utils.downloadImage("http://i.juick.com/a/" + user.UID + ".png")
            if (photo != null) {
                val baos = ByteArrayOutputStream()
                photo.compress(Bitmap.CompressFormat.PNG, 100, baos)
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                builder.withValueBackReference(ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID, 0)
                builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, baos.toByteArray())
                operationList.add(builder.build())
            }

            // link to profile
            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.com.juick.profile")
            builder.withValue(ContactsContract.Data.DATA1, user.UID)
            builder.withValue(ContactsContract.Data.DATA2, user.UName)
            builder.withValue(ContactsContract.Data.DATA3, context.getString(R.string.Juick_profile))
            builder.withValue(ContactsContract.Data.DATA4, user.UName)
            operationList.add(builder.build())

            try {
                mContentResolver!!.applyBatch(ContactsContract.AUTHORITY, operationList)
            } catch (e: Exception) {
                //      Log.e(TAG, "Something went wrong during creation! " + e);
                e.printStackTrace()
            }

        }
    }
}
