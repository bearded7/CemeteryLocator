package com.example.cemeterylocator.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cemeterylocator.databinding.ItemSubmissionBinding
import com.example.cemeterylocator.model.GraveRequest

class SubmissionAdapter : ListAdapter<GraveRequest, SubmissionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.textName.text = item.name
        holder.binding.textSubmittedAt.text = item.submittedAt ?: ""
        holder.binding.textStatus.text = item.status.replaceFirstChar { it.uppercase() }
        holder.binding.textStatus.setTextColor(
            when (item.status) {
                "approved" -> Color.parseColor("#2E7D32")
                "rejected" -> Color.parseColor("#C62828")
                else -> Color.parseColor("#B8860B")
            }
        )
        holder.binding.textNote.text = when {
            item.status == "approved" && item.resultingGraveNo != null ->
                "Added as grave #${item.resultingGraveNo}"
            item.status == "rejected" && !item.adminNote.isNullOrBlank() ->
                "Reason: ${item.adminNote}"
            else -> ""
        }
        holder.binding.textNote.visibility =
            if (holder.binding.textNote.text.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GraveRequest>() {
            override fun areItemsTheSame(a: GraveRequest, b: GraveRequest) = a.requestId == b.requestId
            override fun areContentsTheSame(a: GraveRequest, b: GraveRequest) = a == b
        }
    }
}
