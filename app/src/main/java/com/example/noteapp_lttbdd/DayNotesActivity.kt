package com.example.noteapp_lttbdd

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DayNotesActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tvDateTitle: TextView
    
    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    private var startTime: Long = 0
    private var endTime: Long = 0
    private var dateString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_day_notes)

        startTime = intent.getLongExtra("START_TIME", 0)
        endTime = intent.getLongExtra("END_TIME", 0)
        dateString = intent.getStringExtra("DATE_STRING") ?: ""

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tvDateTitle = findViewById(R.id.tvDateTitle)
        
        tvDateTitle.text = dateString

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int) = when (position) {
                0 -> NoteListFragment.newInstance(startTime, endTime, false)
                else -> NoteListFragment.newInstance(startTime, endTime, true)
            }
        }
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Ghi chú đã tạo"
                else -> "Nhắc hẹn trong ngày"
            }
        }.attach()
    }
}