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
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        txtMonthYear.text = sdf.format(calendar.time)

        val days = mutableListOf<CalendarDay>()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        tempCal.set(Calendar.HOUR_OF_DAY, 0)
        tempCal.set(Calendar.MINUTE, 0)
        tempCal.set(Calendar.SECOND, 0)
        tempCal.set(Calendar.MILLISECOND, 0)
        val monthStartTime = tempCal.timeInMillis

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        tempCal.set(Calendar.DAY_OF_MONTH, maxDay)
        tempCal.set(Calendar.HOUR_OF_DAY, 23)
        tempCal.set(Calendar.MINUTE, 59)
        tempCal.set(Calendar.SECOND, 59)
        tempCal.set(Calendar.MILLISECOND, 999)
        val monthEndTime = tempCal.timeInMillis

        // Fetch all notes for the month once to improve performance
        val notesInMonth = databaseHelper.getNotesByDate(monthStartTime, monthEndTime)

        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, 0))
        }

        for (i in 1..maxDay) {
            tempCal.set(Calendar.DAY_OF_MONTH, i)
            tempCal.set(Calendar.HOUR_OF_DAY, 0)
            tempCal.set(Calendar.MINUTE, 0)
            tempCal.set(Calendar.SECOND, 0)
            tempCal.set(Calendar.MILLISECOND, 0)
            val startTime = tempCal.timeInMillis
            
            tempCal.set(Calendar.HOUR_OF_DAY, 23)
            tempCal.set(Calendar.MINUTE, 59)
            tempCal.set(Calendar.SECOND, 59)
            tempCal.set(Calendar.MILLISECOND, 999)
            val endTime = tempCal.timeInMillis

            val count = notesInMonth.count { it.createdAt in startTime..endTime }
            days.add(CalendarDay(i, count))
        }

        recyclerView.adapter = CalendarAdapter(days)
    }
}
