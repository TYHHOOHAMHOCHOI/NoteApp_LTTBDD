package com.example.noteapp_lttbdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private var noteList: List<Note>,
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note, View) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        val tvNoteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        val ivLockIcon: ImageView = itemView.findViewById(R.id.ivLockIcon)
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
            holder.tvNoteContent.text = note.content
            holder.ivLockIcon.visibility = View.GONE
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
}
