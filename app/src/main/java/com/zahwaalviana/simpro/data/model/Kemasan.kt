package com.zahwaalviana.simpro.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Kemasan(
    var kemasanId: String? = null,
    val namaKemasan: String = "",
    val barangId: String = "",
    val namaBarangJadi: String = "" // Untuk display, tidak disimpan di Firestore
) : Parcelable