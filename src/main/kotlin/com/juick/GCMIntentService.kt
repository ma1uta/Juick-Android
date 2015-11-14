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
package com.juick

import android.app.Notification
import android.app.NotificationManager
// import android.app.PendingIntent
import android.content.Context
import android.content.Intent
// import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gcm.GCMBaseIntentService
import com.juick.android.MainActivity
import com.juick.android.PMActivity
import com.juick.android.Utils
import com.juick.android.api.JuickMessage
import java.net.URLEncoder
import org.json.JSONObject

/**

 * @author Ugnich Anton
 */
class GCMIntentService : GCMBaseIntentService(GCMIntentService.SENDER_ID) {

    override protected fun onRegistered(context: Context, regId: String) {
        try {
            val res = Utils.getJSON(context, "https://api.juick.com/android/register?regid=" + URLEncoder.encode(regId, "UTF-8"))
            if (res != null) {
                val spe = PreferenceManager.getDefaultSharedPreferences(context).edit()
                spe.putString("gcm_regid", regId)
                spe.commit()
            }
        } catch (e: Exception) {
        }

    }

    override protected fun onUnregistered(context: Context, regId: String) {
        try {
            Utils.getJSON(context, "https://api.juick.com/android/unregister?regid=" + URLEncoder.encode(regId, "UTF-8"))
            val spe = PreferenceManager.getDefaultSharedPreferences(context).edit()
            spe.remove("gcm_regid")
            spe.commit()
        } catch (e: Exception) {
        }

    }

    override protected fun onMessage(context: Context, intent: Intent) {
        if (intent.hasExtra("message")) {
            val msg = intent.getExtras().getString("message")
            try {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val curactivity = sp.getString("currentactivity", null)

                val jsonmsg = JSONObject(msg)
                val jmsg = JuickMessage.parseJSON(jsonmsg)
                if (jmsg.MID == 0 && curactivity != null && curactivity == "pm-" + jmsg.User!!.UID) {
                    val i = Intent(GCMEVENTACTION)
                    i.putExtra("message", msg)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i)
                } else {
                    var title = "@" + jmsg.User!!.UName!!
                    if (!jmsg.getTags().isEmpty()) {
                        title += ": " + jmsg.getTags()
                    }
                    val body: String
                    val len = jmsg.Text?.length ?: 0
                    if (len > 65) {
                        body = jmsg.Text!!.substring(0, 60) + "..."
                    } else {
                        body = jmsg.Text!!
                    }

                    var notifpublic = 3
                    try {
                        notifpublic = Integer.parseInt(sp.getString("notif_public", "3"))
                    } catch (e: Exception) {
                    }

                    if (notifpublic > 0) {
                        val i: Intent
                        if (jmsg.MID == 0) {
                            i = Intent(context, PMActivity::class.java)
                            i.putExtra("uname", jmsg.User!!.UName)
                            i.putExtra("uid", jmsg.User!!.UID)
                        } else {
                            i = Intent(context, MainActivity::class.java)
                        }
                        // val contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
                        val notification = Notification(R.drawable.ic_notification, title, System.currentTimeMillis())
                        // notification.setLatestEventInfo(context.applicationContext, title, body, contentIntent)
                        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
                        notification.defaults = notification.defaults or Notification.DEFAULT_LIGHTS
                        if (notifpublic >= 2) {
                            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE
                        }
                        if (notifpublic == 3) {
                            notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
                        }
                        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
                    }
                }
            } catch (e: Exception) {
                Log.e("GCMIntentService.onMessage", e.toString())
            }

        }
    }

    override protected fun onError(context: Context, errorId: String) {
        Log.e("GCMIntentService.onError", errorId)
    }

    override protected fun onRecoverableError(context: Context, errorId: String): Boolean {
        Log.e("GCMIntentService.onRecoverableError", errorId)
        return super.onRecoverableError(context, errorId)
    }

    companion object {

        val SENDER_ID = "314097120259"
        val GCMEVENTACTION = "com.juick.android.gcm-event"
    }
}
