package com.example.wsins.wifiviewer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.example.wsins.wifiviewer.R
import com.example.wsins.wifiviewer.adapter.WifiAdapter
import com.example.wsins.wifiviewer.info.WifiInfo
import com.example.wsins.wifiviewer.utils.ClipBoardUtils
import com.example.wsins.wifiviewer.utils.DensityUtils
import com.example.wsins.wifiviewer.utils.RootUtils
import com.example.wsins.wifiviewer.utils.WifiManage
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var mWifiInfos: MutableList<WifiInfo> = mutableListOf()
    private var mHandler = MyHandler(this)
    private lateinit var mWifiAdapter: WifiAdapter

    companion object {
        const val INIT_DATA = 0
        const val REFRESH_DATA = 1

        private class MyHandler(activity: MainActivity) : Handler() {
            private val mActivity: WeakReference<MainActivity> = WeakReference(activity)
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                val activity = mActivity.get()
                when (msg?.what) {
                    INIT_DATA -> activity?.setData()
                    REFRESH_DATA -> {
                        activity?.run {
                            setData()
                            srl_wifi_list.also {
                                it.isRefreshing = false
                                Snackbar.make(it, "刷新完成。", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRoot()
    }

    private fun initRoot() {
        if (RootUtils().isRoot) {
            if (!RootUtils().checkRoot()) {
                AlertDialog.Builder(this).run {
                    setTitle("Root权限检测")
                    setMessage("无法获取Root权限。")
                    setCancelable(false)
                    setPositiveButton("退出") { _, _ ->
                        this@MainActivity.finish()
                    }
                    show()
                }
            } else {
                initActionBar()
                initView()
                getData()
                initListener()
            }
        } else {
            AlertDialog.Builder(this).run {
                setTitle("Root权限检测")
                setMessage("本设备未Root。")
                setCancelable(false)
                setPositiveButton("退出") { _, _ ->
                    this@MainActivity.finish()
                }
                show()
            }
        }
    }

    private fun getData(what: Int = INIT_DATA) {
        thread(true) {
            val msg = Message.obtain().also {
                it.what = what
            }
            mWifiInfos = WifiManage().readData()!!
            mHandler.sendMessage(msg)
        }
    }

    private fun setData() {
        nav_view.apply {
            setCheckedItem(R.id.nav_wifi_list)
            getHeaderView(0).findViewById<TextView>(R.id.app_name).text = getString(R.string.app_name)
            getHeaderView(0).findViewById<TextView>(R.id.wifi_count).text = "共${mWifiInfos.size}条Wifi信息"
        }
        mWifiAdapter.setData(mWifiInfos)
    }

    private fun initActionBar() {
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }
    }

    private fun initView() {
        val layoutManager = LinearLayoutManager(this).apply {
            orientation = OrientationHelper.VERTICAL
        }
        rv_wifi_list.layoutManager = layoutManager
        mWifiAdapter = WifiAdapter(this)
        rv_wifi_list.addItemDecoration(object : RecyclerView.ItemDecoration() {

            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.run {
                    left = DensityUtils.dip2px(this@MainActivity, 8f)
                    right = DensityUtils.dip2px(this@MainActivity, 8f)
                    top = DensityUtils.dip2px(this@MainActivity, 8f)
                }
            }
        })
        rv_wifi_list.adapter = mWifiAdapter
    }

    private fun initListener() {
        mWifiAdapter.setOnRVItemClickListener(object : WifiAdapter.OnRVItemClickListener {
            override fun onRVItemClick(position: Int) {
                ClipBoardUtils().copyClipBoard(this@MainActivity, "wifipwd", mWifiInfos[position].password)
                Snackbar.make(rv_wifi_list, "已复制密码 ${mWifiInfos[position].password} 到剪贴板。", Snackbar.LENGTH_SHORT).show()
            }

            override fun onRVItemLongClick(position: Int) {
                ClipBoardUtils().copyClipBoard(this@MainActivity, "wifissidpwd", "SSID：" + mWifiInfos[position].ssid + "\n密码：" + mWifiInfos[position].password)
                Snackbar.make(rv_wifi_list, "已复制 ${mWifiInfos[position].ssid} 的SSID和密码到剪贴板。", Snackbar.LENGTH_SHORT).show()
            }

        })
        srl_wifi_list.setOnRefreshListener {
            Handler().postDelayed({
                getData(REFRESH_DATA)
            }, 500)
        }
        nav_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_about -> {
                    it.isCheckable = false
                    it.isChecked = false
                    AboutActivity.move(this)
                }
            }
            drawer_layout.closeDrawers()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> drawer_layout.openDrawer(GravityCompat.START)
            R.id.item_setting -> {
                val intent = Intent().apply {
                    action = "android.net.wifi.PICK_WIFI_NETWORK"
                }
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var mExitTime: Long = 0
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Snackbar.make(rv_wifi_list, "再按一次退出程序。", Snackbar.LENGTH_SHORT).show()
                mExitTime = System.currentTimeMillis()
            } else {
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

}
