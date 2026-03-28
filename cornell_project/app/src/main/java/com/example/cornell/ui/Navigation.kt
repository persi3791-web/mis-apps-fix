package com.example.cornell.ui

object Routes {
    const val HOME = "home"
    const val MANAGE = "manage"
    const val RESULT = "result/{noteId}"
    const val FOLDER = "folder/{folderName}"

    fun result(noteId: String) = "result/$noteId"
    fun folder(folderName: String) = "folder/$folderName"
}
