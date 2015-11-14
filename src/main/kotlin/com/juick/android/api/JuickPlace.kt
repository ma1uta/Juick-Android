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

import java.util.ArrayList
// import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**

 * @author Ugnich Anton
 */
class JuickPlace {

    var pid = 0
    var lat = 0.0
    var lon = 0.0
    var name: String? = null
    var description: String? = null
    var users = 0
    var messages = 0
    var distance = 0
    var tags = ArrayList<String>()

    companion object {

        @Throws(JSONException::class)
        fun parseJSON(json: JSONObject): JuickPlace {
            val jplace = JuickPlace()

            if (json.has("pid")) {
                jplace.pid = json.getInt("pid")
            }
            if (json.has("lat")) {
                jplace.lat = json.getDouble("lat")
            }
            if (json.has("lon")) {
                jplace.lon = json.getDouble("lon")
            }
            if (json.has("name")) {
                jplace.name = json.getString("name").replace("&quot;", "\"")
            }
            if (json.has("description")) {
                jplace.description = json.getString("description").replace("&quot;", "\"")
            }
            if (json.has("users")) {
                jplace.users = json.getInt("users")
            }
            if (json.has("messages")) {
                jplace.messages = json.getInt("messages")
            }
            if (json.has("distance")) {
                jplace.distance = json.getInt("distance")
            }
            if (json.has("tags")) {
                val tags = json.getJSONArray("tags")
                for (n in 0..tags.length() - 1) {
                    jplace.tags.add(tags.getString(n).replace("&quot;", "\""))
                }
            }

            return jplace
        }
    }
}
