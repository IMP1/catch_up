package net.imp1.catchup

import android.graphics.Bitmap
import java.time.LocalDate

data class Contact (
    val id: Long,
    var name: String,
    var photo: Bitmap?,
    var lastContacted: LocalDate?,
    var contactMethod: ContactMethod?,
    var address: String?) {

    companion object {
        const val ID = "id"
        const val LAST_CONTACTED = "last_contacted"
        const val CONTACT_METHOD = "contact_method"
        const val ADDRESS = "address"
    }

}