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
import java.text.SimpleDateFormat
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


const val CONTACT_INFO_FILENAME = "contacts.json"
const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)

class MainActivity :
    AppCompatActivity(),
    PopupMenu.OnMenuItemClickListener {

    private lateinit var contactAdapter : ContactListAdapter
    private lateinit var contacts : ArrayList<Contact>

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

        // For a hard reset, uncomment this out
        // TODO: remove this when all is working
        try {
            deleteFile(CONTACT_INFO_FILENAME)
        } catch (e : FileNotFoundException ) {}

        try {
            loadCatchUpContactDetails()
        } catch (e : FileNotFoundException ) {
            setupDefaultCatchUpContactDetails()
        }

        // TODO: order the contacts
        contacts.sortBy {
            it.lastContacted ?: Date(0L)
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
            // TODO: load contact method and address
            // TODO: give all contacts a contactMethod and address
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
                    contact.contactMethod = "tel"
                }
            }
        }
    }

    fun moreActions(view: View) {
        val position = view.tag as Int
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        popup.setOnMenuItemClickListener(this)
        inflater.inflate(R.menu.contact_menu, popup.menu)
        popup.show()
    }

    override fun onMenuItemClick(item : MenuItem) : Boolean {
        when (item.itemId) {
            R.id.action_change_contact_method -> {
                // TODO: A fragment / overlay view
            }
            R.id.action_change_last_contact -> {
                // TODO: date and time picker overlay / fragment
            }
            R.id.action_remove_contact -> {
                // TODO: remove contact from group
                // TODO: add toast with undo button
            }
        }
        return true
    }

    private fun refreshList() {
        contacts.sortBy {
            it.lastContacted ?: Date(0L)
        }
        contactAdapter.notifyDataSetChanged()
    }

    fun reset(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.updateLastContacted()
            contactAdapter.notifyDataSetChanged()
            refreshList()
        }
    }

    fun catchUp(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.updateLastContacted()
            contactAdapter.notifyDataSetChanged()
            val protocol = when(con.contactMethod) {
                "tel" -> "tel"
                "sms" -> "sms"
                "email" -> "mailto"
                "whatsapp" -> "smsto"
                "signal" -> "smsto"
                // TODO: add telegram to list
                // TODO: add skype to list
                // TODO: split whatsapp (and others) into text and call
                else -> "tel"
            }
            val address = con.address!!
            val uri = Uri.parse("$protocol:$address")
            val action = when (con.contactMethod) {
                "tel" -> Intent.ACTION_DIAL
                "sms" -> Intent.ACTION_VIEW
                "email" -> Intent.ACTION_SENDTO
                "whatsapp" -> Intent.ACTION_SENDTO
                "signal" -> Intent.ACTION_SENDTO
                else -> Intent.ACTION_SEND
            }
            val packageHint = when(con.contactMethod) {
                "whatsapp" -> "com.whatsapp"
                "signal" -> "org.thoughtcrime.securesms"
                else -> null
            }

            val intent = Intent(action, uri)

            packageHint?.let { intent.setPackage(it) }

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
                Log.e("method", uri.toString())
                Log.e("packageHint", packageHint.toString())
            }
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
                var photo : Bitmap? = null
                try {
                    val stream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver,
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id))
                    stream?.let {
                        photo = BitmapFactory.decodeStream(it)
                        stream.close()
                    }
                } catch (e : IOException) {}
                val lastContact : Date? = null
                val contactMethod : String? = null
                val address : String? = null
                val contact = Contact(id, name, photo, lastContact, contactMethod, address)
                contactDetails.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contactDetails
    }


}
