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

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Base64
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.juick.R
import java.net.HttpURLConnection
import java.net.URL

/**

 * @author Ugnich Anton
 */
class SignInActivity : Activity(), OnClickListener {

    private var etNick: EditText? = null
    private var etPassword: EditText? = null
    private var bSave: Button? = null
    // private val bCancel: Button? = null
    private val handlErrToast = object : Handler() {

        override fun handleMessage(msg: Message) {
            Toast.makeText(this@SignInActivity, R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.signin)

        etNick = findViewById(R.id.juickNick) as EditText
        etPassword = findViewById(R.id.juickPassword) as EditText
        bSave = findViewById(R.id.buttonSave) as Button

        bSave!!.setOnClickListener(this)

        if (hasAuth(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setNeutralButton(R.string.OK, object : android.content.DialogInterface.OnClickListener {

                override fun onClick(arg0: DialogInterface, arg1: Int) {
                    setResult(Activity.RESULT_CANCELED)
                    this@SignInActivity.finish()
                }
            })
            builder.setMessage(R.string.Only_one_account)
            builder.show()
        }
    }

    override fun onClick(view: View) {

        val nick = etNick!!.text.toString()
        val password = etPassword!!.text.toString()

        if (nick.length == 0 || password.length == 0) {
            Toast.makeText(this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show()

        val thr = Thread(object : Runnable {

            override fun run() {
                var status = 0
                try {
                    val authStr = nick + ":" + password
                    val basicAuth = "Basic " + Base64.encodeToString(authStr.toByteArray(), Base64.NO_WRAP)

                    val apiUrl = URL("https://api.juick.com/post")
                    val conn = apiUrl.openConnection() as HttpURLConnection
                    conn.useCaches = false
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", basicAuth)
                    conn.connect()
                    status = conn.responseCode
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e("checkingNickPassw", e.toString())
                }

                if (status == 400) {
                    val account = Account(nick, getString(R.string.com_juick))
                    val am = AccountManager.get(this@SignInActivity)
                    val accountCreated = am.addAccountExplicitly(account, password, null)
                    val extras = intent.extras
                    if (extras != null && accountCreated) {
                        val response = extras.getParcelable<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
                        val result = Bundle()
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick)
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick))
                        response.onResult(result)
                    }

                    this@SignInActivity.setResult(Activity.RESULT_OK)
                    this@SignInActivity.finish()
                } else {
                    handlErrToast.sendEmptyMessage(0)
                }
            }
        })
        thr.start()
    }
}
