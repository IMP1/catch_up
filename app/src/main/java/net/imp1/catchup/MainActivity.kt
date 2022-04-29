package net.imp1.catchup

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentUris
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter


const val CONTACT_INFO_FILENAME = "contacts.json"
const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK)

class MainActivity :
    AppCompatActivity(),
    DatePickerDialog.OnDateSetListener {

    private lateinit var contactAdapter : ContactListAdapter
    private lateinit var contacts : ArrayList<Contact>

    private var contactToReset : Contact? = null

    var catchupGroup = "catch-up"

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setup()
                } else {
                    finishAffinity()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                MY_PERMISSIONS_REQUEST_READ_CONTACTS)
        } else {
            setup()
        }
    }

    private fun setup() {
        // TODO: Load contact group from somewhere
        contacts = getContactDetails()

        // <remove>
        // TODO: remove this when all is working
        // For a hard reset, uncomment this out
//        try {
//            deleteFile(CONTACT_INFO_FILENAME)
//        } catch (e : FileNotFoundException ) {}
        // </remove>

        try {
            loadCatchUpContactDetails()
        } catch (e : FileNotFoundException ) {
            e.printStackTrace()
        }

        contacts.sortBy {
            it.lastContacted ?: LocalDate.MIN
        }

        val list : ListView = findViewById(R.id.list)
        contactAdapter = ContactListAdapter(this, contacts)

        list.adapter = contactAdapter
        contactAdapter.notifyDataSetChanged()

    }

    override fun onStop() {
        super.onStop()
        saveCatchUpContactDetails()
    }

    private fun saveCatchUpContactDetails() {
        val list = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject()
            var lastContactString : String? = null
            contact.lastContacted?.let {
                lastContactString = it.format(DATE_FORMATTER)
            }
            obj.put(Contact.ID, contact.id)
            obj.put(Contact.LAST_CONTACTED, lastContactString)
            list.put(obj)
        }
        openFileOutput(CONTACT_INFO_FILENAME, MODE_PRIVATE)?.use {
            it.write(list.toString(4).toByteArray(Charsets.UTF_8))
        }
        // TODO: Save contact group somewhere
    }

    @Throws(FileNotFoundException::class)
    private fun loadCatchUpContactDetails() {
        var jsonString = ""
        openFileInput(CONTACT_INFO_FILENAME)?.use {
            val buffer = ByteArray(it.available())
            it.read(buffer)
            jsonString = String(buffer)
        }
        val list = JSONArray(jsonString)
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val id = item.getLong(Contact.ID)
            val contact = contacts.find { it.id == id } ?: continue
            if (item.has(Contact.LAST_CONTACTED)) {
                val lastContactString = item.getString(Contact.LAST_CONTACTED)
                try {
                    val lastContactDate = LocalDate.parse(lastContactString)
                    contact.lastContacted = lastContactDate ?: contact.lastContacted
                } catch (e : ParseException) {
                    contact.lastContacted = null
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun viewContact(view: View) {
        if (view.tag == null) {
            println("Tag is null")
            println(view)
            println(view.tag)
            return
        }
        val position = view.tag as Int
        val id = contacts[position].id.toString()
        val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
        val intent = Intent(Intent.ACTION_VIEW, contactUri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun refreshList() {
        contacts.sortBy {
            it.lastContacted ?: LocalDate.MIN
        }
        contactAdapter.notifyDataSetChanged()
    }

    fun reset(view : View) {
        val position = view.tag as Int
        contactToReset = contacts[position]
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)
        val dateFragment = DatePickerDialog(this, this, year, month, day)
        dateFragment.datePicker.maxDate = now.timeInMillis
        dateFragment.show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        // onDateSet returns a month from 0-11, but LocalDate assumes a month 1-12
        // How ridiculous is that? But that's the reason for this silly conversion
        val datePickerToLocalDateMonthConversion = 1
        val date = LocalDate.of(year,
            month + datePickerToLocalDateMonthConversion,
            dayOfMonth)
        contactToReset?.let {
            it.lastContacted = date
            refreshList()
        }
        contactToReset = null
    }

    fun catchUp(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.lastContacted = LocalDate.now()
            contactAdapter.notifyDataSetChanged()
            refreshList()
        }
    }

    private fun getCatchUpGroupId() : Long? {
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE
        )
        val selection = ContactsContract.Groups.TITLE + " = ?"
        val args = arrayOf(
            catchupGroup
        )

        contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Groups._ID))
                cursor.close()
                return id
            }
        }
        return null
    }

    private fun getContactDetails() : ArrayList<Contact> {
        val contactDetails = ArrayList<Contact>()
        val groupId = getCatchUpGroupId() ?: return contactDetails
        val selection = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI
        )
        val condition =
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ? AND " +
            ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + " = '" +
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'"
        val args = arrayOf(
            groupId.toString()
        )
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            selection,
            condition,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id =
                        cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    var photo: Bitmap? = null
                    try {
                        val stream = ContactsContract.Contacts.openContactPhotoInputStream(
                            contentResolver,
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
                        )
                        stream?.let {
                            photo = BitmapFactory.decodeStream(it)
                            stream.close()
                        }
                    } catch (e: IOException) {
                    }
                    val lastContact: LocalDate? = null
                    val contact = Contact(id, name, photo, lastContact)
                    contactDetails.add(contact)
                } while (cursor.moveToNext())
            }
        }
        return contactDetails
    }

    fun setContactGroup(item: MenuItem) {

    }

    fun showAboutInfo(item: MenuItem) {
        // TODO: Add About Info Page?
    }

}
