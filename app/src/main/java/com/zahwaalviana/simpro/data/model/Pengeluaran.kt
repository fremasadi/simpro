package com.zahwaalviana.simpro.data.model

data class MasterPengeluaran(
    val id: String = "",
    val namaPengeluaran: String = "",
    val kategori: String = ""
)

data class Pengeluaran(
    val id: String = "",
    val masterPengeluaranId: String = "",
    // Link ke entitas lain bersifat Opsional (Bisa Null)
    val produksiId: String? = null,
    val barangId: String? = null,
    val kemasanId: String? = null,
    val tanggal: String = "",
    val keterangan: String = "",
    val biaya: Int = 0
)
