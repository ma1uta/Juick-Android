/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 * Copyright (C) 2011 Johan Nilsson <https://github.com/johannilsson/android-pulltorefresh>
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

import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_FLING
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import com.juick.android.api.JuickMessage
import android.content.Context
import android.content.Intent
// import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.juick.R
import java.net.URLEncoder

/**

 * @author Ugnich Anton
 */
class MessagesFragment : ListFragment(), AdapterView.OnItemClickListener, AbsListView.OnScrollListener, View.OnTouchListener, View.OnClickListener {

    private var listAdapter: JuickMessagesAdapter? = null
    private var longClickListener: JuickMessageMenu? = null
    private var viewLoading: View? = null
    private var apiurl: String? = null
    private var loading: Boolean = false
    private var usecache: Boolean = false
    private var mRefreshView: RelativeLayout? = null
    private var mRefreshViewText: TextView? = null
    private var mRefreshViewImage: ImageView? = null
    private var mRefreshViewProgress: ProgressBar? = null
    private var mCurrentScrollState: Int = 0
    private var mRefreshState: Int = 0
    private var mFlipAnimation: RotateAnimation? = null
    private var mReverseFlipAnimation: RotateAnimation? = null
    private var mRefreshViewHeight: Int = 0
    private var mRefreshOriginalTopPadding: Int = 0
    private var mLastMotionY: Int = 0
    private var mBounceHack: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var home = false
        var uid = 0
        var uname: String? = null
        var search: String? = null
        var tag: String? = null
        var place_id = 0
        var popular = false
        var media = false

        val args = arguments
        if (args != null) {
            home = args.getBoolean("home", false)
            uid = args.getInt("uid", 0)
            uname = args.getString("uname")
            search = args.getString("search")
            tag = args.getString("tag")
            place_id = args.getInt("place_id", 0)
            popular = args.getBoolean("popular", false)
            media = args.getBoolean("media", false)
            usecache = args.getBoolean("usecache", false)
        }

        if (home) {
            apiurl = "https://api.juick.com/home?1=1"
        } else {
            apiurl = "https://api.juick.com/messages?1=1"
            if (uid > 0 && uname != null) {
                apiurl += "&user_id=" + uid
            } else if (search != null) {
                try {
                    apiurl += "&search=" + URLEncoder.encode(search, "utf-8")
                } catch (e: Exception) {
                    Log.e("ApiURL", e.toString())
                }

            } else if (tag != null) {
                try {
                    apiurl += "&tag=" + URLEncoder.encode(tag, "utf-8")
                } catch (e: Exception) {
                    Log.e("ApiURL", e.toString())
                }

                if (uid == -1) {
                    apiurl += "&user_id=-1"
                }
            } else if (place_id > 0) {
                apiurl += "&place_id=" + place_id
            } else if (popular) {
                apiurl += "&popular=1"
            } else if (media) {
                apiurl += "&media=all"
            }
        }

