package com.example.maps.adapters

import android.graphics.Path
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.maps.R
import com.example.maps.databinding.ItemViewpagerBinding
import com.example.maps.models.Direction

class ViewPageAdapter(private val directionList: List<Direction>): RecyclerView.Adapter<ViewPageAdapter.ViewPagerViewHolder>() {

    inner class ViewPagerViewHolder(private val binding: ItemViewpagerBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(currentItem: Direction) {
                binding.apply {
                    direction = currentItem
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val binding = ItemViewpagerBinding.inflate(LayoutInflater.from(parent.context),
        parent, false)
        return ViewPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        val currentItem = directionList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int {
        return directionList.size
    }
}