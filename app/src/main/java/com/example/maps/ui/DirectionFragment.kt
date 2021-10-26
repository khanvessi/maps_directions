package com.example.maps.ui

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.maps.R
import com.example.maps.adapters.ViewPageAdapter
import com.example.maps.databinding.FragmentDirectionBinding
import com.example.maps.ui.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class DirectionFragment : BottomSheetDialogFragment() {

    lateinit var binding: FragmentDirectionBinding
    val mainViewModel by activityViewModels<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_direction, container, false
        )
//        binding.employee = employeeViewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(mainViewModel.isMarkerClicked.value != true){
            //OBSEERVING TO ADD LIST FOR NEW DIRECTIONS
        mainViewModel.listOfDirection.observe(viewLifecycleOwner) {
            Log.e(TAG, "onViewCreated_list: CALLLLED" + mainViewModel.listOfDirection.value, )
            val adapterViewPager = mainViewModel.listOfDirection.value?.let { ViewPageAdapter(it) }
                binding.apply {
                    viewPager2.adapter = adapterViewPager
                    viewPager2.currentItem = mainViewModel.mDirectionList.lastIndex
                    circleIndicator.setViewPager(viewPager2)
                    viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            mainViewModel.currentSelectedPagePos.value = position
                        }
                    })
                }
            }
        }

        if(mainViewModel.isMarkerClicked.value == true) {
            //OBSERVING AFTER CLICKING ON MARKERS FROM MAP TO SHOW THEIR RESPECTIVE POSITIONS
            mainViewModel.markerTag.observe(viewLifecycleOwner) { markerTag ->
                val adapterViewPager =
                    mainViewModel.listOfDirection.value?.let { ViewPageAdapter(it) }
                binding.apply {
                    viewPager2.adapter = adapterViewPager
                    circleIndicator.setViewPager(viewPager2)
                    viewPager2.registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            mainViewModel.currentSelectedPagePos.value = position
                        }
                    })
                    mainViewModel.isMarkerClicked.value = false
                }

                Log.e(TAG, "onViewCreated_marker: CALLLLED" + mainViewModel.listOfDirection.value, )

                for (direction in mainViewModel.mDirectionList) {
                    if (direction.markerTag == markerTag) {
                        Log.e(TAG, "TAG: ${direction.address}")
                        Log.e(TAG, "MARKER TAG: $markerTag")
                        binding.viewPager2.currentItem = mainViewModel.mDirectionList.indexOf(direction)
                        Log.e(
                            TAG,
                            "onViewCreated: " + mainViewModel.mDirectionList.indexOf(direction)
                        )
                    }
                }
            }
        }


    }
}