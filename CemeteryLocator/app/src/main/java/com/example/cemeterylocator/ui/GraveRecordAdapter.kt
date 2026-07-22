package com.example.cemeterylocator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cemeterylocator.databinding.ItemGraveRecordBinding
import com.example.cemeterylocator.model.GraveRecord

class GraveRecordAdapter(
    private val onClick: (GraveRecord) -> Unit
) : ListAdapter<GraveRecord, GraveRecordAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemGraveRecordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGraveRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.textName.text = item.name
        holder.binding.textMeta.text = buildString {
            append("Grave #").append(item.graveNo)
            if (!item.location.isNullOrBlank()) append(" • ").append(item.location)
        }
        holder.binding.imageThumb.load(item.photoUrl) {
            crossfade(true)
        }
        holder.binding.root.setOnClickListener { onClick(item) }

        if (item.lat != null && item.lng != null) {
            holder.binding.buttonDirections.visibility = android.view.View.VISIBLE
            holder.binding.buttonDirections.setOnClickListener {
                DirectionsHelper.openExternalDirections(
                    holder.itemView.context, item.lat, item.lng, item.name
                )
            }
        } else {
            holder.binding.buttonDirections.visibility = android.view.View.GONE
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GraveRecord>() {
            override fun areItemsTheSame(a: GraveRecord, b: GraveRecord) = a.graveNo == b.graveNo
            override fun areContentsTheSame(a: GraveRecord, b: GraveRecord) = a == b
        }
    }
}
