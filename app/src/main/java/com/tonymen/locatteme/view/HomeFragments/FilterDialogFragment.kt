package com.tonymen.locatteme.view.HomeFragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentFilterDialogBinding
import com.tonymen.locatteme.model.EcuadorLocations

class FilterDialogFragment(
    private val applyFilter: (startDate: String?, endDate: String?, status: String?, province: String?, city: String?) -> Unit
) : DialogFragment() {

    private var _binding: FragmentFilterDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var ecuadorLocations: EcuadorLocations

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilterDialogBinding.inflate(inflater, container, false)

        loadEcuadorLocations()

        setupSpinners()
        setupButtons()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun loadEcuadorLocations() {
        val inputStream = resources.openRawResource(R.raw.ecuador_locations)
        val json = inputStream.bufferedReader().use { it.readText() }
        ecuadorLocations = Gson().fromJson(json, EcuadorLocations::class.java)
    }

    private fun setupSpinners() {
        val statusOptions = listOf("--", "Desaparecido", "Localizado", "Muerto")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusSpinner.adapter = statusAdapter

        val provinceNames = listOf("--") + ecuadorLocations.provinces.map { it.name }
        val provinceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, provinceNames)
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.provinceSpinner.adapter = provinceAdapter

        binding.provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedProvince = ecuadorLocations.provinces.getOrNull(position - 1)
                val cityNames = listOf("--") + (selectedProvince?.cities ?: listOf())
                val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cityNames)
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.citySpinner.adapter = cityAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("--"))
                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.citySpinner.adapter = emptyAdapter
            }
        }
    }

    private fun setupButtons() {
        binding.applyButton.setOnClickListener {
            val startDate = binding.startDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val endDate = binding.endDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val status = binding.statusSpinner.selectedItem?.toString().takeIf { it != "--" }
            val province = binding.provinceSpinner.selectedItem?.toString().takeIf { it != "--" }
            val city = binding.citySpinner.selectedItem?.toString().takeIf { it != "--" }

            applyFilter(startDate, endDate, status, province, city)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
