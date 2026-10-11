package com.mcm.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextClock
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

// Mid-century modern palette
private val CREAM = Color.parseColor("#F4EBD9")
private val ORANGE = Color.parseColor("#E8702A")
private val MUSTARD = Color.parseColor("#E9B44C")
private val TEAL = Color.parseColor("#2F6F6B")
private val WALNUT = Color.parseColor("#3B2A20")

/** Geometric backdrop: starburst, big orange sun, mustard circle, teal half-moon. */
class Backdrop(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        cv.drawColor(CREAM)

        // starburst, top right
        val cx = w * 0.82f; val cy = h * 0.12f
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.008f
        p.strokeCap = Paint.Cap.ROUND
        p.color = MUSTARD
        for (i in 0 until 16) {
            val a = Math.toRadians(i * 22.5)
            val r1 = w * 0.07f
            val r2 = if (i % 2 == 0) w * 0.17f else w * 0.12f
            cv.drawLine(
                cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat(),
                cx + (r2 * cos(a)).toFloat(), cy + (r2 * sin(a)).toFloat(), p
            )
        }
        p.style = Paint.Style.FILL
        p.color = ORANGE
        cv.drawCircle(cx, cy, w * 0.045f, p)

        // big orange sun rising from the bottom
        cv.drawCircle(w * 0.85f, h, w * 0.7f, p)
        // mustard circle, bottom left
        p.color = MUSTARD
        cv.drawCircle(w * 0.1f, h * 0.98f, w * 0.22f, p)
        // teal half-moon
        p.color = TEAL
        val s = w * 0.3f
        cv.drawArc(RectF(w * 0.3f, h * 0.8f, w * 0.3f + s, h * 0.8f + s), 180f, 180f, true, p)
    }
}

class MainActivity : Activity() {
    private lateinit var drawer: View
    private lateinit var gd: GestureDetector
    private var apps: List<ResolveInfo> = emptyList()
    private val adapter = AppAdapter()

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = FrameLayout(this)
        root.addView(Backdrop(this))
        val content = FrameLayout(this)
        root.addView(content)
        content.setOnApplyWindowInsetsListener { v, i ->
            @Suppress("DEPRECATION")
            v.setPadding(0, i.systemWindowInsetTop, 0, i.systemWindowInsetBottom)
            i
        }

        // Home: clock, date, rule
        val home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36.dp(), 110.dp(), 36.dp(), 0)
        }
        val clock = TextClock(this).apply {
            format12Hour = "h:mm"; format24Hour = "H:mm"
            textSize = 92f
            setTextColor(WALNUT)
            typeface = Typeface.create("serif", Typeface.BOLD)
            includeFontPadding = false
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE  ·  MMM d"; format24Hour = "EEEE  ·  MMM d"
            textSize = 14f
            letterSpacing = 0.2f
            setAllCaps(true)
            setTextColor(TEAL)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val rule = View(this).apply { setBackgroundColor(ORANGE) }
        home.addView(clock)
        home.addView(date)
        home.addView(rule, LinearLayout.LayoutParams(56.dp(), 4.dp()).apply { topMargin = 20.dp() })
        content.addView(home)

        // Pill button
        val pill = TextView(this).apply {
            text = "apps  ↑"
            textSize = 16f
            setTextColor(CREAM)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(28.dp(), 14.dp(), 28.dp(), 14.dp())
            background = GradientDrawable().apply {
                setColor(WALNUT); cornerRadius = 100.dp().toFloat()
            }
            setOnClickListener { showDrawer() }
        }
        content.addView(pill, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.START
        ).apply { setMargins(36.dp(), 0, 0, 44.dp()) })

        // App drawer
        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CREAM)
            setPadding(36.dp(), 28.dp(), 36.dp(), 0)
            visibility = View.GONE
            addView(TextView(context).apply {
                text = "apps"
                textSize = 40f
                setTextColor(ORANGE)
                typeface = Typeface.create("serif", Typeface.BOLD)
            })
            addView(View(context).apply { setBackgroundColor(TEAL) },
                LinearLayout.LayoutParams(56.dp(), 4.dp()).apply { topMargin = 8.dp(); bottomMargin = 12.dp() })
            addView(ListView(context).apply {
                divider = null
                setAdapter(adapter)
                isVerticalScrollBarEnabled = false
            })
        }
        content.addView(drawer, FrameLayout.LayoutParams(-1, -1))

        gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (vy < -900 && drawer.visibility != View.VISIBLE) { showDrawer(); return true }
                return false
            }
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(q, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        adapter.notifyDataSetChanged()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gd.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) hideDrawer(false)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) hideDrawer(true)
    }

    private fun showDrawer() {
        drawer.alpha = 0f
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).setDuration(180).start()
    }

    private fun hideDrawer(animate: Boolean) {
        if (animate) drawer.animate().alpha(0f).setDuration(150)
            .withEndAction { drawer.visibility = View.GONE }.start()
        else { drawer.animate().cancel(); drawer.visibility = View.GONE }
    }

    inner class AppAdapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val ri = apps[p]
            val pm = packageManager
            return LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10.dp(), 0, 10.dp())
                addView(ImageView(context).apply { setImageDrawable(ri.loadIcon(pm)) },
                    LinearLayout.LayoutParams(44.dp(), 44.dp()))
                addView(TextView(context).apply {
                    text = ri.loadLabel(pm)
                    textSize = 20f
                    setTextColor(WALNUT)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(20.dp(), 0, 0, 0)
                })
                setOnClickListener {
                    startActivity(
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                            .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    hideDrawer(false)
                }
            }
        }
    }
}
