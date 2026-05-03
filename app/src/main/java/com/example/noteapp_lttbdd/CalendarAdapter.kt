package com.example.noteapp_lttbdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(private val days: List<CalendarDay>) :
    RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDay: TextView = view.findViewById(R.id.txtDay)
        val txtNote: TextView = view.findViewById(R.id.txtNoteCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = days[position]
        
        if (item.day == 0) {
            holder.txtDay.text = ""
            holder.txtNote.text = ""
            holder.itemView.visibility = View.INVISIBLE
        } else {
            holder.txtDay.text = item.day.toString()
            holder.txtNote.text = if (item.noteCount > 0) "${item.noteCount} notes" else ""
            holder.itemView.visibility = View.VISIBLE
        }
    }
}
