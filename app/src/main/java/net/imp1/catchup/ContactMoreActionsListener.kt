package net.imp1.catchup

import android.provider.ContactsContract
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast

class ContactMoreActionsListener(private val contact : Contact, private val activity: MainActivity) : PopupMenu.OnMenuItemClickListener {

    override fun onMenuItemClick(item: MenuItem): Boolean {
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
                removeContact(contact)
                activity.refreshList()
            }
        }
        return true
    }


    private fun removeContact(contact : Contact) {
        val id = contact.id
        val mimeType = ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
        val where = ContactsContract.Groups.TITLE + " = ? AND " +
                ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID + " = ? AND " +
                ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + " = '$mimeType'"
        val args = arrayOf(activity.catchupGroup, id.toString())
        try {
            activity.contentResolver.delete(ContactsContract.Data.CONTENT_URI, where, args)
        } catch (e : java.lang.Exception) {
            Toast.makeText(activity, e.toString(), Toast.LENGTH_LONG).show()
            Log.e("remove_contact_error", e.toString())
        }
    }


}