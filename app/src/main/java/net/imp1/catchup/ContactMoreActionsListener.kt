package net.imp1.catchup

import android.content.Intent
import android.provider.ContactsContract
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import android.app.DatePickerDialog;
import android.widget.DatePicker
import java.time.LocalDate
import java.util.*


class ContactMoreActionsListener(private val contact : Contact, private val activity: MainActivity) :
    PopupMenu.OnMenuItemClickListener,
    DatePickerDialog.OnDateSetListener {

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_change_contact_method -> {
                val intent = Intent(activity, EditContactMethodActivity::class.java)
                intent.putExtra(Contact.ID, contact.id)
                activity.startActivity(intent)
                // TODO: Change from activity to fragment
            }
            R.id.action_change_last_contact -> {
                showDatePicker(contact)
                // TODO: date and time picker overlay / fragment
                activity.refreshList()
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


    private fun removeContact(contact: Contact) {
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

    private fun showDatePicker(contact: Contact) {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)
        // TODO: dismiss the popup window when touched
        val dateFragment = DatePickerDialog(activity, this, year, month, day)
        dateFragment.show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val date = LocalDate.of(year, month, dayOfMonth)
        contact.updateLastContacted(date)
        activity.refreshList()
    }

}