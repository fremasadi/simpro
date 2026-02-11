package com.zahwaalviana.simpro.data.model

data class MasterPengeluaran(
    val id: String = "",
    val namaPengeluaran: String = "",
    val kategori: String = ""
)

data class Pengeluaran(
    val id: String = "",
    val masterPengeluaranId: String = "",
    val produksiId: String = "",
    val tanggal: String = "",
    val keterangan: String = "",
    val biaya: Int = 0
)