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

// import android.content.Intent
// import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
// import android.support.v4.app.FragmentTransaction
import com.juick.R

/**

 * @author Ugnich Anton
 */
class MessagesActivity : FragmentActivity() {

    override protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        val i = intent
        var uid = i.getIntExtra("uid", 0)
        var uname = i.getStringExtra("uname")
        val search = i.getStringExtra("search")
        val tag = i.getStringExtra("tag")
        val place_id = i.getIntExtra("place_id", 0)
        val home = i.getBooleanExtra("home", false)
        val popular = i.getBooleanExtra("popular", false)
        val media = i.getBooleanExtra("media", false)

        if (i.data != null) {
            val cursor = contentResolver.query(intent.data, null, null, null, null);
            // val cursor = managedQuery(getIntent().getData(), null, null, null, null)
            if (cursor != null && cursor.moveToNext()) {
                uid = cursor.getInt(cursor.getColumnIndex("DATA1"))
                uname = cursor.getString(cursor.getColumnIndex("DATA2"))
            }
        }

        if (uid > 0 && uname != null) {
            title = "@" + uname
        } else if (search != null) {
            title = resources.getString(R.string.Search) + ": " + search
        } else if (tag != null) {
            var title = resources.getString(R.string.Tag) + ": " + tag
            if (uid == -1) {
                title += " (" + resources.getString(R.string.Your_messages) + ")"
            }
            setTitle(title)
        } else if (place_id > 0) {
            title = "Location"
        } else if (home) {
            title = resources.getString(R.string.Subscriptions)
        } else if (popular) {
            title = resources.getString(R.string.Top_messages)
        } else if (media) {
            title = resources.getString(R.string.With_photos)
        } else {
            title = resources.getString(R.string.Last_messages)
        }

        setContentView(R.layout.messages)
        val ft = supportFragmentManager.beginTransaction()
        val mf = MessagesFragment()
        val args = Bundle()

        args.putInt("uid", uid)
        args.putString("uname", uname)
        args.putString("search", search)
        args.putString("tag", tag)
        args.putInt("place_id", place_id)
        args.putBoolean("home", home)
        args.putBoolean("popular", popular)
        args.putBoolean("media", media)

        mf.arguments = args
        ft.replace(R.id.messagesfragment, mf)
        ft.commit()
    }
}
