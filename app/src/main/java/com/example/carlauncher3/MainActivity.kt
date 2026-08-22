package com.example.carlauncher3

import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

data class Slot(var packageName: String? = null, var weight: Float = 1f)

class MainActivity : Activity() {
    private lateinit var container: LinearLayout
    private val slots = mutableListOf(Slot(), Slot(), Slot())
    private val adbHost = "127.0.0.1"
    private val adbPort = 5555

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        buildUi()
        tryAdb()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        val bar = TextView(this).apply {
            text = "＋ 添加分屏     ADB: $adbHost:$adbPort"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            setOnClickListener { addSlot() }
        }
        root.addView(bar, LinearLayout.LayoutParams(-1, 48))
        setContentView(root)
        render()
    }

    private fun render() {
        container.removeAllViews()
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val divider = SeekBar(this).apply {
                    max = 100
                    progress = 50
                    rotation = 90f
                    setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val left = slots[index-1].weight
                                val right = slots[index].weight
                                val total = left + right
                                val ratio = (p.coerceIn(10,90) / 100f)
                                slots[index-1].weight = total * ratio
                                slots[index].weight = total * (1-ratio)
                                render()
                            }
                        }
                        override fun onStartTrackingTouch(s: SeekBar?) {}
                        override fun onStopTrackingTouch(s: SeekBar?) {}
                    })
                }
                val lp = LinearLayout.LayoutParams(34, -1)
                lp.gravity = Gravity.CENTER_VERTICAL
                container.addView(divider, lp)
            }
            val frame = FrameLayout(this).apply {
                setBackgroundColor(Color.rgb(18,18,18))
                setOnClickListener {
                    if (slot.packageName == null) chooseApp(index)
                    else launch(slot.packageName!!)
                }
            }
            val label = TextView(this).apply {
                text = if (slot.packageName == null) "＋" else appName(slot.packageName!!)
                textSize = if (slot.packageName == null) 48f else 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(8,8,8,8)
            }
            frame.addView(label, FrameLayout.LayoutParams(-1,-1))
            frame.setOnLongClickListener {
                if (slot.packageName != null) {
                    slot.packageName = null
                    render()
                    true
                } else false
            }
            val lp = LinearLayout.LayoutParams(0,-1,slot.weight)
            container.addView(frame, lp)
        }
    }

    private fun addSlot() {
        if (slots.size >= 8) {
            Toast.makeText(this, "最多支持 8 个框", Toast.LENGTH_SHORT).show()
            return
        }
        slots.add(Slot())
        render()
    }

    private fun chooseApp(index: Int) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString() }
        val names = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择在第 ${index+1} 个框启动的 APP")
            .setItems(names) { _, which ->
                slots[index].packageName = apps[which].packageName
                render()
                launch(apps[which].packageName)
            }.setNegativeButton("取消", null).show()
    }

    private fun launch(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else Toast.makeText(this,"无法启动：$pkg",Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this,"启动失败：${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun appName(pkg: String): String = try {
        val ai = packageManager.getApplicationInfo(pkg,0)
        packageManager.getApplicationLabel(ai).toString()
    } catch (_: Exception) { pkg }

    private fun tryAdb() {
        thread {
            var ok=false
            try {
                val p = Runtime.getRuntime().exec(arrayOf("sh","-c","echo ping | nc -w 1 $adbHost $adbPort"))
                p.waitFor()
                ok = p.exitValue()==0
            } catch (_: Exception) {}
            runOnUiThread {
                if (ok) Toast.makeText(this,"已检测到 ADB：$adbHost:$adbPort",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
