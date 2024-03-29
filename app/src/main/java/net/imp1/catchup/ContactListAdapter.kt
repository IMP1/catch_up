package net.imp1.catchup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.Period
import kotlin.collections.ArrayList

class ContactListAdapter(context: Context, resource: Int) :
    ArrayAdapter<Contact>(context, resource) {

    private lateinit var values : ArrayList<Contact>

    constructor(context : Context, values: ArrayList<Contact>) : this(context, R.layout.contact_list) {
        this.values = values
    }

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): Contact? {
        return values[position]
    }

    private fun getLastContactedTime(datetime: LocalDate?) : String {
        datetime?.let { then ->
            val now = LocalDate.now()
            val period = Period.between(then, now)
            val days = period.days
            val weeks = days / 7
            val years = period.years
            val months = period.months
            if (years > 0) {
                return context.resources.getQuantityString(R.plurals.years_ago, years, years)
            }
            if (months > 0) {
                return context.resources.getQuantityString(R.plurals.months_ago, months, months)
            }
            if (weeks > 0) {
                return context.resources.getQuantityString(R.plurals.weeks_ago, weeks, weeks)
            }
            if (days > 1) {
                return context.resources.getQuantityString(R.plurals.days_ago, days, days)
            }
            if (days > 0) {
                return context.getString(R.string.yesterday)
            }
            return context.getString(R.string.today)
        }
        return context.getString(R.string.never)
    }

    @Override
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.contact_list_item, parent, false)
        val contact = values[position]
        val contactNameTextView : TextView = rowView.findViewById(R.id.contact_name)
        val contactImageView : ImageView = rowView.findViewById(R.id.contact_image)
        val contactTimeTextView : TextView = rowView.findViewById(R.id.last_contacted)
        val catchUpButton : Button = rowView.findViewById(R.id.catch_up_btn)
        val resetButton : Button = rowView.findViewById(R.id.reset_btn)

        contactNameTextView.text = contact.name
        contactNameTextView.tag = position

        contactTimeTextView.text = getLastContactedTime(contact.lastContacted)
        contactTimeTextView.tag = position

        if (contact.photo == null) {
            contactImageView.setImageResource(R.drawable.ic_person_black_128dp)
        } else {
            contactImageView.setImageBitmap(contact.photo)
        }
        contactImageView.contentDescription = context.getString(R.string.contact_photo_description, contact.name)
        contactImageView.tag = position

        catchUpButton.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_baseline_beenhere_24, 0, 0, 0)
            it.tag = position
        }
        resetButton.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_update_black_24dp, 0, 0, 0)
            it.tag = position
        }
        rowView.tag = position
        return rowView
    }

}