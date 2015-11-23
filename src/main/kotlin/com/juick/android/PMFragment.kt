/*
 * Juick
 * Copyright (C) 2008-2013, ugnich
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

// import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.juick.R
import com.juick.android.api.JuickMessage
import org.json.JSONArray

/**

 * @author ugnich
 */
class PMFragment : ListFragment() {

    private var parentActivity: PMFragmentListener? = null
    private var listAdapter: PMAdapter? = null
    private var uname: String? = null
    private var uid: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            parentActivity = activity as PMFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement PMFragmentListener")
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle) {
        super.onViewCreated(view, savedInstanceState)

        uname = arguments.getString("uname")
        uid = arguments.getInt("uid", 0)

        listAdapter = PMAdapter(activity, uid)
        listView.dividerHeight = 0

        val thr = Thread(object : Runnable {

            override fun run() {
                val url = "https://api.juick.com/pm?uname=" + uname!!
                val jsonStr = getJSON(activity, url)
                if (isAdded) {
                    onNewMessages(jsonStr)
                }
            }
        })
        thr.start()
    }

    fun onNewMessages(msg: String?) {
        if (listAdapter != null && msg != null) {
            activity.runOnUiThread(object : Runnable {

                override fun run() {
                    try {
                        listAdapter!!.parseJSON(msg)
                        setListAdapter(listAdapter)
                        listView.setSelection(listAdapter!!.count - 1)
                    } catch (e: Exception) {
                        Log.e("PMFragment.onNewMessage", e.toString())
                    }

                }
            })
        }
    }

    interface PMFragmentListener
}

internal class PMAdapter(context: Context, var uid: Int) : ArrayAdapter<JuickMessage>(context, R.layout.listitem_pm_in) {

    fun parseJSON(jsonStr: String): Int {
        try {
            val json = JSONArray(jsonStr)
            val cnt = json.length()
            for (i in 0..cnt - 1) {
                add(JuickMessage.parseJSON(json.getJSONObject(i)))
            }
            return cnt
        } catch (e: Exception) {
            Log.e("initOpinionsAdapter", e.toString())
        }

        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val msg = getItem(position)

        var v: View? = convertView

        if (msg.User!!.UID == uid) {
            if (v == null || !v.tag.toString().equals("i")) {
                val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = vi.inflate(R.layout.listitem_pm_in, null)
                v!!.tag = "i"
            }

            val tv = v.findViewById(R.id.text) as TextView
            tv.text = msg.Text
        } else {
            if (v == null || !v.tag.toString().equals("o")) {
                val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = vi.inflate(R.layout.listitem_pm_out, null)
                v!!.tag = "o"
            }

            val tv = v.findViewById(R.id.text) as TextView
            tv.text = msg.Text
        }

        return v
    }
}
