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
import android.os.AsyncTask
import com.juick.android.api.JuickMessage
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.ListFragment
import android.view.View
import android.widget.AdapterView
import com.juick.R
import com.juick.android.api.JuickUser
import com.neovisionaries.ws.client.*

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**

 * @author Ugnich Anton
 */
class ThreadFragment : ListFragment(), AdapterView.OnItemClickListener {

    private var parentActivity: ThreadFragmentListener? = null
    private var listAdapter: JuickMessagesAdapter? = null
    private var ws: WebSocket? = null
    private var mid = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        try {
            parentActivity = activity as ThreadFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement ThreadFragmentListener")
        }

    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        if (args != null) {
            mid = args.getInt("mid", 0)
        }
        if (mid == 0) {
            return
        }

        initWebSocket()
        initAdapter()
    }

    private fun initWebSocket() {
        if (ws == null) {
            AsyncTask.execute(object : Runnable {
                override fun run() {
                    try {
                        ws = Utils.wsFactory!!.createSocket(URI("wss", "ws.juick.com", "/" + mid, null))
                        ws!!.addHeader("Origin", "ws.juick.com")
                        ws!!.addHeader("Host", "ws.juick.com") //TODO: remove from server side
                        ws!!.addListener(object : WebSocketAdapter() {
                            @Throws(Exception::class)
                            override fun onTextMessage(websocket: WebSocket?, jsonStr: String?) {
                                super.onTextMessage(websocket, jsonStr)
                                if (!isAdded) {
                                    return
                                }
                                (activity.getSystemService(Activity.VIBRATOR_SERVICE) as Vibrator).vibrate(250)
                                activity.runOnUiThread(object : Runnable {

                                    override fun run() {
                                        if (jsonStr != null) {
                                            listAdapter!!.parseJSON("[$jsonStr]")
                                            listAdapter!!.getItem(1).Text = resources.getString(R.string.Replies) + " (" + Integer.toString(listAdapter!!.count - 2) + ")"
                                        }
                                    }
                                })
                            }
                        })
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                    }

                    ws!!.connectAsynchronously()
                }
            })

        }
    }

    private fun initAdapter() {
        listAdapter = JuickMessagesAdapter(activity, JuickMessagesAdapter.TYPE_THREAD)

        listView.onItemClickListener = this
        listView.onItemLongClickListener = JuickMessageMenu(activity)

        val thr = Thread(object : Runnable {

            override fun run() {
                val jsonStr = Utils.getJSON(activity, "https://api.juick.com/thread?mid=" + mid)
                if (isAdded) {
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (jsonStr != null) {
                                listAdapter!!.parseJSON(jsonStr)
                                setListAdapter(listAdapter)
                                if (listAdapter!!.count > 0) {
                                    initAdapterStageTwo()
                                }
                            }
                        }
                    })
                }
            }
        })
        thr.start()
    }

    private fun initAdapterStageTwo() {
        if (!isAdded) {
            return
        }
        val replies = resources.getString(R.string.Replies) + " (" + Integer.toString(listAdapter!!.count - 1) + ")"
        listAdapter!!.addDisabledItem(replies, 1)

        val author = listAdapter!!.getItem(0).User
        parentActivity!!.onThreadLoaded(author!!.UID, author.UName!!)
    }

    override fun onPause() {
        if (ws != null) {
            ws!!.disconnect()
            ws = null
        }
        super.onPause()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val jmsg = parent.getItemAtPosition(position) as JuickMessage
        parentActivity!!.onReplySelected(jmsg.RID, jmsg!!.Text!!)
    }

    interface ThreadFragmentListener {

        fun onThreadLoaded(uid: Int, nick: String)

        fun onReplySelected(rid: Int, txt: String)
    }
}
