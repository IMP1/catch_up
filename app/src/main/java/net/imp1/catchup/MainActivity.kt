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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.content.pm.PackageManager
import android.content.Intent
import android.content.pm.ResolveInfo


const val CONTACT_INFO_FILENAME = "contacts.json"
val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)

class MainActivity :
    AppCompatActivity() {

    private lateinit var contactAdapter : ContactListAdapter
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

        // TODO: order the contacts
        contacts.sortBy {
            it.lastContacted ?: Date(0L)
        }

        val list : ListView = findViewById(R.id.list)
        contactAdapter = ContactListAdapter(this, contacts)

        list.adapter = contactAdapter

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
            // TODO: load contact method and address
            // TODO: give all contacts a contactMethod and address
        }
    }

    fun reset(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.updateLastContacted()
            contactAdapter.notifyDataSetChanged()
        }
    }

    fun catchUp(view : View) {
        val position = view.tag as Int
        val contact = contactAdapter.getItem(position)
        contact?.let { con ->
            con.updateLastContacted()
            contactAdapter.notifyDataSetChanged()
            val protocol = con.contactMethod ?: "tel" // TODO: remove when this can't be null
            val address = con.address ?: "07411056431" // TODO: remove when this can't be null
            val uri = Uri.parse("$protocol:$address")
            val action = when (protocol) {
                "tel" -> Intent.ACTION_DIAL
                else -> Intent.ACTION_SEND
            }
            val intent = Intent(action, uri)

            val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
            val isIntentSafe: Boolean = activities.isNotEmpty()

            if (isIntentSafe) {
                startActivity(intent)
            } else {
                val name = con.name
                val message = "Couldn't find an app to use to catch up with $name"
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
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
                val iconUri : Uri? = null
                val lastContact : Date? = null
                val contactMethod : String? = null
                val address : String? = null
                val contact = Contact(id, name, iconUri, lastContact, contactMethod, address)
                contactDetails.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contactDetails
    }


}
