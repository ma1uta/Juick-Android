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

import android.app.Activity
import android.content.Context
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import com.juick.R
import org.jetbrains.anko.async
import java.net.URLEncoder

/**

 * @author Ugnich Anton
 */

val JUICK_TAG = "Juick.com"
val ACTIVITY_SIGNIN = 2
val ACTIVITY_PREFERENCES = 3

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val intent = intent
        if (intent != null) {
            val uri = intent.data
            if (uri != null && uri.pathSegments.size > 0 && parseUri(uri)) {
                return
            }
        }

        if (!hasAuth(this)) {
            startActivityForResult(Intent(this, SignInActivity::class.java), ACTIVITY_SIGNIN)
            return
        }

        val context = this as Context

        async {
            try {
                val instanceID = InstanceID.getInstance(context)
                val token = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val prefRegId = sp.getString("gcm_regid", null)
                if (token.length == 0 || token != prefRegId) {
                    val res = getJSON(context, "https://api.juick.com/android/register?regid=" + URLEncoder.encode(token, "UTF-8"))
                    if (res != null) {
                        val spe = PreferenceManager.getDefaultSharedPreferences(context).edit()
                        spe.putString("gcm_regid", token)
                        spe.commit()
                    }
                    Log.d(JUICK_TAG,    "GCM token $(token)")
                }
            } catch (e: Exception) {
                Log.e(JUICK_TAG, e.toString())
            }
        }

        setContentView(R.layout.main)
        val toolbar = findViewById(R.id.my_awesome_toolbar) as Toolbar
        toolbar.setLogo(R.drawable.ic_logo)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        val viewPager = findViewById(R.id.viewpager) as ViewPager
        viewPager.adapter = MessagesFragmentPagerAdapter(supportFragmentManager, this@MainActivity)

        val tabLayout = findViewById(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_SIGNIN) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = intent
                finish()
                startActivity(intent)
            } else {
                finish()
            }
        } else if (requestCode == ACTIVITY_PREFERENCES) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = intent
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
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
}
