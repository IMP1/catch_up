package net.imp1.catchup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ContactListAdapter(context: Context, resource: Int) :
    ArrayAdapter<String>(context, resource) {

    private lateinit var values : ArrayList<String>

    constructor(context : Context, values: ArrayList<String>) : this(context, R.layout.contact_list) {
        this.values = values
    }

    override fun getCount(): Int {
        return values.size
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
        val inflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView : View = inflater.inflate(R.layout.contact_list_item, parent, false)
        val contact = values[position]
        val contactNameTextView : TextView = rowView.findViewById(R.id.contact_name)
        val contactImageView : ImageView = rowView.findViewById(R.id.contact_image)
        val contactTimeTextView : TextView = rowView.findViewById(R.id.last_contacted)

        contactNameTextView.text = contact.name
        contactTimeTextView.text = getLastContactedTime(contact.lastContacted)
        contactImageView.setImageResource(R.drawable.ic_launcher_foreground)
        contactImageView.contentDescription = context.getString(R.string.contact_photo_description, contact.name)

        return rowView
    }

}