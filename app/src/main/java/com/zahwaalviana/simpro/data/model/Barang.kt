package com.zahwaalviana.simpro.data.model

data class Barang(
    val id: String = "",
    val namaBarang: String = "",
    val varian: List<BarangVarian> = emptyList()
)

data class BarangVarian(
    val id: String = "",
    val barangId: String = "",
    val kemasanId: String = "",
    val kemasanNama: String = "",
    val kemasanSatuan: String = "",
    val shelfLifeHari: Int = 0,
    val hargaJual: Int = 0
)

data class BarangWithVarian(
    val barang: Barang,
    val varianList: List<BarangVarian>
)
