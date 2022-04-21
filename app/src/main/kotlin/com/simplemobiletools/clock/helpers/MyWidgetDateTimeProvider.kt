package com.simplemobiletools.clock.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SplashActivity
import com.simplemobiletools.clock.extensions.config
import com.simplemobiletools.clock.extensions.formatTo12HourFormat
import com.simplemobiletools.clock.extensions.getFormattedDate
import com.simplemobiletools.clock.extensions.getNextAlarm
import com.simplemobiletools.commons.extensions.*
import java.util.*

class MyWidgetDateTimeProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            RemoteViews(context.packageName, R.layout.widget_date_time).apply {
                updateTexts(context, this)
                updateColors(context, this)
                setupAppOpenIntent(context, this)
                appWidgetManager.updateAppWidget(it, this)
            }
        }
    }

    private fun updateTexts(context: Context, views: RemoteViews) {
        val nextAlarm = getFormattedNextAlarm(context)
        views.apply {
            setText(R.id.widget_date, context.getFormattedDate(Calendar.getInstance()))
            setText(R.id.widget_next_alarm, nextAlarm)
            setVisibleIf(R.id.widget_alarm_holder, nextAlarm.isNotEmpty())
        }
    }

    private fun updateColors(context: Context, views: RemoteViews) {
        val config = context.config
        val widgetTextColor = config.widgetTextColor

        views.apply {
            applyColorFilter(R.id.widget_background, config.widgetBgColor)
            setTextColor(R.id.widget_text_clock, widgetTextColor)
            setTextColor(R.id.widget_date, widgetTextColor)
            setTextColor(R.id.widget_next_alarm, widgetTextColor)

            val bitmap = getMultiplyColoredBitmap(R.drawable.ic_clock_shadowed, widgetTextColor, context)
            setImageViewBitmap(R.id.widget_next_alarm_image, bitmap)
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, this::class.java)

    private fun setupAppOpenIntent(context: Context, views: RemoteViews) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            putExtra(OPEN_TAB, TAB_CLOCK)
            val pendingIntent = PendingIntent.getActivity(context, OPEN_APP_INTENT_ID, this, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_date_time_holder, pendingIntent)
        }
    }

    private fun getFormattedNextAlarm(context: Context): String {
        val nextAlarm = context.getNextAlarm()
        if (nextAlarm.isEmpty()) {
            return ""
        }

        val isIn24HoursFormat = !nextAlarm.endsWith(".")
        return when {
            DateFormat.is24HourFormat(context) && !isIn24HoursFormat -> {
                val dayTime = nextAlarm.split(" ")
                val times = dayTime[1].split(":")
                val hours = times[0].toInt()
                val minutes = times[1].toInt()
                val seconds = 0
                val isAM = dayTime[2].startsWith("a", true)
                val newHours = when {
                    hours == 12 && isAM -> 0
                    hours == 12 && !isAM -> 12
                    !isAM -> hours + 12
                    else -> hours
                }
                formatTime(false, true, newHours, minutes, seconds)
            }
            !DateFormat.is24HourFormat(context) && isIn24HoursFormat -> {
                val times = nextAlarm.split(" ")[1].split(":")
                val hours = times[0].toInt()
                val minutes = times[1].toInt()
                val seconds = 0
                context.formatTo12HourFormat(false, hours, minutes, seconds)
            }
            else -> nextAlarm
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        performUpdate(context)
    }

    private fun getMultiplyColoredBitmap(resourceId: Int, newColor: Int, context: Context): Bitmap {
        val options = BitmapFactory.Options()
        options.inMutable = true
        val bmp = BitmapFactory.decodeResource(context.resources, resourceId, options)
        val paint = Paint()
        val filter = PorterDuffColorFilter(newColor, PorterDuff.Mode.MULTIPLY)
        paint.colorFilter = filter
        val canvas = Canvas(bmp)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return bmp
    }
}
