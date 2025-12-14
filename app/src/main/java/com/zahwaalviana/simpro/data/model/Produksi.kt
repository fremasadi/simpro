package com.zahwaalviana.simpro.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Produksi(
    var produksiId: String? = null,
    var tanggal: Timestamp? = null,
    var barangId: String = "",
    var namaBarangJadi: String = "",
    var jumlah_produksi: Int = 0,
    var jumlah_keluar: Int = 0,
    var mandorId: String? = null // optional, bisa isi user id
) : Parcelable {
    fun net(): Int = jumlah_produksi - jumlah_keluar
}
