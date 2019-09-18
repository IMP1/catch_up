package net.imp1.catchup

import android.content.Intent

enum class ContactMethod(val protocol: String, val action: String, val packageHint: String? = null) {
    TELEPHONE("tel", Intent.ACTION_DIAL),
    SMS("sms", Intent.ACTION_VIEW),
    EMAIL("mailto", Intent.ACTION_SENDTO),
    WHATSAPP("smsto", Intent.ACTION_SENDTO, "com.whatsapp"),
    SIGNAL("smsto", Intent.ACTION_SENDTO, "org.thoughtcrime.securesms"),
    // TODO: add telegram to list
    // TODO: add skype to list
    // TODO: split whatsapp (and others) into text and call
}
