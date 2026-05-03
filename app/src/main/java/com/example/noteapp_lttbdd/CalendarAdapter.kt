package com.example.noteapp_lttbdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutCircle: LinearLayout = view.findViewById(R.id.layoutCircle)
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
        val context = holder.itemView.context

        if (item.day == 0) {
            holder.itemView.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            return
        }

        holder.itemView.visibility = View.VISIBLE
        holder.txtDay.text = item.day.toString()

        // Hide note count as requested
        holder.txtNote.visibility = View.GONE

        if (item.hasReminder) {
            // Ngày có nhắc hẹn -> Hiển thị ô tròn màu đỏ
            holder.layoutCircle.setBackgroundResource(R.drawable.bg_calendar_day_reminder)
            holder.txtDay.setTextColor(ContextCompat.getColor(context, R.color.on_primary))
        } else if (item.noteCount > 0) {
            // Ngày có note -> Hiển thị ô tròn màu tím (primary)
            holder.layoutCircle.setBackgroundResource(R.drawable.bg_calendar_day_with_notes)
            holder.txtDay.setTextColor(ContextCompat.getColor(context, R.color.on_primary))
        } else {
            // Ngày không có note -> Ô tròn rỗng
            holder.layoutCircle.setBackgroundResource(R.drawable.bg_calendar_day_empty)
            holder.txtDay.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        holder.itemView.setOnClickListener {
            onItemClick(item.day)
        }
    }
}
