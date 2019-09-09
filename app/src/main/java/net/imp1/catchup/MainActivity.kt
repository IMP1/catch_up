package net.imp1.catchup

import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View

import android.util.Log
import android.widget.*

private const val CONTACT_ID_INDEX : Int = 0
private const val CONTACT_KEY_INDEX : Int = 1

class MainActivity :
    AppCompatActivity(),
    AdapterView.OnItemClickListener {

    lateinit var contactList : ListView
    var contactId : Long = 0
    var contactKey : String? = null
    var contactUri : Uri? = null
    lateinit var arrayAdapter : ContactListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list)
        val contactDetails = getContactDetails(getContactIds())
        Log.e("contact_names", contactDetails.joinToString("\n"))
        arrayAdapter = ContactListAdapter(this, contactDetails)
        contactList = ListView(this)
        contactList.onItemClickListener = this
        contactList.adapter = arrayAdapter
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Toast.makeText(applicationContext, "Hello World", Toast.LENGTH_LONG).show()
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

    private fun getContactDetails(contctIds : List<Long>) : ArrayList<String> {
        val contactList = ArrayList<String>()
        contactList.add("Foo")
        contactList.add("Bar")
        return contactList
    }

}
