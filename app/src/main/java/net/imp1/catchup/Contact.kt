package net.imp1.catchup

import android.net.Uri
import java.util.*

data class Contact (
    val id: Long,
    var name: String,
    var icon_uri: Uri?,
    var lastContacted: Date?,
    var contactMethod: String?)