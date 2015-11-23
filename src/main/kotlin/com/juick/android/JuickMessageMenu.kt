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

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Toast
import com.juick.R
import com.juick.android.api.JuickMessage
import java.net.URLEncoder
import java.util.ArrayList
// import java.util.regex.Matcher

/**

 * @author Ugnich Anton
 */
class JuickMessageMenu(private var activity: Activity) : OnItemLongClickListener, OnClickListener {
    private var listSelectedItem: JuickMessage? = null
    private var urls: ArrayList<String>? = null
    private var menuLength: Int = 0

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val l = parent.getAdapter().getItem(position)
        if (l is JuickMessage) {
            listSelectedItem = l
            urls = ArrayList<String>()
            val photo = listSelectedItem?.Photo
            if (photo != null)
                urls!!.add(photo)
            val video = listSelectedItem?.Video
            if (video != null)
                urls!!.add(video)

            var pos = 0
            var m = JuickMessagesAdapter.urlPattern.matcher(listSelectedItem?.Text)
            while (m.find(pos)) {
                urls!!.add(listSelectedItem?.Text!!.substring(m.start(), m.end()))
                pos = m.end()
            }

            pos = 0
            m = JuickMessagesAdapter.msgPattern.matcher(listSelectedItem?.Text)
            while (m.find(pos)) {
                urls!!.add(listSelectedItem?.Text!!.substring(m.start(), m.end()))
                pos = m.end()
            }
            /*
            pos = 0;
            m = JuickMessagesAdapter.usrPattern.matcher(listSelectedItem.Text);
            while (m.find(pos)) {
            urls.add(listSelectedItem.Text.substring(m.start(), m.end()));
            pos = m.end();
            }
            */
            menuLength = 4 + urls!!.size
            if (listSelectedItem?.RID == 0) {
                menuLength++
            }
            val items = arrayOfNulls<CharSequence>(menuLength)
            var i = 0
            if (urls!!.size > 0) {
                for (url in urls!!) {
                    items[i++] = url
                }
            }
            if (listSelectedItem?.RID == 0) {
                items[i++] = activity.getResources().getString(R.string.Recommend_message)
            }
            val UName = listSelectedItem?.User!!.UName
            items[i++] = '@' + UName.toString() + " " + activity.getResources().getString(R.string.blog)
            items[i++] = activity.getResources().getString(R.string.Subscribe_to) + " @" + UName
            items[i++] = activity.getResources().getString(R.string.Blacklist) + " @" + UName
            items[i] = activity.getResources().getString(R.string.Share)

            val builder = AlertDialog.Builder(activity)
            builder.setItems(items, this)
            builder.show()
            return true
        }
        return false
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        var which = which
        if (urls != null) {
            if (which < urls!!.size) {
                val url = urls!![which]
                if (url.startsWith("#")) {
                    val mid = Integer.parseInt(url.substring(1))
                    if (mid > 0) {
                        val intent = Intent(activity, ThreadActivity::class.java)
                        intent.putExtra("mid", mid)
                        activity.startActivity(intent)
                    }
                    //} else if (url.startsWith("@")) {
                } else {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return
            }
            which -= urls!!.size
        }
        if (listSelectedItem?.RID != 0) {
            which += 1
        }
        when (which) {
            0 -> confirmAction(R.string.Are_you_sure_recommend, object : Runnable {

                override fun run() {
                    postMessage("! #" + listSelectedItem?.MID, activity.getResources().getString(R.string.Recommended))
                }
            })
            1 -> {
                val i = Intent(activity, MessagesActivity::class.java)
                i.putExtra("uid", listSelectedItem?.User!!.UID)
                i.putExtra("uname", listSelectedItem?.User!!.UName)
                activity.startActivity(i)
            }
            2 -> confirmAction(R.string.Are_you_sure_subscribe, object : Runnable {

                override fun run() {
                    postMessage("S @" + listSelectedItem?.User!!.UName!!, activity.getResources().getString(R.string.Subscribed))
                }
            })
            3 -> confirmAction(R.string.Are_you_sure_blacklist, object : Runnable {

                override fun run() {
                    postMessage("BL @" + listSelectedItem?.User!!.UName!!, activity.getResources().getString(R.string.Added_to_BL))
                }
            })
            4 -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.setType("text/plain")
                intent.putExtra(Intent.EXTRA_TEXT, listSelectedItem.toString())
                activity.startActivity(intent)
            }
        }
    }

    private fun confirmAction(resId: Int, action: Runnable) {
        val builder = AlertDialog.Builder(activity)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(activity.getResources().getString(resId))
        builder.setPositiveButton(R.string.Yes, object : OnClickListener {

            override fun onClick(dialog: DialogInterface, which: Int) {
                action.run()
            }
        })
        builder.setNegativeButton(R.string.Cancel, null)
        builder.show()
    }

    private fun postMessage(body: String, ok: String) {
        val thr = Thread(object : Runnable {

            override fun run() {
                try {
                    val ret = postJSON(activity, "https://api.juick.com/post", "body=" + URLEncoder.encode(body, "utf-8"))
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            Toast.makeText(activity, if ((ret != null)) ok else activity.getResources().getString(R.string.Error), Toast.LENGTH_SHORT).show()
                        }
                    })
                } catch (e: Exception) {
                    Log.e("postMessage", e.toString())
                }

            }
        })
        thr.start()
    }
}
