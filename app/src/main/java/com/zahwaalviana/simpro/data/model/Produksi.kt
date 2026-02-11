package com.zahwaalviana.simpro.data.model

data class Produksi(
    val id: String = "",
    val tanggalProduksi: Long = 0L, // timestamp
    val mandorId: String = "",
    val mandorName: String = "",
    val items: List<ProduksiItem> = emptyList()
)

data class ProduksiItem(
    val id: String = "",
    val produksiId: String = "",
    val varianId: String = "",
    val barangNama: String = "",
    val kemasanNama: String = "",
    val kemasanSatuan: String = "",
    val jumlahProduksi: Int = 0,
    val expiredAt: Long = 0L, // timestamp: tanggal_produksi + shelf_life_hari
    val shelfLifeHari: Int = 0
)

data class ProduksiWithItems(
    val produksi: Produksi,
    val items: List<ProduksiItem>
)