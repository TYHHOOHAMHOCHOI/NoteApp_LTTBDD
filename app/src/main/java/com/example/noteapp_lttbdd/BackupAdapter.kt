package com.example.noteapp_lttbdd

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class BackupAdapter(private val onItemClick: (Uri) -> Unit) :
    RecyclerView.Adapter<BackupAdapter.BackupViewHolder>() {

    private var backupFiles: List<File> = emptyList()

    fun updateData(newFiles: List<File>) {
        backupFiles = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val file = backupFiles[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = backupFiles.size

    inner class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(file: File) {
            textView.text = file.name
            itemView.setOnClickListener {
                onItemClick(Uri.fromFile(file))
            }
        }
    }
}
