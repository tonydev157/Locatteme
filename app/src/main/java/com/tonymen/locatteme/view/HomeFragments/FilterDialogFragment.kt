package com.tonymen.locatteme.view.HomeFragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentFilterDialogBinding
import com.tonymen.locatteme.model.EcuadorLocations
import java.text.SimpleDateFormat
import java.util.*

class FilterDialogFragment : DialogFragment() {

    private var _binding: FragmentFilterDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var ecuadorLocations: EcuadorLocations
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var applyFilter: ((String?, String?, String?, String?, String?, String?, String?) -> Unit)? = null

    companion object {
        fun newInstance(
            applyFilter: (
                startDisappearanceDate: String?, endDisappearanceDate: String?,
                startPublicationDate: String?, endPublicationDate: String?,
                status: String?, province: String?, city: String?
            ) -> Unit
        ): FilterDialogFragment {
            return FilterDialogFragment().apply {
                this.applyFilter = applyFilter
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilterDialogBinding.inflate(inflater, container, false)

        loadEcuadorLocations()

        setupSpinners()
        setupDatePickers()
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
                binding.citySpinner.adapter = null
            }
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dateSetListener = { editText: EditText, year: Int, month: Int, day: Int ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!selectedDate.after(today)) {
                editText.setText(dateFormat.format(selectedDate.time))
            } else {
                Toast.makeText(requireContext(), "La fecha no puede ser en el futuro", Toast.LENGTH_SHORT).show()
            }
        }

        binding.startDisappearanceDateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth -> dateSetListener(binding.startDisappearanceDateEditText, year, month, dayOfMonth) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = today.timeInMillis
            }.show()
        }

        binding.endDisappearanceDateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth -> dateSetListener(binding.endDisappearanceDateEditText, year, month, dayOfMonth) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = today.timeInMillis
            }.show()
        }

        binding.startPublicationDateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth -> dateSetListener(binding.startPublicationDateEditText, year, month, dayOfMonth) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = today.timeInMillis
            }.show()
        }

        binding.endPublicationDateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth -> dateSetListener(binding.endPublicationDateEditText, year, month, dayOfMonth) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = today.timeInMillis
            }.show()
        }
    }

    private fun setupButtons() {
        binding.applyButton.setOnClickListener {
            val startDisappearanceDate = binding.startDisappearanceDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val endDisappearanceDate = binding.endDisappearanceDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val startPublicationDate = binding.startPublicationDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val endPublicationDate = binding.endPublicationDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val status = binding.statusSpinner.selectedItem?.toString().takeIf { it != "--" }
            val province = binding.provinceSpinner.selectedItem?.toString().takeIf { it != "--" }
            val city = binding.citySpinner.selectedItem?.toString().takeIf { it != "--" }

            applyFilter?.invoke(startDisappearanceDate, endDisappearanceDate, startPublicationDate, endPublicationDate, status, province, city)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
