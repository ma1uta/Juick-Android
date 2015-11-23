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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.juick.GCMIntentService
import com.juick.R
import java.net.URLEncoder
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.LocalBroadcastManager

/**

 * @author ugnich
 */
class PMActivity : AppCompatActivity(), PMFragment.PMFragmentListener, View.OnClickListener {
    private var uname: String? = null
    private var uid: Int = 0
    private var etMessage: EditText? = null
    private var bSend: Button? = null
    private val mMessageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            (context.getSystemService(Activity.VIBRATOR_SERVICE) as Vibrator).vibrate(250)
            val message = intent.getStringExtra("message")
            if (message[0] == '{') {
                (getSupportFragmentManager().findFragmentByTag(PMFRAGMENTID) as PMFragment).onNewMessages("[$message]")
            } else {
                (getSupportFragmentManager().findFragmentByTag(PMFRAGMENTID) as PMFragment).onNewMessages(message)
            }
        }
    }

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uname = getIntent().getStringExtra("uname")
        uid = getIntent().getIntExtra("uid", 0)

        val bar = getSupportActionBar()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setTitle(uname)

        setContentView(R.layout.pm)

        etMessage = findViewById(R.id.editMessage) as EditText
        bSend = findViewById(R.id.buttonSend) as Button
        bSend!!.setOnClickListener(this)

        val ft = getSupportFragmentManager().beginTransaction()
        val pf = PMFragment()
        val args = Bundle()
        args.putString("uname", uname)
        args.putInt("uid", uid)
        pf.setArguments(args)
        ft.add(R.id.pmfragment, pf, PMFRAGMENTID)
        ft.commit()
    }

    override fun onClick(view: View) {
        if (view === bSend) {
            val msg = etMessage!!.text.toString()
            if (msg.length > 0) {
                postText(msg)
            } else {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun postText(body: String) {
        val thr = Thread(object : Runnable {

            override fun run() {
                try {
                    val ret = postJSON(this@PMActivity, "https://api.juick.com/pm", "uname=" + uname + "&body=" + URLEncoder.encode(body, "utf-8"))
                    this@PMActivity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (ret != null) {
                                etMessage!!.setText("")
                                (getSupportFragmentManager().findFragmentByTag(PMFRAGMENTID) as PMFragment).onNewMessages("[$ret]")
                            } else {
                                Toast.makeText(this@PMActivity, R.string.Error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("postPM", e.toString())
                }

            }
        })
        thr.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        val spe = PreferenceManager.getDefaultSharedPreferences(this).edit()
        if (hasFocus) {
            spe.putString("currentactivity", "pm-" + uid)
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, IntentFilter(GCMIntentService.GCMEVENTACTION))
        } else {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
            spe.remove("currentactivity")
        }
        spe.commit()
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null)
            return false
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val PMFRAGMENTID = "PMFRAGMENT"
    }
}
class AppCompatActivity {

}