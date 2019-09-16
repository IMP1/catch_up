package net.imp1.catchup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.util.*
import java.util.concurrent.TimeUnit
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

    private fun getLastContactedTime(datetime: Date?) : String {
        val now = Calendar.getInstance().timeInMillis
        datetime?.let {
            val duration = now - datetime.time
            val days = TimeUnit.DAYS.convert(duration, TimeUnit.MILLISECONDS)
            val weeks = days / 7
            if (weeks > 0) {
                return context.getString(R.string.weeks_ago, weeks)
            }
            if (days > 0) {
                return context.getString(R.string.days_ago, days)
            }
            val hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS)
            if (hours > 0) {
                return context.getString(R.string.hours_ago, hours)
            }
            val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
            if (minutes > 0) {
                return context.getString(R.string.minutes_ago, minutes)
            }
            val seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
            if (seconds > 10) {
                return context.getString(R.string.seconds_ago, seconds)
            }
            return context.getString(R.string.just_now)
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
        val moreButton : Button = rowView.findViewById(R.id.more_btn)

        contactNameTextView.text = contact.name
        contactTimeTextView.text = getLastContactedTime(contact.lastContacted)
        if (contact.photo == null) {
            contactImageView.setImageResource(R.drawable.ic_person_black_128dp)
        } else {
            contactImageView.setImageBitmap(contact.photo)
        }
        contactImageView.contentDescription = context.getString(R.string.contact_photo_description, contact.name)

        catchUpButton.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_phone_black_24dp, 0, 0, 0)
            it.tag = position
        }
        resetButton.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_update_black_24dp, 0, 0, 0)
            it.tag = position
        }
        moreButton.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_more_vert_black_24dp, 0, 0, 0)
            it.tag = position
        }

        return rowView
    }

}