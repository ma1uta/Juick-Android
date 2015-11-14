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

import com.actionbarsherlock.app.ActionBar.Tab
import com.juick.GCMIntentService
import android.content.Intent
// import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.util.Log
import com.actionbarsherlock.app.ActionBar
import com.actionbarsherlock.app.SherlockFragment
import com.actionbarsherlock.app.SherlockFragmentActivity
import com.actionbarsherlock.view.Menu
// import com.actionbarsherlock.view.MenuInflater
import com.actionbarsherlock.view.MenuItem
import com.google.android.gcm.GCMRegistrar
import com.juick.R

/**

 * @author Ugnich Anton
 */
class MainActivity : SherlockFragmentActivity(), ActionBar.TabListener {
    private var fChats: Fragment? = null
    private var fMessages: Fragment? = null
    private var fExplore: Fragment? = null

    override protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        val intent = intent
        if (intent != null) {
            val uri = intent.data
            if (uri != null && uri.pathSegments.size > 0 && parseUri(uri)) {
                return
            }
        }

        if (!Utils.hasAuth(this)) {
            startActivityForResult(Intent(this, SignInActivity::class.java), ACTIVITY_SIGNIN)
            return
        }

        try {
            GCMRegistrar.checkDevice(this)
            GCMRegistrar.checkManifest(this)
            val regId = GCMRegistrar.getRegistrationId(this)
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val prefRegId = sp.getString("gcm_regid", null)
            if (regId.length == 0 || regId != prefRegId) {
                GCMRegistrar.register(this, GCMIntentService.SENDER_ID)
            }
        } catch (e: Exception) {
            Log.e("Juick.GCM", e.toString())
        }

        val bar = supportActionBar
        bar.setHomeButtonEnabled(false)
        bar.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        var tab: Tab
        tab = bar.newTab().setTag("c").setText("Chats").setTabListener(this)
        bar.addTab(tab)
        tab = bar.newTab().setTag("f").setText("Feed").setTabListener(this)
        bar.addTab(tab)
        tab = bar.newTab().setTag("s").setText("Search").setTabListener(this)
        bar.addTab(tab)
    }

    override fun onTabReselected(tab: Tab, ft: FragmentTransaction) {
    }

    override fun onTabSelected(tab: Tab, ft: FragmentTransaction) {
        val tag = tab.tag.toString()
        if (tag == "c") {
            if (fChats == null) {
                fChats = SherlockFragment.instantiate(this, ChatsFragment::class.java.name)
                ft.add(android.R.id.content, fChats, "c")
            } else {
                ft.attach(fChats)
            }
        } else if (tag == "f") {
            if (fMessages == null) {
                val b = Bundle()
                b.putBoolean("home", true)
                b.putBoolean("usecache", true)
                fMessages = SherlockFragment.instantiate(this, MessagesFragment::class.java.name, b)
                ft.add(android.R.id.content, fMessages, "m")
            } else {
                ft.attach(fMessages)
            }
        } else {
            if (fExplore == null) {
                fExplore = SherlockFragment.instantiate(this, ExploreFragment::class.java.name)
                ft.add(android.R.id.content, fExplore, "e")
            } else {
                ft.attach(fExplore)
            }
        }
    }

    override fun onTabUnselected(tab: Tab, ft: FragmentTransaction) {
        val tag = tab.tag.toString()
        if (tag == "c") {
            if (fChats != null) {
                ft.detach(fChats)
            }
        } else if (tag == "f") {
            if (fMessages != null) {
                ft.detach(fMessages)
                fMessages = null // ANDROID BUG
            }
        } else {
            if (fExplore != null) {
                ft.detach(fExplore)
            }
        }
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_SIGNIN) {
            if (resultCode == RESULT_OK) {
                val intent = intent
                finish()
                startActivity(intent)
            } else {
                finish()
            }
        } else if (requestCode == ACTIVITY_PREFERENCES) {
            if (resultCode == RESULT_OK) {
                val intent = intent
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = supportMenuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuitem_preferences -> {
                startActivityForResult(Intent(this, PreferencesActivity::class.java), ACTIVITY_PREFERENCES)
                return true
            }
            R.id.menuitem_newmessage -> {
                startActivity(Intent(this, NewMessageActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun parseUri(uri: Uri): Boolean {
        val segs = uri.pathSegments
        if ((segs.size == 1 && segs[0].matches("\\A[0-9]+\\z".toRegex())) || (segs.size == 2 && segs[1].matches("\\A[0-9]+\\z".toRegex()) && segs[0] != "places")) {
            val mid = Integer.parseInt(segs[segs.size - 1])
            if (mid > 0) {
                finish()
                val intent = Intent(this, ThreadActivity::class.java)
                intent.setData(null)
                intent.putExtra("mid", mid)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }
        } else if (segs.size == 1 && segs[0].matches("\\A[a-zA-Z0-9\\-]+\\z".toRegex())) {
            //TODO show user
        }
        return false
    }

    companion object {

        val ACTIVITY_SIGNIN = 2
        val ACTIVITY_PREFERENCES = 3
        // val PENDINGINTENT_CONSTANT = 713242183
    }
}
