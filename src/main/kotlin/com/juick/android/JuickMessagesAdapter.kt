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

import com.juick.android.api.JuickMessage
import android.content.Context
import android.content.SharedPreferences
// import android.graphics.Bitmap
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.juick.R
// import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
// import java.util.regex.Matcher
import java.util.regex.Pattern
import org.json.JSONArray

/**

 * @author Ugnich Anton
 */
class JuickMessagesAdapter(context: Context, private val type: Int) : ArrayAdapter<JuickMessage>(context, R.layout.listitem_juickmessage) {
    //    public static Pattern usrPattern = Pattern.compile("@[a-zA-Z0-9\\-]{2,16}");
    private val userpics: ImageCache
    private val photos: ImageCache
    private var usenetwork = false
    private val sp: SharedPreferences
    private var textSize: Float = 0.toFloat()

    init {

        sp = PreferenceManager.getDefaultSharedPreferences(context)
        val textScaleStr = sp.getString(PREFERENCES_FONTSIZE, "16")
        try {
            textSize = java.lang.Float.parseFloat(textScaleStr)
        } catch (e: Exception) {
            textSize = 16f
        }

        val loadphotos = sp.getString("loadphotos", "Always")
        if (loadphotos[0] == 'A' || (loadphotos[0] == 'W' && Utils.isWiFiConnected(context))) {
            usenetwork = true
        }

        photos = ImageCache(context, "photos-small", 1024 * 1024 * 5)
        userpics = ImageCache(context, "userpics-small", 1024 * 1024 * 2)
    }

    fun parseJSON(jsonStr: String): Int {
        try {
            val json = JSONArray(jsonStr)
            val cnt = json.length()
            for (i in 0..cnt - 1) {
                add(JuickMessage.parseJSON(json.getJSONObject(i)))
            }
            return cnt
        } catch (e: Exception) {
            Log.e("initOpinionsAdapter", e.toString())
        }

        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View? {
        val jmsg = getItem(position)
        var v: View? = convertView

        if (jmsg.User != null && jmsg.Text != null) {
            if (v == null || v !is LinearLayout) {
                val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = vi.inflate(R.layout.listitem_juickmessage, null)
                (v.findViewById(R.id.text) as TextView).textSize = textSize
                if (type == TYPE_THREAD) {
                    v.findViewById(R.id.comment).visibility = View.GONE
                    v.findViewById(R.id.replies).visibility = View.GONE
                }
            }

            val upic = v?.findViewById(R.id.userpic) as ImageView
            val bitmapupic = photos.getImageMemory(Integer.toString(jmsg.User!!.UID))
            if (bitmapupic != null) {
                upic.setImageBitmap(bitmapupic)
            } else {
                upic.setImageResource(R.drawable.ic_user_32)
                val task = ImageLoaderTask(userpics, upic, usenetwork)
                task.execute(Integer.toString(jmsg.User!!.UID), "https://i.juick.com/as/" + jmsg.User!!.UID + ".png")
            }

            val username = v?.findViewById(R.id.username) as TextView
            username.text = jmsg.User!!.UName

            val timestamp = v?.findViewById(R.id.timestamp) as TextView
            timestamp.text = formatMessageTimestamp(jmsg)

            val t = v?.findViewById(R.id.text) as TextView
            if (type == TYPE_THREAD && jmsg.RID == 0) {
                t.text = formatFirstMessageText(jmsg)
            } else {
                t.text = formatMessageText(jmsg)
            }

            val p = v?.findViewById(R.id.photo) as ImageView
            if (jmsg.Photo != null) {
                val key = Integer.toString(jmsg.MID) + "-" + Integer.toString(jmsg.RID)
                val bitmap = photos.getImageMemory(key)
                if (bitmap != null) {
                    p.setImageBitmap(bitmap)
                } else {
                    p.setImageResource(R.drawable.ic_attach_photo)
                    val task = ImageLoaderTask(photos, p, usenetwork)
                    task.execute(key, jmsg.Photo)
                }
                p.visibility = View.VISIBLE
            } else {
                p.visibility = View.GONE
            }

            if (jmsg.replies > 0 && type != TYPE_THREAD) {
                val replies = v?.findViewById(R.id.replies) as TextView
                replies.visibility = View.VISIBLE
                replies.text = Integer.toString(jmsg.replies)
            } else {
                v?.findViewById(R.id.replies)?.visibility = View.GONE
            }

        } else {
            if (v == null || v !is TextView) {
                val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = vi.inflate(R.layout.preference_category, null)
            }

            (v as TextView?)?.textSize = textSize

            if (jmsg.Text != null) {
                v?.text = jmsg.Text
            } else {
                v?.text = ""
            }
        }

        return v
    }

    override fun isEnabled(position: Int): Boolean {
        val jmsg = getItem(position)
        return (jmsg != null && jmsg.User != null && jmsg.MID > 0)
    }

    fun addDisabledItem(txt: String, position: Int) {
        val jmsg = JuickMessage()
        jmsg.Text = txt
        insert(jmsg, position)
    }

    private fun formatMessageTimestamp(jmsg: JuickMessage): String {
        val df = SimpleDateFormat("HH:mm dd/MMM/yy")
        df.timeZone = TimeZone.getDefault()
        return df.format(jmsg.Timestamp)
    }

    private fun formatMessageText(jmsg: JuickMessage): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        ssb.append(jmsg.Text)

        // Highlight links http://example.com/
        var pos = 0
        var m = urlPattern.matcher(jmsg.Text)
        while (m.find(pos)) {
            ssb.setSpan(ForegroundColorSpan(-16777012), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            pos = m.end()
        }

        // Highlight messages #1234
        pos = 0
        m = msgPattern.matcher(jmsg.Text)
        while (m.find(pos)) {
            ssb.setSpan(ForegroundColorSpan(-16777012), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            pos = m.end()
        }

        /*
        // Highlight usernames @username
        pos = 0;
        m = usrPattern.matcher(txt);
        while (m.find(pos)) {
        ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        pos = m.end();
        }
         */

        return ssb
    }

    private fun formatFirstMessageText(jmsg: JuickMessage): SpannableStringBuilder {
        val ssb = formatMessageText(jmsg)
        val tags = jmsg.getTags()
        if (tags.length > 0) {
            val padding = ssb.length
            ssb.append("\n" + tags)
            ssb.setSpan(ForegroundColorSpan(-6710887), padding, padding + 1 + tags.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return ssb
    }

    companion object {

        private val PREFERENCES_FONTSIZE = "fontsizesp"
        val TYPE_THREAD = 1
        var urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE)
        var msgPattern = Pattern.compile("#[0-9]+")
    }
}
