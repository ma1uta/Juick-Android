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

import android.content.Context
import android.content.Intent
// import android.content.SharedPreferences
// import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.juick.R
// import org.json.JSONArray
import org.json.JSONObject

/**

 * @author ugnich
 */
class ChatsFragment : ListFragment(), OnItemClickListener {

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.onItemClickListener = this

        val sp = PreferenceManager.getDefaultSharedPreferences(activity)
        val jcacheMain = sp.getString("jcache_main", null)
        if (jcacheMain != null) {
            try {
                val listAdapter = ChatsAdapter(activity)
                listAdapter.parseJSON(jcacheMain)
                setListAdapter(listAdapter)
            } catch (e: Exception) {
            }

        }

        val thr = Thread(object : Runnable {

            override fun run() {
                val url = "https://api.juick.com/groups_pms?cnt=10"
                val jsonStr = getJSON(activity, url)
                if (isAdded() && jsonStr != null && (jcacheMain == null || jsonStr != jcacheMain)) {
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            try {
                                var listAdapter: ChatsAdapter? = listAdapter as ChatsAdapter
                                if (listAdapter == null) {
                                    listAdapter = ChatsAdapter(activity)
                                }
                                listAdapter.parseJSON(jsonStr)
                                setListAdapter(listAdapter)

                                val spe = PreferenceManager.getDefaultSharedPreferences(activity).edit()
                                spe.putString("jcache_main", jsonStr)
                                spe.commit()
                            } catch (e: Exception) {
                            }

                        }
                    })
                }
            }
        })
        thr.start()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val listAdapter = listAdapter as ChatsAdapter
        val item = listAdapter.getItem(position)

        val i = Intent(activity, PMActivity::class.java)
        i.putExtra("uname", item.userName)
        i.putExtra("uid", item.userID)
        startActivity(i)
    }
}

private class ChatsAdapter(context: Context) : ArrayAdapter<ChatsAdapterItem>(context, R.layout.listitem_juickmessage) {
    private val userpics: ImageCache

    init {
        userpics = ImageCache(context, "userpics-small", 1024 * 1024 * 1)
    }

    fun parseJSON(jsonStr: String): Int {
        try {
            clear()

            val json = JSONObject(jsonStr).getJSONArray("pms")
            val cnt = json.length()
            for (i in 0..cnt - 1) {
                val j = json.getJSONObject(i)
                val item = ChatsAdapterItem()
                item.userName = j.getString("uname")
                item.userID = j.getInt("uid")
                if (j.has("MessagesCount")) {
                    item.unreadMessages = j.getInt("MessagesCount")
                }
                add(item)
            }
            return cnt
        } catch (e: Exception) {
            Log.e("MainAdapter.parseJSON", e.toString())
        }

        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val i = getItem(position)
        var v: View? = convertView

        if (v == null || v !is LinearLayout) {
            val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = vi.inflate(R.layout.listitem_main, null)
        }

        val t = v!!.findViewById(R.id.text) as TextView
        val img = v.findViewById(R.id.icon) as ImageView
        t.text = i.userName
        img.visibility = View.VISIBLE

        val bitmap = userpics.getImageMemory(Integer.toString(i.userID))
        if (bitmap != null) {
            img.setImageBitmap(bitmap)
        } else {
            img.setImageResource(R.drawable.ic_user_32)
            val task = ImageLoaderTask(userpics, img, true)
            task.execute(Integer.toString(i.userID), "http://i.juick.com/as/" + i.userID + ".png")
        }

        val unread = v.findViewById(R.id.unreadMessages) as TextView
        if (i.unreadMessages > 0) {
            unread.text = Integer.toString(i.unreadMessages)
            unread.visibility = View.VISIBLE
        } else {
            unread.visibility = View.GONE
        }

        return v
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }
}

internal class ChatsAdapterItem {

    var userName: String? = null
    var userID = 0
    var unreadMessages = 0
}
