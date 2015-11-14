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

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.FragmentActivity
// import android.support.v4.app.FragmentTransaction
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.juick.R
import java.net.URLEncoder

/**

 * @author Ugnich Anton
 */
class ThreadActivity : FragmentActivity(), View.OnClickListener, ThreadFragment.ThreadFragmentListener {
    private var tvReplyTo: TextView? = null
    private var etMessage: EditText? = null
    private var bSend: Button? = null
    private var bAttach: ImageButton? = null
    private var mid = 0
    private var rid = 0
    private var attachmentUri: String? = null
    private var attachmentMime: String? = null
    private var progressDialog: ProgressDialog? = null
    private val progressDialogCancel = NewMessageActivity.BooleanReference(false)
    private val progressHandler = object : Handler() {

        override fun handleMessage(msg: Message) {
            if (progressDialog!!.max < msg.what) {
                progressDialog!!.max = msg.what
            } else {
                progressDialog!!.progress = msg.what
            }
        }
    }

    override protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        val i = intent
        mid = i.getIntExtra("mid", 0)
        if (mid == 0) {
            finish()
        }

        setContentView(R.layout.thread)
        tvReplyTo = findViewById(R.id.textReplyTo) as TextView
        etMessage = findViewById(R.id.editMessage) as EditText
        bSend = findViewById(R.id.buttonSend) as Button
        bSend!!.setOnClickListener(this)
        bAttach = findViewById(R.id.buttonAttachment) as ImageButton
        bAttach!!.setOnClickListener(this)

        val ft = supportFragmentManager.beginTransaction()
        val tf = ThreadFragment()
        val args = Bundle()
        args.putInt("mid", mid)
        tf.arguments = args
        ft.add(R.id.threadfragment, tf)
        ft.commit()
    }

    private fun resetForm() {
        rid = 0
        tvReplyTo!!.visibility = View.GONE
        etMessage!!.setText("")
        attachmentMime = null
        attachmentUri = null
        bAttach!!.setSelected(false)
        setFormEnabled(true)
    }

    private fun setFormEnabled(state: Boolean) {
        etMessage!!.setEnabled(state)
        bSend!!.setEnabled(state)
    }

    override fun onThreadLoaded(uid: Int, nick: String) {
        title = "@" + nick
    }

    override fun onReplySelected(rid: Int, txt: String) {
        if (rid > 0) {
            val ssb = SpannableStringBuilder()
            val inreplyto = resources.getString(R.string.In_reply_to_) + " "
            ssb.append(inreplyto + txt)
            ssb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, inreplyto.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvReplyTo!!.text = ssb
            tvReplyTo!!.visibility = View.VISIBLE
        } else {
            tvReplyTo!!.visibility = View.GONE
        }
    }

    override fun onClick(view: View) {
        if (view === bAttach) {
            if (attachmentUri == null) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE)
            } else {
                attachmentUri = null
                attachmentMime = null
                bAttach!!.setSelected(false)
            }
        } else if (view === bSend) {
            val msg = etMessage!!.text.toString()
            if (msg.length < 3) {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show()
                return
            }
            //        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

            var msgnum = "#" + mid
            if (rid > 0) {
                msgnum += "/" + rid
            }
            val body = msgnum + " " + msg

            setFormEnabled(false)

            if (attachmentUri == null) {
                postText(body)
            } else {
                postMedia(body)
            }
        }
    }

    fun postText(body: String) {
        val thr = Thread(object : Runnable {

            override fun run() {
                try {
                    val ret = Utils.postJSON(this@ThreadActivity, "https://api.juick.com/post", "body=" + URLEncoder.encode(body, "utf-8"))
                    this@ThreadActivity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (ret != null) {
                                Toast.makeText(this@ThreadActivity, R.string.Message_posted, Toast.LENGTH_SHORT).show()
                                resetForm()
                            } else {
                                Toast.makeText(this@ThreadActivity, R.string.Error, Toast.LENGTH_SHORT).show()
                                setFormEnabled(true)
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("postComment", e.toString())
                }

            }
        })
        thr.start()
    }

    fun postMedia(body: String) {
        progressDialog = ProgressDialog(this)
        progressDialogCancel.bool = false
        progressDialog!!.setOnCancelListener(object : DialogInterface.OnCancelListener {

            override fun onCancel(arg0: DialogInterface) {
                this@ThreadActivity.progressDialogCancel.bool = true
            }
        })
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog!!.max = 0
        progressDialog!!.show()
        val thr = Thread(object : Runnable {

            override fun run() {
                val res = NewMessageActivity.sendMessage(this@ThreadActivity, body, 0.0, 0.0, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel)
                this@ThreadActivity.runOnUiThread(object : Runnable {

                    override fun run() {
                        if (progressDialog != null) {
                            progressDialog!!.dismiss()
                        }
                        setFormEnabled(true)
                        if (res) {
                            resetForm()
                        }
                        if (res && attachmentUri == null) {
                            Toast.makeText(this@ThreadActivity, R.string.Message_posted, Toast.LENGTH_LONG).show()
                        } else {
                            val builder = AlertDialog.Builder(this@ThreadActivity)
                            builder.setNeutralButton(R.string.OK, null)
                            if (res) {
                                builder.setIcon(android.R.drawable.ic_dialog_info)
                                builder.setMessage(R.string.Message_posted)
                            } else {
                                builder.setIcon(android.R.drawable.ic_dialog_alert)
                                builder.setMessage(R.string.Error)
                            }
                            builder.show()
                        }
                    }
                })
            }
        })
        thr.start()
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = data.dataString
                // How to get correct mime type?
                attachmentMime = "image/jpeg"
                bAttach!!.setSelected(true)
            }
        }
    }

    companion object {

        val ACTIVITY_ATTACHMENT_IMAGE = 2
    }
}
