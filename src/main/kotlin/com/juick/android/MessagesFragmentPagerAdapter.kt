package com.juick.android

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

/**
 * Created by vt on 21/11/15.
 */
class MessagesFragmentPagerAdapter(fm: FragmentManager, private val context: Context) : FragmentPagerAdapter(fm) {

    private val tabTitles = arrayOf("Home", "Discover", "Photos")
    private val tabTags = arrayOf("home", "all", "media")

    override fun getItem(position: Int): Fragment {
        val b = Bundle()
        b.putBoolean(tabTags[position], true)
        b.putBoolean("usecache", true)
        return Fragment.instantiate(context, MessagesFragment::class.java.name, b)
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }
}
