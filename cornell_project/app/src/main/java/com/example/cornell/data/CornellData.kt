package com.example.cornell.data

import com.google.gson.annotations.SerializedName

data class CornellData(
    @SerializedName("asignatura") val asignatura: String = "General",
    @SerializedName("titulo") val titulo: String = "",
    @SerializedName("ideas_clave") val ideasClave: String = "",
    @SerializedName("notas_clase") val notasClase: String = "",
    @SerializedName("resumen") val resumen: String = ""
)
