package com.example.noteapp_lttbdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeletedNoteAdapter(
    private val onItemClick: (Note) -> Unit
) : RecyclerView.Adapter<DeletedNoteAdapter.DeletedViewHolder>() {

    private val deletedList = mutableListOf<Note>()

    fun submitList(newList: List<Note>) {
        deletedList.clear()
        deletedList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deleted_note, parent, false)
        return DeletedViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeletedViewHolder, position: Int) {
        val note = deletedList[position]
        holder.bind(note)
    }

    override fun getItemCount() = deletedList.size

    inner class DeletedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvDeletedTitle)
        private val tvContentPreview: TextView = itemView.findViewById(R.id.tvContentPreview)
        private val tvDeleteTime: TextView = itemView.findViewById(R.id.tvDeleteTime)

        fun bind(note: Note) {
            tvTitle.text = note.title.ifEmpty { "Không tiêu đề" }

            // Hiển thị preview nội dung (giới hạn 80 ký tự)
            val preview = if (note.content.length > 80) {
                note.content.substring(0, 77) + "..."
            } else {
                note.content
            }
            tvContentPreview.text = preview

            // TODO: Sau này thêm thời gian xóa nếu có cột deleted_at
            tvDeleteTime.text = "Đã xóa gần đây"

            itemView.setOnClickListener {
                onItemClick(note)
            }
        }
    }
}