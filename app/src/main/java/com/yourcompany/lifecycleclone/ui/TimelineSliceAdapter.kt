package com.yourcompany.lifecycleclone.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourcompany.lifecycleclone.R

class TimelineSliceAdapter : RecyclerView.Adapter<TimelineSliceAdapter.ViewHolder>() {

    private val items = mutableListOf<TimelineRow>()

    fun submitList(data: List<TimelineRow>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_slice, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorSwatch: View = itemView.findViewById(R.id.colorSwatch)
        private val labelText: TextView = itemView.findViewById(R.id.labelText)
        private val detailText: TextView = itemView.findViewById(R.id.detailText)

        fun bind(row: TimelineRow) {
            labelText.text = row.label
            detailText.text = row.detail
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(row.color)
            }
            colorSwatch.background = drawable
        }
    }
}

data class TimelineRow(
    val label: String,
    val detail: String,
    val color: Int
)
