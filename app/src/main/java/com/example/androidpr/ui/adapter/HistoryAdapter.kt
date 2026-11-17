package com.example.androidpr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpr.data.db.AirQualityRecord
import com.example.androidpr.databinding.ItemHistoryRecordBinding
import com.example.androidpr.ui.util.AqiUtils

class HistoryAdapter(
    private val onDeleteClick: (AirQualityRecord) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<AirQualityRecord>()

    fun submitList(newItems: List<AirQualityRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemHistoryRecordBinding,
        private val onDeleteClick: (AirQualityRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: AirQualityRecord) {
            binding.tvHistoryAqi.text = "AQI ${record.aqi}"
            binding.tvHistoryCategory.text = record.category
            binding.tvHistoryDate.text = record.dateFormatted
            binding.tvHistoryLocation.text = record.location
            binding.tvHistoryPollutants.text =
                "PM2.5: ${record.pm25} | PM10: ${record.pm10} | CO: ${record.co}"

            val colorRes = AqiUtils.getColorResId(record.aqi)
            binding.tvHistoryCategory.backgroundTintList =
                ContextCompat.getColorStateList(binding.root.context, colorRes)

            binding.btnDeleteHistory.setOnClickListener {
                onDeleteClick(record)
            }
        }
    }
}
