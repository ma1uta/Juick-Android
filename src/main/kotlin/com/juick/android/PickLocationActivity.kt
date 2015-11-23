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
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
// import com.google.android.maps.GeoPoint
import com.google.android.maps.MapActivity
import com.google.android.maps.MapView
import com.google.android.maps.MyLocationOverlay
import com.juick.R

/**

 * @author Ugnich Anton
 */
class PickLocationActivity : MapActivity(), OnClickListener {

    private var mapView: MapView? = null
    private var myLocation: MyLocationOverlay? = null
    private var bOK: Button? = null

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.map)

        bOK = findViewById(R.id.buttonOK) as Button
        bOK!!.setOnClickListener(this)

        mapView = findViewById(R.id.mapView) as MapView
        mapView!!.setBuiltInZoomControls(true)
        myLocation = MyLocationOverlay(this, mapView)
        myLocation!!.runOnFirstFix(object : Runnable {

            override fun run() {
                mapView!!.controller.setCenter(myLocation!!.myLocation)
                mapView!!.controller.setZoom(17)
            }
        })
        myLocation!!.enableMyLocation()
        mapView!!.overlays.add(myLocation)
    }

    override protected fun onPause() {
        super.onPause()
        myLocation!!.disableMyLocation()
    }

    override fun onClick(v: View?) {
        val i = Intent()
        val center = mapView!!.mapCenter
        i.putExtra("lat", center.latitudeE6 / 1000000)
        i.putExtra("lon", center.longitudeE6 / 1000000)
        setResult(RESULT_OK, i)
        finish()
    }

    override protected fun isRouteDisplayed(): Boolean {
        return false
    }
}
