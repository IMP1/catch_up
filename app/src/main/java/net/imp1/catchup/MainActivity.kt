package net.imp1.catchup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.util.Log
import android.widget.*

class MainActivity :
    AppCompatActivity(),
    AdapterView.OnItemClickListener {

    private lateinit var arrayAdapter : ContactListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list)

        val list : ListView = findViewById(R.id.list)
        val contactDetails = getContactDetails(getContactIds())
        arrayAdapter = ContactListAdapter(this, contactDetails)

        list.onItemClickListener = this
        list.adapter = arrayAdapter
    }

    override fun onStop() {
        super.onStop()
        // TODO: save data
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

    private fun getContactDetails(contactIds : List<Long>) : ArrayList<ContactDetails> {
        Log.d("contact_ids", contactIds.joinToString { it.toString() + "\n" })
        val contactList = ArrayList<ContactDetails>()
        contactIds.forEach {
            val contact = ContactDetails(it, "Foo", null, null, null)
            contactList.add(contact)
        }
        return contactList
    }

}
