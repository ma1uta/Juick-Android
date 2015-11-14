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

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
// import android.support.v4.app.FragmentTransaction
import com.juick.R

/**

 * @author Ugnich Anton
 */
class TagsActivity : FragmentActivity(), TagsFragment.TagsFragmentListener {

    private var uid: Int = 0

    override protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        uid = intent.getIntExtra("uid", 0)

        if (uid == 0) {
            setTitle(R.string.Popular_tags)
        }

        setContentView(R.layout.tags)

        val ft = supportFragmentManager.beginTransaction()
        val tf = TagsFragment()
        val args = Bundle()
        args.putInt("uid", uid)
        tf.arguments = args
        ft.add(R.id.tagsfragment, tf)
        ft.commit()
    }

    override fun onTagClick(tag: String) {
        val i = Intent()
        i.putExtra("tag", tag)
        setResult(RESULT_OK, i)
        finish()
    }

    override fun onTagLongClick(tag: String) {
        val i = Intent(this, MessagesActivity::class.java)
        i.putExtra("tag", tag)
        i.putExtra("uid", uid)
        startActivity(i)
    }
}
