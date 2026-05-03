package com.example.noteapp_lttbdd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var txtMonthYear: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var bottomNavigation: BottomNavigationView

    private var calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        val rootView = findViewById<View>(R.id.calendar_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        databaseHelper = DatabaseHelper(this)
        txtMonthYear = findViewById(R.id.txtMonthYear)
        recyclerView = findViewById(R.id.recyclerCalendar)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        recyclerView.layoutManager = GridLayoutManager(this, 7)

        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            loadCalendar()
        }

        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            loadCalendar()
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_calendar
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_calendar -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.navigation_calendar
        loadCalendar()
    }

    private fun loadCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN"))
        txtMonthYear.text = "tháng ${calendar.get(Calendar.MONTH) + 1} năm ${calendar.get(Calendar.YEAR)}"

        val days = mutableListOf<CalendarDay>()
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        tempCal.set(Calendar.HOUR_OF_DAY, 0)
        tempCal.set(Calendar.MINUTE, 0)
        tempCal.set(Calendar.SECOND, 0)
        tempCal.set(Calendar.MILLISECOND, 0)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val paddingDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(paddingDays) {
            days.add(CalendarDay(0, 0))
        }

        val monthStart = tempCal.timeInMillis

        val tempEnd = tempCal.clone() as Calendar
        tempEnd.set(Calendar.DAY_OF_MONTH, maxDay)
        tempEnd.set(Calendar.HOUR_OF_DAY, 23)
        tempEnd.set(Calendar.MINUTE, 59)
        tempEnd.set(Calendar.SECOND, 59)
        tempEnd.set(Calendar.MILLISECOND, 999)
        val monthEnd = tempEnd.timeInMillis

        val notesInMonth = databaseHelper.getNotesByDate(monthStart, monthEnd)
        val remindersInMonth = databaseHelper.getNotesByReminderRange(monthStart, monthEnd)

        for (i in 1..maxDay) {
            tempCal.set(Calendar.DAY_OF_MONTH, i)
            tempCal.set(Calendar.HOUR_OF_DAY, 0)
            tempCal.set(Calendar.MINUTE, 0)
            tempCal.set(Calendar.SECOND, 0)
            tempCal.set(Calendar.MILLISECOND, 0)
            val startOfDay = tempCal.timeInMillis

            tempCal.set(Calendar.HOUR_OF_DAY, 23)
            tempCal.set(Calendar.MINUTE, 59)
            tempCal.set(Calendar.SECOND, 59)
            tempCal.set(Calendar.MILLISECOND, 999)
            val endOfDay = tempCal.timeInMillis

            val count = notesInMonth.count { it.createdAt in startOfDay..endOfDay }
            val hasReminder = remindersInMonth.any { it.reminderTime in startOfDay..endOfDay }
            days.add(CalendarDay(i, count, hasReminder))
        }

        recyclerView.adapter = CalendarAdapter(days) { day ->
            val intent = Intent(this, DayNotesActivity::class.java)

            val dayCal = calendar.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val startTime = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val endTime = dayCal.timeInMillis

            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = "Ghi chú ngày ${sdfDate.format(dayCal.time)}"

            intent.putExtra("START_TIME", startTime)
            intent.putExtra("END_TIME", endTime)
            intent.putExtra("DATE_STRING", dateStr)
            startActivity(intent)
        }
    }
}