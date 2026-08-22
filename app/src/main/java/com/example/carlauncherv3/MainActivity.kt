package com.example.carlauncherv3

import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.*
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {
    private lateinit var log: TextView
    private lateinit var selectedText: TextView
    private var selectedPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        build()
    }

    private fun build() {
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(18,12,18,10)
        }
        val title=TextView(this).apply {
            text="APP 窗口嵌入测试 V3"
            textSize=22f; setTextColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL
        }
        root.addView(title,LinearLayout.LayoutParams(-1,52))

        selectedText=TextView(this).apply {
            text="当前 APP：未选择"
            textSize=16f; setTextColor(Color.LTGRAY); gravity=Gravity.CENTER_VERTICAL
            setOnClickListener { chooseApp() }
        }
        root.addView(selectedText,LinearLayout.LayoutParams(-1,48))

        val grid=GridLayout(this).apply { columnCount=3; rowCount=3 }
        val buttons=listOf(
            "选择 APP" to { chooseApp() },
            "A 普通 Activity" to { launch(0) },
            "B 多窗口" to { launch(1) },
            "C ADB 启动" to { launch(2) },
            "D ADB wm/am" to { runShellTest() },
            "E 无障碍" to { openAccessibility() },
            "读取窗口信息" to { readWindowInfo() },
            "悬浮窗权限" to { openOverlay() },
            "停止/返回" to { finish() }
        )
        buttons.forEach { (text,action) ->
            val b=Button(this).apply {
                this.text=text
                textSize=14f
                setOnClickListener { action() }
            }
            val lp=GridLayout.LayoutParams().apply {
                width=0; height=58
                columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                setMargins(5,5,5,5)
            }
            grid.addView(b,lp)
        }
        root.addView(grid,LinearLayout.LayoutParams(-1,185))

        val hint=TextView(this).apply {
            text="先选择一个 APP，再依次测试 A/B/C/D/E。每次测试后观察 APP 是否进入本程序的测试区域。"
            textSize=13f; setTextColor(Color.LTGRAY); setPadding(6,4,6,4)
        }
        root.addView(hint,LinearLayout.LayoutParams(-1,55))

        log=TextView(this).apply {
            textSize=12f; setTextColor(Color.WHITE); setPadding(10,8,10,8)
            background=GradientDrawable().apply {
                setColor(Color.rgb(18,18,18)); setStroke(1,Color.DKGRAY)
            }
        }
        val scroll=ScrollView(this).apply { addView(log) }
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        append("启动成功。测试目标：${"127.0.0.1"}:5555")
    }

    private fun chooseApp() {
        val pm=packageManager
        val apps=pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter{it.packageName!=packageName}
            .sortedBy{pm.getApplicationLabel(it).toString()}
        val names=apps.map{pm.getApplicationLabel(it).toString()}.toTypedArray()
        AlertDialog.Builder(this).setTitle("选择测试 APP")
            .setItems(names){_,i->
                selectedPkg=apps[i].packageName
                selectedText.text="当前 APP：${names[i]}  ($selectedPkg)"
                append("已选择：$selectedPkg")
            }.setNegativeButton("取消",null).show()
    }

    private fun launch(mode:Int) {
        val pkg=selectedPkg ?: run { chooseApp(); return }
        val intent=packageManager.getLaunchIntentForPackage(pkg) ?: return append("没有启动入口：$pkg")
        try {
            when(mode) {
                0 -> { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); append("A：普通 Activity 已启动") }
                1 -> {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                    try { intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK) } catch(_:Exception){}
                    startActivity(intent); append("B：已尝试多窗口/相邻 Activity")
                }
                2 -> {
                    val cmd="am start -n $pkg/${intent.component?.className ?: ""}"
                    shell(cmd)
                    append("C：已尝试 shell：$cmd")
                }
            }
        } catch(e:Exception) { append("启动失败：${e.message}") }
    }

    private fun runShellTest() {
        val pkg=selectedPkg ?: run { chooseApp(); return }
        append("D：开始检查 shell / ADB 环境")
        shell("echo ADB_TEST && id && wm size && wm density && dumpsys window windows | head -80")
        shell("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $pkg")
    }

    private fun readWindowInfo() {
        shell("wm size")
        shell("wm density")
        shell("dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp|Window #|mFrame=' | head -120")
        append("注意：如果系统拒绝 shell，会在日志显示 Permission denied / not found。")
    }

    private fun shell(cmd:String) {
        Thread {
            try {
                val p=Runtime.getRuntime().exec(arrayOf("sh","-c",cmd))
                val out=BufferedReader(InputStreamReader(p.inputStream)).readText()
                val err=BufferedReader(InputStreamReader(p.errorStream)).readText()
                p.waitFor()
                runOnUiThread {
                    append("$ $cmd\n${out.trim()}\n${err.trim()}")
                }
            } catch(e:Exception) {
                runOnUiThread { append("shell 异常：${e.message}") }
            }
        }.start()
    }

    private fun openAccessibility() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            append("已打开无障碍设置，请测试是否可以启用本测试器服务。")
        } catch(e:Exception) { append("无法打开无障碍设置：${e.message}") }
    }

    private fun openOverlay() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")))
            append("已打开悬浮窗权限页面。")
        } catch(e:Exception) { append("无法打开悬浮窗权限：${e.message}") }
    }

    private fun append(s:String) {
        if(!::log.isInitialized) return
        log.append("\n$s\n")
    }
}
