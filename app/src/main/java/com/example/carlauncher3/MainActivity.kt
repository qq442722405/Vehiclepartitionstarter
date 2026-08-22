package com.example.carlauncher3

import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.max
import kotlin.math.min

data class Slot(var packageName: String? = null, var widthPx: Int = 0)

class MainActivity : Activity() {
    private lateinit var container: LinearLayout
    private lateinit var root: LinearLayout
    private val slots = mutableListOf(Slot(), Slot(), Slot())
    private val dividers = mutableListOf<View>()
    private var totalWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        buildUi()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(12,12,12))
        }
        val add = TextView(this).apply {
            text = "＋ 添加分区"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(28,0,28,0)
            setOnClickListener { addSlot() }
        }
        val access = TextView(this).apply {
            text = "  窗口控制权限  "
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
            }
        }
        bottom.addView(add, LinearLayout.LayoutParams(-2, 56))
        bottom.addView(access, LinearLayout.LayoutParams(-2,56))
        root.addView(bottom, LinearLayout.LayoutParams(-1,56))
        setContentView(root)

        container.post { totalWidth = container.width; initWidths() }
        render()
    }

    private fun initWidths() {
        if (totalWidth <= 0) totalWidth = resources.displayMetrics.widthPixels
        if (slots.all { it.widthPx == 0 }) {
            val w = max(200, totalWidth / slots.size)
            slots.forEach { it.widthPx = w }
        }
    }

    private fun render() {
        if (!::container.isInitialized) return
        container.removeAllViews()
        dividers.clear()
        container.post { totalWidth = container.width }
        initWidths()

        slots.forEachIndexed { i, slot ->
            val frame = FrameLayout(this).apply {
                setBackgroundColor(Color.rgb(22,22,22))
                val border = GradientDrawable()
                border.setColor(Color.TRANSPARENT)
                border.setStroke(2, Color.rgb(60,60,60))
                background = border
                setOnClickListener {
                    if (slot.packageName == null) chooseApp(i)
                    else launch(slot.packageName!!)
                }
                setOnLongClickListener {
                    if (slot.packageName != null) {
                        slot.packageName = null
                        render()
                        true
                    } else false
                }
            }

            val label = TextView(this).apply {
                text = if (slot.packageName == null) "+" else appName(slot.packageName!!)
                textSize = if (slot.packageName == null) 48f else 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            frame.addView(label, FrameLayout.LayoutParams(-1,-1))
            container.addView(frame, LinearLayout.LayoutParams(max(120,slot.widthPx),-1))

            if (i < slots.lastIndex) {
                val divider = View(this).apply {
                    setBackgroundColor(Color.DKGRAY)
                    setOnTouchListener(DragDivider(i))
                }
                dividers.add(divider)
                val dlp = LinearLayout.LayoutParams(32,-1)
                container.addView(divider, dlp)
            }
        }
    }

    private inner class DragDivider(private val index: Int) : View.OnTouchListener {
        private var downX = 0f
        private var leftStart = 0
        private var rightStart = 0

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    leftStart = slots[index].widthPx
                    rightStart = slots[index+1].widthPx
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = (event.rawX-downX).toInt()
                    val minW = 180
                    val newLeft = max(minW, leftStart+delta)
                    val newRight = max(minW, rightStart-delta)
                    val total = leftStart+rightStart
                    if (newLeft+newRight <= total) {
                        slots[index].widthPx = newLeft
                        slots[index+1].widthPx = newRight
                        applyWidths()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
            }
            return false
        }
    }

    private fun applyWidths() {
        var k=0
        for (i in slots.indices) {
            val child = container.getChildAt(k++)
            child.layoutParams = (child.layoutParams as LinearLayout.LayoutParams).apply {
                width = max(120, slots[i].widthPx)
                weight = 0f
            }
            child.requestLayout()
            if (i < slots.lastIndex) k++
        }
    }

    private fun addSlot() {
        if (slots.size >= 8) {
            Toast.makeText(this,"最多 8 个分区",Toast.LENGTH_SHORT).show()
            return
        }
        val each = max(180, totalWidth / (slots.size+1))
        slots.forEach { it.widthPx = each }
        slots.add(Slot(widthPx=each))
        render()
    }

    private fun chooseApp(index: Int) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString() }
        val names = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择第 ${index+1} 个分区 APP")
            .setItems(names) { _, which ->
                slots[index].packageName = apps[which].packageName
                render()
                launch(apps[which].packageName)
            }.setNegativeButton("取消",null).show()
    }

    private fun launch(pkg:String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            try { startActivity(intent) } catch (e:Exception) {
                Toast.makeText(this,"启动失败：${e.message}",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun appName(pkg:String):String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg,0)).toString()
    } catch (_:Exception) { pkg }
}
