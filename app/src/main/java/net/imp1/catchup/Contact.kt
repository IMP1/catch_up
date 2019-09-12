package net.imp1.catchup

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Contact (
    @PrimaryKey val id: Long,
    @ColumnInfo(name="name") var name: String,
    @ColumnInfo(name="icon_uri") var icon_uri: Uri?,
    @ColumnInfo(name="last_contacted") var lastContacted: Date?,
    @ColumnInfo(name="contact_method") var contactMethod: String?)