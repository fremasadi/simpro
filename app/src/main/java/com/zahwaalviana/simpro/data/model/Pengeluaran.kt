package com.zahwaalviana.simpro.data.model

data class MasterPengeluaran(
    val id: String = "",
    val namaPengeluaran: String = "",
    val kategori: String = ""
)

data class RelatedItem(
    val id: String = "",
    val type: String = "", // "produksi", "barang", "kemasan"
    val name: String = ""
)

data class Pengeluaran(
    val id: String = "",
    val masterPengeluaranId: String = "",
    val relatedItems: List<RelatedItem> = emptyList(),
    val tanggal: String = "",
    val keterangan: String = "",
    val biaya: Int = 0
)
