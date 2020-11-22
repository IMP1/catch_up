package net.imp1.catchup

import android.content.Intent
import android.provider.ContactsContract
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import android.app.DatePickerDialog;
import android.view.View
import android.widget.DatePicker
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDate
import java.time.Month
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
                showDatePicker()
                activity.refreshList()
            }
            R.id.action_remove_contact -> {
                val context = activity.applicationContext
                val undoBar = Snackbar.make(item.actionView,
                        context.getString(R.string.removed_contact, contact.name),
                        Snackbar.LENGTH_LONG)
                undoBar.setAction(context.getString(R.string.undo), View.OnClickListener {
                    Toast.makeText(context,
                            context.getString(R.string.restored_contact, contact.name),
                            Toast.LENGTH_SHORT).show()
                })
                undoBar.show()
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

    private fun showDatePicker() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)
        val dateFragment = DatePickerDialog(activity, this, year, month, day)
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
        contact.lastContacted = date
        activity.refreshList()
    }

}