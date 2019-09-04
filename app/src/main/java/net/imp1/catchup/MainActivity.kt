package net.imp1.catchup

import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CursorAdapter
import android.widget.ListView
import android.widget.SimpleCursorAdapter

private const val CONTACT_ID_INDEX : Int = 0
private const val CONTACT_KEY_INDEX : Int = 1

class MainActivity :
    AppCompatActivity(),
    AdapterView.OnItemClickListener {

    lateinit var contactList : ListView
    var contactId : Long = 0
    var contactKey : String? = null
    var contactUri : Uri? = null

    private val CONTACT_NAME = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    } else {
        ContactsContract.Contacts.DISPLAY_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list)
        also {
            contactList = ListView(this)
            contactList.onItemClickListener = this
        }
        getContactIds()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val cursor : Cursor? = (parent?.adapter as? CursorAdapter)?.cursor?.apply {
            moveToPosition(position)
            contactId = getLong(CONTACT_ID_INDEX)
            contactKey = getString(CONTACT_KEY_INDEX)
            contactUri = ContactsContract.Contacts.getLookupUri(contactId, contactKey)
        }
    }

    private fun getCatchUpGroupId() : Long? {
        val catchUpGroupName = "catch-up"
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE
        )
        val selection = ContactsContract.Groups.TITLE + " = ?"
        val args = arrayOf(
            catchUpGroupName
        )
        val cursor = with(contentResolver) {
            query(
                ContactsContract.Groups.CONTENT_URI,
                projection,
                selection,
                args,
                null
            )
        } ?: return null

        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Groups._ID))
            cursor.close()
            return id
        }
        return null
    }

    private fun getContactIds() : ArrayList<Long> {
        val contactIds = ArrayList<Long>()
        val groupId = getCatchUpGroupId() ?: return contactIds
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
            // CONTACT_NAME
            //ContactsContract.Contacts.PHOTO_ID,
            //ContactsContract.CommonDataKinds.Photo.CONTACT_ID
        )
        val selection =
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ? AND " +
            ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + " = '" +
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'"
        val args = arrayOf(
            groupId.toString()
        )
        val cursor = with(contentResolver) {
            query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                args,
                null
            )
        } ?: return contactIds
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))
                contactIds.add(id)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contactIds
    }

}
