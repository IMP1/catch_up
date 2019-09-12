package net.imp1.catchup

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlin.collections.ArrayList

val CONTACT_INFO_FILENAME = "contacts.json"
val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)

class MainActivity :
    AppCompatActivity(),
    AdapterView.OnItemClickListener {

    private lateinit var arrayAdapter : ContactListAdapter
    private lateinit var contacts : ArrayList<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list)

        contacts = getContactDetails()

        try {
            loadContactDetails()
        } catch (e : FileNotFoundException ) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }

        val list : ListView = findViewById(R.id.list)
        arrayAdapter = ContactListAdapter(this, contacts)

        list.onItemClickListener = this
        list.adapter = arrayAdapter
    }

    override fun onStop() {
        super.onStop()

        saveContactDetails()
    }

    private fun saveContactDetails() {
        val list = JSONArray()
        contacts.forEach {contact ->
            val obj = JSONObject()
            var lastContactString : String? = null
            contact.lastContacted?.let {
                lastContactString = DATE_FORMATTER.format(it)
            }
            var contactMethodString : String? = null
            contact.contactMethod?.let {
                contactMethodString = it
            }
            obj.put("id", contact.id)
            obj.put("last_contacted", lastContactString)
            obj.put("contact_method", contactMethodString)
            list.put(obj)
        }
        openFileOutput(CONTACT_INFO_FILENAME, MODE_PRIVATE)?.use {
            it.write(list.toString(4).toByteArray(Charsets.UTF_8))
        }
    }

    @Throws(FileNotFoundException::class)
    private fun loadContactDetails() {
        var jsonString = ""
        openFileInput(CONTACT_INFO_FILENAME)?.use {
            val buffer = ByteArray(it.available())
            it.read(buffer)
            jsonString = String(buffer)
        }
        val list = JSONArray(jsonString)
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val id = item.getLong("id")
            val contact = contacts.find { it.id == id } ?: continue
            if (item.has("last_contacted")) {
                val lastContactString = item.getString("last_contacted")
                try {
                    val lastContactDate = DATE_FORMATTER.parse(lastContactString)
                    contact.lastContacted = lastContactDate ?: contact.lastContacted
                } catch (e : ParseException) {
                    contact.lastContacted = null
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapter = parent?.adapter as ContactListAdapter
        val contact = adapter.getItem(position)
        contact?.let { con ->
            con.updateLastContacted()
            adapter.notifyDataSetChanged()
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

    private fun getContactDetails() : ArrayList<Contact> {
        val contactDetails = ArrayList<Contact>()
        val groupId = getCatchUpGroupId() ?: return contactDetails
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI
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
        } ?: return contactDetails
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))
                val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val iconUri : Uri? = null
                val lastContact : Date? = null
                val contactMethod : String? = null
                val contact = Contact(id, name, iconUri, lastContact, contactMethod)
                contactDetails.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contactDetails
    }


}
