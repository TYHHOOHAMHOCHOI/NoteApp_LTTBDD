package com.example.noteapp_lttbdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private var noteList: List<Note>,
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note, View) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        val tvNoteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        val tvItemReminderInfo: TextView = itemView.findViewById(R.id.tvItemReminderInfo)
        val ivLockIcon: ImageView = itemView.findViewById(R.id.ivLockIcon)
        val ivPinIcon: ImageView = itemView.findViewById(R.id.ivPinIcon)   // Icon ghim
        val tvNoteTag: TextView = itemView.findViewById(R.id.tvNoteTag)    // Label hiển thị tag
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]
        holder.tvNoteTitle.text = note.title
        
        if (note.isLocked) {
            holder.tvNoteContent.text = "Ghi chú đã bị khóa"
            holder.ivLockIcon.visibility = View.VISIBLE
        } else {
            holder.tvNoteContent.text = androidx.core.text.HtmlCompat.fromHtml(note.content, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
            holder.ivLockIcon.visibility = View.GONE
        }

        // Hiện icon ghim nếu ghi chú được ghim
        if (note.isPinned) {
            holder.ivPinIcon.visibility = View.VISIBLE
        } else {
            holder.ivPinIcon.visibility = View.GONE
        }

        if (note.isReminderEnabled && note.reminderTime > 0L) {
            holder.tvItemReminderInfo.visibility = View.VISIBLE
            holder.tvItemReminderInfo.text =
                "⏰ Nhắc lúc: ${formatReminderTime(note.reminderTime)} • ${getRepeatText(note.repeatType)}"
        } else {
            holder.tvItemReminderInfo.visibility = View.GONE
        }

        // Hiện label tag nếu ghi chú có tag, ẩn đi nếu tag rỗng
        if (note.tag.isNotEmpty()) {
            holder.tvNoteTag.visibility = View.VISIBLE
            holder.tvNoteTag.text = "#${note.tag}"  // Hiển thị dạng #tenTag
        } else {
            holder.tvNoteTag.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(note)
        }
        
        holder.itemView.setOnLongClickListener {
            onItemLongClick(note, it)
            true
        }
    }

    override fun getItemCount(): Int {
        return noteList.size
    }

    fun updateData(newList: List<Note>) {
        noteList = newList
        notifyDataSetChanged()
    }
    private fun formatReminderTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(timeMillis)
    }

    private fun getRepeatText(repeatType: String): String {
        return when (repeatType) {
            "daily" -> "Hằng ngày"
            "weekly" -> "Hằng tuần"
            else -> "Không lặp"
        }
    }

}
