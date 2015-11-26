package com.juick.android

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.google.android.gms.gcm.GcmListenerService
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import com.google.android.gms.iid.InstanceIDListenerService
import com.juick.android.api.JuickMessage
import org.json.JSONObject
import java.net.URLEncoder
import com.juick.R

val SENDER_ID = "314097120259"
val GCMEVENTACTION = "com.juick.android.gcm-event"

class GCMReceriverService : GcmListenerService() {
    override fun onMessageReceived(from: String?, data: Bundle?) {
        Log.d(JUICK_TAG, "onMessageReceived")

        val message = data?.getString("message")
        Log.i(JUICK_TAG, "GCM RECEIVE $(message)")
        if (message != null) {
            try {
                val sp = PreferenceManager.getDefaultSharedPreferences(this)
                val curactivity = sp.getString("currentactivity", null)

                val jsonmsg = JSONObject(message)
                val jmsg = JuickMessage.parseJSON(jsonmsg)
                if (jmsg.MID == 0 && curactivity != null && curactivity == "pm-" + jmsg.User!!.UID) {
                    val i = Intent(GCMEVENTACTION)
                    i.putExtra("message", message)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i)
                } else {
                    var title = "@" + jmsg.User!!.UName!!
                    if (!jmsg.tags.isEmpty()) {
                        title += ": " + jmsg.getTags()
                    }
                    val body: String?
                    if (jmsg.Text!!.length > 64) {
                        body = jmsg.Text!!.substring(0, 60) + "..."
                    } else {
                        body = jmsg.Text
                    }

                    var notifpublic = 3
                    try {
                        notifpublic = Integer.parseInt(sp.getString("notif_public", "3"))
                    } catch (e: Exception) {
                    }

                    if (notifpublic > 0) {
                        val i: Intent
                        if (jmsg.MID == 0) {
                            i = Intent(this, PMActivity::class.java)
                            i.putExtra("uname", jmsg.User!!.UName)
                            i.putExtra("uid", jmsg.User!!.UID)
                        } else {
                            i = Intent(this, MainActivity::class.java)
                        }
                        val contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
                        val notification = NotificationCompat.Builder(this)
                        notification.setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle(title)
                                .setContentText(body)
                                .setAutoCancel(true)
                                .setWhen(0)
                                .setContentIntent(contentIntent).setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND)

                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification.build())
                    }
                }

            } catch (e: Exception) {
                Log.e(JUICK_TAG, "GCM onMessageReceived: $(e.toString())")
            }
        }
    }
}

class GCMIDService : InstanceIDListenerService() {
    override fun onTokenRefresh() {
        Log.d(JUICK_TAG, "GCM: onTokenRefresh")

        val instanceID = InstanceID.getInstance(this)
        val token = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)
        try {
            val res = getJSON(this, "https://api.juick.com/android/register?regid=" + URLEncoder.encode(token, "UTF-8"))
            if (res != null) {
                val spe = PreferenceManager.getDefaultSharedPreferences(this).edit()
                spe.putString("gcm_regid", token)
                spe.commit()
            }
        } catch (e: Exception) {
            Log.e(JUICK_TAG, "GCM onTokenRefresh: " + e.toString())
        }

        Log.d(JUICK_TAG, "GCM onTokenRefresh token = $(token)")
    }
}
