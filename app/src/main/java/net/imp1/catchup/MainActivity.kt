package net.imp1.catchup

import android.Manifest
import android.content.ContentUris
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
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList
import android.content.pm.PackageManager
import android.content.Intent
import android.content.pm.ResolveInfo
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
    AppCompatActivity() {

    private lateinit var contactAdapter : ContactListAdapter
    private lateinit var contacts : ArrayList<Contact>

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
            setupDefaultCatchUpContactDetails()
        }

        contacts.sortBy {
            it.lastContacted ?: LocalDate.MIN
        }

        val list : ListView = findViewById(R.id.list)
        contactAdapter = ContactListAdapter(this, contacts)

        list.adapter = contactAdapter
        contactAdapter.notifyDataSetChanged()

        // TODO: Debugging to find permissions relevant to communications apps
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val interestingPermissions = arrayOf(
            "android.permission.CALL_PHONE",
            "android.permission.SEND_SMS",
            "android.permission.WRITE_SMS"
        )

        for (applicationInfo in packages) {
            if (applicationInfo.name == null) { continue }
            try {
                val packageInfo =
                    pm.getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS)
                //Get Permissions
                val relevantPermissions = packageInfo.requestedPermissions?.filter {
                    it in interestingPermissions
                } ?: ArrayList<String>()
                if (relevantPermissions.isNotEmpty()){
                    Log.d("test","App: " + applicationInfo.name + " Package: " + applicationInfo.packageName)
                    for (p in relevantPermissions) {
                        Log.d(applicationInfo.name, p)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            var contactMethodString : String? = null
            contact.contactMethod?.let {
                contactMethodString = it.name
            }
            var contactAddressString : String? = null
            contact.address?.let {
                contactAddressString = it
            }
            obj.put(Contact.ID, contact.id)
            obj.put(Contact.LAST_CONTACTED, lastContactString)
            obj.put(Contact.CONTACT_METHOD, contactMethodString)
            obj.put(Contact.ADDRESS, contactAddressString)
            list.put(obj)
        }
        openFileOutput(CONTACT_INFO_FILENAME, MODE_PRIVATE)?.use {
            it.write(list.toString(4).toByteArray(Charsets.UTF_8))
        }
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
            if (item.has(Contact.CONTACT_METHOD)) {
                val contactMethodString = item.getString(Contact.CONTACT_METHOD)
                contact.contactMethod = ContactMethod.valueOf(contactMethodString)
            }
            if (item.has(Contact.ADDRESS)) {
                contact.address = item.getString(Contact.ADDRESS)
            }
        }
    }

    private fun setupDefaultCatchUpContactDetails() {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = null
        val selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?"
        contacts.forEach { contact ->
            val args : Array<String> = arrayOf(contact.id.toString())
            contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    contact.address = number
                    contact.contactMethod = ContactMethod.TELEPHONE
                }
            }
        }
    }

    fun moreActions(view: View) {
        val position = view.tag as Int
        val listener = ContactMoreActionsListener(contacts[position], this)
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        popup.setOnMenuItemClickListener(listener)
        inflater.inflate(R.menu.contact_menu, popup.menu)
        popup.show()
    }

    fun viewContact(view: View) {
        val position = view.tag as Int
        val id = contacts[position].id.toString()
        val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
        val intent = Intent(Intent.ACTION_VIEW, contactUri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun refreshList() {
        contacts.sortBy {
            it.lastContacted ?: LocalDate.MIN
        }
        contactAdapter.notifyDataSetChanged()
    }

    fun reset(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.lastContacted = LocalDate.now()
            contactAdapter.notifyDataSetChanged()
            refreshList()
        }
    }

    fun catchUp(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.lastContacted = LocalDate.now()
            contactAdapter.notifyDataSetChanged()
            val protocol = con.contactMethod?.protocol ?: "tel"
            val address = con.address!!
            val uri = Uri.parse("$protocol:$address")
            val action = con.contactMethod?.action ?: Intent.ACTION_SEND

            val intent = Intent(action, uri)

            con.contactMethod?.packageHint?.let {
                intent.setPackage(it)
            }

            val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
            val isIntentSafe: Boolean = activities.isNotEmpty()

            if (isIntentSafe) {
                startActivity(intent)
                refreshList()
            } else {
                val name = con.name
                val message = "Couldn't find an app to use to catch up with $name"
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                Log.e("contact", con.name)
                Log.e("method", con.contactMethod.toString())
                Log.e("uri", uri.toString())
            }
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
                    val contactMethod: ContactMethod? = null
                    val address: String? = null
                    val contact = Contact(id, name, photo, lastContact, contactMethod, address)
                    contactDetails.add(contact)
                } while (cursor.moveToNext())
            }
        }
        return contactDetails
    }


}
