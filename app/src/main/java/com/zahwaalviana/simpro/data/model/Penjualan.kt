package com.zahwaalviana.simpro.data.model

data class Penjualan(
    val id: String = "",
    val tanggal: Long = 0L,
    val totalHarga: Int = 0,
    val totalBayar: Int = 0,
    val kembalian: Int = 0
)

data class PenjualanItem(
    val id: String = "",
    val penjualanId: String = "",
    val varianId: String = "",
    val barangNama: String = "",
    val kemasanNama: String = "",
    val hargaSatuan: Int = 0,
    val jumlah: Int = 0,
    val subtotal: Int = 0
)

data class PenjualanWithItems(
    val penjualan: Penjualan,
    val items: List<PenjualanItem>
)
