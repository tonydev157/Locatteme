package com.tonymen.locatteme.model

data class EcuadorLocations(
    val provinces: List<Province>
)

data class Province(
    val name: String,
    val cities: List<String>
)
