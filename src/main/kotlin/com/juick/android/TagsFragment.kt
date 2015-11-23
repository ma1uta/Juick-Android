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

// import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import org.json.JSONArray

/**

 * @author Ugnich Anton
 */
class TagsFragment : ListFragment(), OnItemClickListener, OnItemLongClickListener {

    private var parentActivity: TagsFragmentListener? = null
    private var uid = 0

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            parentActivity = activity as TagsFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement TagsFragmentListener")
        }

    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        if (args != null) {
            uid = args.getInt("uid", 0)
        }

        listView.onItemClickListener = this
        listView.onItemLongClickListener = this

        val thr = Thread(object : Runnable {

            override fun run() {
                var url = "https://api.juick.com/tags"
                if (uid != 0) {
                    url += "?user_id=" + uid
                }
                val jsonStr = Utils.getJSON(activity, url)
                if (isAdded) {
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (jsonStr != null) {
                                try {
                                    val listAdapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1)

                                    val json = JSONArray(jsonStr)
                                    val cnt = json.length()
                                    for (i in 0..cnt - 1) {
                                        listAdapter.add(json.getJSONObject(i).getString("tag"))
                                    }
                                    setListAdapter(listAdapter)
                                } catch (e: Exception) {
                                    Log.e("initTagsAdapter", e.toString())
                                }

                            }
                        }
                    })
                }
            }
        })
        thr.start()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        parentActivity!!.onTagClick(listAdapter.getItem(position) as String)
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        parentActivity!!.onTagLongClick(listAdapter.getItem(position) as String)
        return true
    }

    interface TagsFragmentListener {

        fun onTagClick(tag: String)

        fun onTagLongClick(tag: String)
    }
}