        mFlipAnimation = RotateAnimation(0.0f, -180.0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mFlipAnimation!!.interpolator = LinearInterpolator()
        mFlipAnimation!!.duration = 250
        mFlipAnimation!!.fillAfter = true
        mReverseFlipAnimation = RotateAnimation(-180.0f, 0.0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mReverseFlipAnimation!!.interpolator = LinearInterpolator()
        mReverseFlipAnimation!!.duration = 250
        mReverseFlipAnimation!!.fillAfter = true

        val li = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        viewLoading = li.inflate(R.layout.listitem_loading, null)

        mRefreshView = li.inflate(R.layout.pull_to_refresh_header, null) as RelativeLayout
        mRefreshViewText = mRefreshView!!.findViewById(R.id.pull_to_refresh_text) as TextView
        mRefreshViewImage = mRefreshView!!.findViewById(R.id.pull_to_refresh_image) as ImageView
        mRefreshViewProgress = mRefreshView!!.findViewById(R.id.pull_to_refresh_progress) as ProgressBar
        mRefreshViewImage!!.minimumHeight = 50
        mRefreshView!!.setOnClickListener(this)
        mRefreshOriginalTopPadding = mRefreshView!!.paddingTop
        mRefreshState = TAP_TO_REFRESH

        listAdapter = JuickMessagesAdapter(activity, 0)
        longClickListener = JuickMessageMenu(activity)

        loadData()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.setOnTouchListener(this)
        listView.setOnScrollListener(this)
        listView.onItemClickListener = this
        listView.onItemLongClickListener = longClickListener

        if (usecache) {
            val sp = PreferenceManager.getDefaultSharedPreferences(activity)
            val jcacheStr = sp.getString("jcache_feed", null)
            if (jcacheStr != null) {
                processData(jcacheStr)
            }
        }
    }

    private fun loadData() {
        loading = true
        val thr = Thread(object : Runnable {

            override fun run() {
                val jsonStr = Utils.getJSON(activity, apiurl!!)
                if (isAdded) {
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            processData(jsonStr)
                        }
                    })
                }
            }
        })
        thr.start()
    }

    private fun processData(jsonStr: String?) {
        var newadapter = mRefreshState != TAP_TO_REFRESH
        if (jsonStr != null) {
            listAdapter!!.clear()
            val cnt = listAdapter!!.parseJSON(jsonStr)

            if (getListAdapter() == null) {
                listView.addHeaderView(mRefreshView, null, false)
                mRefreshViewHeight = mRefreshView!!.measuredHeight

                if (cnt == 20) {
                    listView.addFooterView(viewLoading, null, false)
                }

                setListAdapter(listAdapter)
                newadapter = true
            } else {
                if (cnt < 20 && this@MessagesFragment.listView.footerViewsCount > 0) {
                    this@MessagesFragment.listView.removeFooterView(viewLoading)
                }
            }

            if (usecache) {
                val spe = PreferenceManager.getDefaultSharedPreferences(activity).edit()
                spe.putString("jcache_feed", jsonStr)
                spe.commit()
            }
        }
        loading = false

        resetHeader()
        listView.invalidateViews()
        if (newadapter) {
            setSelection(1)
        }
    }

    private fun loadMore(before_mid: Int) {
        loading = true
        val thr = Thread(object : Runnable {

            override fun run() {
                val jsonStr = Utils.getJSON(activity, apiurl + "&before_mid=" + before_mid)
                if (isAdded) {
                    activity.runOnUiThread(object : Runnable {

                        override fun run() {
                            if (jsonStr == null || listAdapter!!.parseJSON(jsonStr) != 20) {
                                if (this@MessagesFragment.listView.footerViewsCount > 0) {
                                    this@MessagesFragment.listView.removeFooterView(viewLoading)
                                }
                            }
                            loading = false
                        }
                    })
                }
            }
        })
        thr.start()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val i = Intent(activity, ThreadActivity::class.java)
        i.putExtra("mid", (parent?.getItemAtPosition(position) as JuickMessage).MID)
        startActivity(i)
    }

    // Refresh
    override fun onClick(view: View?) {
        mRefreshState = REFRESHING
        prepareForRefresh()
        loadData()
    }

    override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        if (visibleItemCount < totalItemCount && (firstVisibleItem + visibleItemCount == totalItemCount) && loading == false) {
            val before_mid = (listAdapter!!.getItem(listAdapter!!.count - 1) as JuickMessage).MID
            loadMore(before_mid)
        }

        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mRefreshState != REFRESHING) {
            if (firstVisibleItem == 0) {
                mRefreshViewImage!!.visibility = View.VISIBLE
                if ((mRefreshView!!.bottom >= mRefreshViewHeight + 20 || mRefreshView!!.top >= 0) && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshViewText!!.setText(R.string.pull_to_refresh_release_label)
                    mRefreshViewImage!!.clearAnimation()
                    mRefreshViewImage!!.startAnimation(mFlipAnimation)
                    mRefreshState = RELEASE_TO_REFRESH
                } else if (mRefreshView!!.bottom < mRefreshViewHeight + 20 && mRefreshState != PULL_TO_REFRESH) {
                    mRefreshViewText!!.setText(R.string.pull_to_refresh_pull_label)
                    if (mRefreshState != TAP_TO_REFRESH) {
                        mRefreshViewImage!!.clearAnimation()
                        mRefreshViewImage!!.startAnimation(mReverseFlipAnimation)
                    }
                    mRefreshState = PULL_TO_REFRESH
                }
            } else {
                mRefreshViewImage!!.visibility = View.GONE
                resetHeader()
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING && firstVisibleItem == 0 && mRefreshState != REFRESHING) {
            setSelection(1)
            mBounceHack = true
        } else if (mBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
            setSelection(1)
        }
    }

    override fun onTouch(view: View, event: MotionEvent?): Boolean {
        mBounceHack = false

        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                if (!listView.isVerticalScrollBarEnabled) {
                    listView.setVerticalScrollBarEnabled(true)
                }
                if (listView.firstVisiblePosition === 0 && mRefreshState != REFRESHING) {
                    if ((mRefreshView!!.bottom >= mRefreshViewHeight || mRefreshView!!.top >= 0) && mRefreshState == RELEASE_TO_REFRESH) {
                        // Initiate the refresh
                        onClick(listView)
                    } else if (mRefreshView!!.bottom < mRefreshViewHeight || mRefreshView!!.top <= 0) {
                        // Abort refresh and scroll down below the refresh view
                        resetHeader()
                        setSelection(1)
                    }
                }
            }
            MotionEvent.ACTION_DOWN -> mLastMotionY = event!!.y.toInt()
            MotionEvent.ACTION_MOVE -> applyHeaderPadding(event!!)
        }
        return false
    }

    private fun applyHeaderPadding(ev: MotionEvent) {
        // getHistorySize has been available since API 1
        val pointerCount = ev.historySize

        for (p in 0..pointerCount - 1) {
            if (mRefreshState == RELEASE_TO_REFRESH) {
                if (listView.isVerticalFadingEdgeEnabled) {
                    listView.setVerticalScrollBarEnabled(false)
                }

                // Calculate the padding to apply, we divide by 1.7 to
                // simulate a more resistant effect during pull.
                val topPadding = (((ev.getHistoricalY(p) - mLastMotionY) - mRefreshViewHeight) / 1.7).toInt()

                mRefreshView!!.setPadding(
                        mRefreshView!!.paddingLeft,
                        topPadding,
                        mRefreshView!!.paddingRight,
                        mRefreshView!!.paddingBottom)
            }
        }
    }

    /**
     * Sets the header padding back to original size.
     */
    private fun resetHeaderPadding() {
        mRefreshView!!.setPadding(
                mRefreshView!!.paddingLeft,
                mRefreshOriginalTopPadding,
                mRefreshView!!.paddingRight,
                mRefreshView!!.paddingBottom)
    }

    /**
     * Resets the header to the original state.
     */
    private fun resetHeader() {
        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH

            resetHeaderPadding()

            // Set refresh view text to the pull label
            mRefreshViewText!!.setText(R.string.pull_to_refresh_tap_label)
            // Replace refresh drawable with arrow drawable
            mRefreshViewImage!!.setImageResource(R.drawable.ic_pulltorefresh_arrow)
            // Clear the full rotation animation
            mRefreshViewImage!!.clearAnimation()
            // Hide progress bar and arrow.
            mRefreshViewImage!!.visibility = View.GONE
            mRefreshViewProgress!!.visibility = View.GONE
        }
    }

    override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
        mCurrentScrollState = scrollState

        if (mCurrentScrollState == SCROLL_STATE_IDLE) {
            mBounceHack = false
        }
    }

    fun prepareForRefresh() {
        resetHeaderPadding()

        mRefreshViewImage!!.visibility = View.GONE
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage!!.setImageDrawable(null)
        mRefreshViewProgress!!.visibility = View.VISIBLE

        // Set refresh view text to the refreshing label
        mRefreshViewText!!.setText(R.string.pull_to_refresh_refreshing_label)

        mRefreshState = REFRESHING
    }

    companion object {
        // Pull to refresh
        private val TAP_TO_REFRESH = 1
        private val PULL_TO_REFRESH = 2
        private val RELEASE_TO_REFRESH = 3
        private val REFRESHING = 4
    }
}
