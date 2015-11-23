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
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.juick.R
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**

 * @author Ugnich Anton
 */
class NewMessageActivity : AppCompatActivity(), OnClickListener {
    private var etMessage: EditText? = null
    private var bTags: ImageButton? = null
    private var bLocation: ImageButton? = null
    private var bAttachment: ImageButton? = null
    private var lat = 0.0
    private var lon = 0.0
    private var attachmentUri: String? = null
    private var attachmentMime: String? = null
    private var progressDialog: ProgressDialog? = null
    private val progressDialogCancel = BooleanReference(false)
    private val progressHandler = object : Handler() {

        override fun handleMessage(msg: Message) {
            if (progressDialog!!.max < msg.what) {
                progressDialog!!.max = msg.what
            } else {
                progressDialog!!.progress = msg.what
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        val bar = supportActionBar
        bar.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.New_message)

        setContentView(R.layout.newmessage)

        etMessage = findViewById(R.id.editMessage) as EditText
        bTags = findViewById(R.id.buttonTags) as ImageButton
        bLocation = findViewById(R.id.buttonLocation) as ImageButton
        bAttachment = findViewById(R.id.buttonAttachment) as ImageButton

        bTags!!.setOnClickListener(this)
        bLocation!!.setOnClickListener(this)
        bAttachment!!.setOnClickListener(this)

        resetForm()
        handleIntent(intent)
    }

    private fun resetForm() {
        etMessage!!.setText("")
        bLocation!!.isSelected = false
        bAttachment!!.isSelected = false
        lat = 0.0
        lon = 0.0
        attachmentUri = null
        attachmentMime = null
        progressDialog = null
        progressDialogCancel.bool = false
        etMessage!!.requestFocus()
        setSupportProgressBarIndeterminateVisibility(java.lang.Boolean.FALSE!!)

        /*
        setProgressBarIndeterminateVisibility(true);
        Thread thr = new Thread(new Runnable() {
        
        public void run() {
        String jsonUrl = "http://api.juick.com/postform";
        
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (loc != null) {
        jsonUrl += "?lat=" + loc.getLatitude() + "&lon=" + loc.getLongitude() + "&acc=" + loc.getAccuracy() + "&fixage=" + Math.round((System.currentTimeMillis() - loc.getTime()) / 1000);
        }
        
        final String jsonStr = getJSON(NewMessageActivity.this, jsonUrl);
        
        NewMessageActivity.this.runOnUiThread(new Runnable() {
        
        public void run() {
        if (jsonStr != null) {
        
        try {
        JSONObject json = new JSONObject(jsonStr);
        if (json.has("facebook")) {
        etTo.setText(etTo.getText() + ", Facebook");
        }
        if (json.has("twitter")) {
        etTo.setText(etTo.getText() + ", Twitter");
        }
        if (json.has("place")) {
        JSONObject jsonPlace = json.getJSONObject("place");
        pidHint = jsonPlace.getInt("pid");
        bLocationHint.setVisibility(View.VISIBLE);
        bLocationHint.setText(jsonPlace.getString("name"));
        }
        } catch (JSONException e) {
        System.err.println(e);
        }
        }
        NewMessageActivity.this.setProgressBarIndeterminateVisibility(false);
        }
        });
        }
        });
        thr.start();
         */
    }

    private fun setFormEnabled(state: Boolean) {
        etMessage!!.isEnabled = state
        bTags!!.isEnabled = state
        bLocation!!.isEnabled = state
        bAttachment!!.isEnabled = state
        setSupportProgressBarIndeterminateVisibility(if (state) java.lang.Boolean.FALSE else java.lang.Boolean.TRUE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetForm()
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent) {
        val action = i.action
        if (action != null && action == Intent.ACTION_SEND) {
            val mime = i.type
            val extras = i.extras
            if (mime == "text/plain") {
                etMessage!!.append(extras.getString(Intent.EXTRA_TEXT))
            } else if (mime == "image/jpeg" || mime == "image/png") {
                attachmentUri = extras.get(Intent.EXTRA_STREAM)!!.toString()
                attachmentMime = mime
                bAttachment!!.isSelected = true
            }
        }
    }

    override fun onClick(v: View?) {
        if (v === bTags) {
            val i = Intent(this, TagsActivity::class.java)
            i.setAction(Intent.ACTION_PICK)
            i.putExtra("uid", (-1).toInt())
            startActivityForResult(i, ACTIVITY_TAGS)

        } else if (v === bLocation) {
            if (lat == 0.0 && lon == 0.0) {
                startActivityForResult(Intent(this, PickLocationActivity::class.java), ACTIVITY_LOCATION)
            } else {
                lat = 0.0
                lon = 0.0
                bLocation!!.isSelected = false
            }
        } else if (v === bAttachment) {
            if (attachmentUri == null) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE)
            } else {
                attachmentUri = null
                attachmentMime = null
                bAttachment!!.isSelected = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.newmessage, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuitem_send) {
            val msg = etMessage!!.text.toString()
            if (msg.length < 3) {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show()
                return false
            }
            setFormEnabled(false)
            if (attachmentUri != null) {
                progressDialog = ProgressDialog(this)
                progressDialogCancel.bool = false
                progressDialog!!.setOnCancelListener(object : DialogInterface.OnCancelListener {

                    override fun onCancel(arg0: DialogInterface) {
                        this@NewMessageActivity.progressDialogCancel.bool = true
                    }
                })
                progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                progressDialog!!.max = 0
                progressDialog!!.show()
            }
            val thr = Thread(object : Runnable {

                override fun run() {
                    val res = sendMessage(this@NewMessageActivity, msg, lat, lon, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel)
                    this@NewMessageActivity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (progressDialog != null) {
                                progressDialog!!.dismiss()
                            }
                            setFormEnabled(true)
                            if (res) {
                                resetForm()
                            }
                            if ((res && attachmentUri == null) || this@NewMessageActivity.isFinishing) {
                                Toast.makeText(this@NewMessageActivity, if (res) R.string.Message_posted else R.string.Error, Toast.LENGTH_LONG).show()
                            } else {
                                val builder = AlertDialog.Builder(this@NewMessageActivity)
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
            return true
        } else if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ACTIVITY_TAGS) {
                etMessage!!.setText("*" + data!!.getStringExtra("tag") + " " + etMessage!!.text)
            } else if (requestCode == ACTIVITY_LOCATION) {
                lat = data!!.getDoubleExtra("lat", 0.0)
                lon = data.getDoubleExtra("lon", 0.0)
                if (lat != 0.0 && lon != 0.0) {
                    bLocation!!.isSelected = true
                }
            } else if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = data.dataString
                // How to get correct mime type?
                attachmentMime = "image/jpeg"
                bAttachment!!.isSelected = true
            }
        }
    }

    class BooleanReference(var bool: Boolean)

    companion object {

        private val ACTIVITY_LOCATION = 1
        val ACTIVITY_ATTACHMENT_IMAGE = 2
        private val ACTIVITY_TAGS = 4

        fun sendMessage(context: Context, txt: String, lat: Double, lon: Double, attachmentUri: String?, attachmentMime: String?, progressDialog: ProgressDialog?, progressHandler: Handler, progressDialogCancel: BooleanReference): Boolean {
            try {
                val end = "\r\n"
                val twoHyphens = "--"
                val boundary = "****+++++******+++++++********"

                val apiUrl = URL("https://api.juick.com/post")
                val conn = apiUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.doOutput = true
                conn.useCaches = false
                conn.requestMethod = "POST"
                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.setRequestProperty("Charset", "UTF-8")
                conn.setRequestProperty("Authorization", getBasicAuthString(context))
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)

                var outStr = twoHyphens + boundary + end
                outStr += "Content-Disposition: form-data; name=\"body\"$end$end$txt$end"

                if (lat != 0.0 && lon != 0.0) {
                    outStr += twoHyphens + boundary + end
                    outStr += "Content-Disposition: form-data; name=\"lat\"" + end + end + lat.toString() + end
                    outStr += twoHyphens + boundary + end
                    outStr += "Content-Disposition: form-data; name=\"lon\"" + end + end + lon.toString() + end
                }

                if (attachmentUri != null && attachmentUri.length > 0 && attachmentMime != null) {
                    var fname = "file."
                    if (attachmentMime == "image/jpeg") {
                        fname += "jpg"
                    } else if (attachmentMime == "image/png") {
                        fname += "png"
                    }
                    outStr += twoHyphens + boundary + end
                    outStr += "Content-Disposition: form-data; name=\"attach\"; filename=\"$fname\"$end$end"
                }
                val outStrB = outStr.toByteArray("utf-8")

                val outStrEnd = twoHyphens + boundary + twoHyphens + end
                val outStrEndB = outStrEnd.toByteArray()

                var size = outStrB.size + outStrEndB.size

                var fileInput: FileInputStream? = null
                if (attachmentUri != null && attachmentUri.length > 0) {
                    fileInput = context.contentResolver.openAssetFileDescriptor(Uri.parse(attachmentUri), "r").createInputStream()
                    size += fileInput!!.available()
                    size += 2 // \r\n (end)
                }

                if (progressDialog != null) {
                    val fsize = size
                    progressHandler.sendEmptyMessage(fsize)
                }

                conn.setFixedLengthStreamingMode(size)
                conn.connect()
                val out = conn.outputStream
                out.write(outStrB)

                if (attachmentUri != null && attachmentUri.length > 0 && fileInput != null) {
                    val buffer = ByteArray(4096)
                    var total = 0
                    var totallast = 0
                    var len = fileInput.read(buffer, 0, 4096)
                    while (len != -1 && progressDialogCancel.bool == false) {
                        out.write(buffer, 0, len)
                        total += len
                        if (((total / 102400).toInt()) != totallast) {
                            totallast = (total / 102400).toInt()
                            progressHandler.sendEmptyMessage(total)
                        }
                        len = fileInput.read(buffer, 0, 4096)
                    }
                    if (progressDialogCancel.bool == false) {
                        out.write(end.toByteArray())
                    }
                    fileInput.close()
                    progressHandler.sendEmptyMessage(size)
                }
                if (progressDialogCancel.bool == false) {
                    out.write(outStrEndB)
                    out.flush()
                }
                out.close()

                if (progressDialogCancel.bool) {
                    return false
                } else {
                    return (conn.responseCode == 200)
                }
            } catch (e: Exception) {
                Log.e("sendOpinion", e.toString())
            }

            return false
        }
    }
}
