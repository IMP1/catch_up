package net.imp1.catchup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_list_view)
        nameEmailDetails()
    }

    private fun nameEmailDetails() : ArrayList<String> {
        val names = ArrayList<String>()
        val cr = contentResolver
        val cur = with(cr) {
            query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        } ?: return names
        if (cur.count > 0)
        {
            while (cur.moveToNext())
            {
                val id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))
                val cur1 = with(cr) {
                    query(
                        ContactsContract.Contacts.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                    )
                } ?: return names
                while (cur1.moveToNext())
                {
                    val name = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    Log.e("Name :", name)
                    names.add(name)
                }
                cur1.close()
            }
            cur.close()
        }
        return names
    }

}
