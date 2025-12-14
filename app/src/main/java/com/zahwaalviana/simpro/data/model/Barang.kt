package com.zahwaalviana.simpro.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Barang(
    var barangId: String? = null,
    val namaBahanBaku: String = "",
    val namaBarangJadi: String = "",
    val total_stok: Int = 0

) : Parcelable