package com.example.androidpr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpr.databinding.ItemHourlyAqiBinding
import com.example.androidpr.ui.util.AqiUtils

data class HourlyItem(
    val hourLabel: String,
    val aqi: Int
)

class HourlyAqiAdapter : RecyclerView.Adapter<HourlyAqiAdapter.ViewHolder>() {

    private val items = mutableListOf<HourlyItem>()

    fun submitList(newItems: List<HourlyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyAqiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemHourlyAqiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyItem) {
            binding.tvHour.text = item.hourLabel
            binding.tvHourlyAqi.text = item.aqi.toString()

            val category = AqiUtils.getCategory(item.aqi)
            binding.tvHourlyCategory.text = category.label

            val colorRes = AqiUtils.getColorResId(item.aqi)
            binding.tvHourlyCategory.backgroundTintList =
                ContextCompat.getColorStateList(binding.root.context, colorRes)
        }
    }
}
