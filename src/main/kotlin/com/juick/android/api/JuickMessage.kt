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
package com.juick.android.api

// import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.TimeZone
// import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JuickMessage() {

    var MID = 0
    var RID = 0
    var Text: String? = null
    var User: JuickUser? = null
    var tags = ArrayList<String>()
    var Timestamp: Date? = null
    var replies = 0
    var Photo: String? = null
    var Video: String? = null

    fun getTags(): String {
        var t = String()
        val i = tags.iterator()
        while (i.hasNext()) {
            if (t.length > 0) {
                t += ' '
            }
            t += '*' + i.next()
        }
        return t
    }


    override fun equals(other: Any?): Boolean {
        if (other !is JuickMessage)
            return false
        return (this.MID == other.MID && this.RID == other.RID)
    }

    @Throws(ClassCastException::class)
    operator fun compareTo(obj: Any): Int {
        if (obj !is JuickMessage) {
            throw ClassCastException()
        }

        if (this.MID != obj.MID) {
            if (this.MID > obj.MID) {
                return -1
            } else {
                return 1
            }
        }

        if (this.RID != obj.RID) {
            if (this.RID < obj.RID) {
                return -1
            } else {
                return 1
            }
        }

        return 0
    }

    override fun toString(): String {
        var msg = ""
        if (User != null) {
            msg += "@" + User!!.UName + ": "
        }
        msg += getTags()
        if (msg.length > 0) {
            msg += "\n"
        }
        if (Photo != null) {
            msg += Photo!! + "\n"
        } else if (Video != null) {
            msg += Video!! + "\n"
        }
        if (Text != null) {
            msg += Text!! + "\n"
        }
        msg += "#" + MID
        if (RID > 0) {
            msg += "/" + RID
        }
        msg += " http://juick.com/" + MID
        if (RID > 0) {
            msg += "#" + RID
        }
        return msg
    }

    companion object {

        @Throws(JSONException::class)
        fun parseJSON(json: JSONObject): JuickMessage {
            val jmsg = JuickMessage()
            if (json.has("mid")) {
                jmsg.MID = json.getInt("mid")
            }
            if (json.has("rid")) {
                jmsg.RID = json.getInt("rid")
            }
            if (json.has("body")) {
                jmsg.Text = json.getString("body").replace("&quot;", "\"")
            }
            if (json.has("user")) {
                jmsg.User = JuickUser.parseJSON(json.getJSONObject("user"))
            }

            if (json.has("timestamp")) {
                try {
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    df.timeZone = TimeZone.getTimeZone("UTC")
                    jmsg.Timestamp = df.parse(json.getString("timestamp"))
                } catch (e: ParseException) {
                }

            }

            if (json.has("tags")) {
                val tags = json.getJSONArray("tags")
                for (n in 0..tags.length() - 1) {
                    jmsg.tags.add(tags.getString(n).replace("&quot;", "\""))
                }
            }

            if (json.has("replies")) {
                jmsg.replies = json.getInt("replies")
            }

            if (json.has("photo")) {
                jmsg.Photo = json.getJSONObject("photo").getString("small")
            }
            if (json.has("video")) {
                jmsg.Video = json.getJSONObject("video").getString("mp4")
            }

            return jmsg
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
