package net.imp1.catchup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class ContactListAdapter(context: Context, resource: Int) :
    ArrayAdapter<String>(context, resource) {

    private lateinit var values : ArrayList<String>

    constructor(context : Context, values: ArrayList<String>) : this(context, R.layout.contact_list) {
        this.values = values
    }

    override fun getCount(): Int {
        return values.size
    }

    @Override
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView : View = inflater.inflate(R.layout.contact_list_item, parent, false)

        val textView : TextView = rowView.findViewById(R.id.label)
        val imageView : ImageView = rowView.findViewById(R.id.icon)
        val contactName = values[position]
        textView.text = contactName
        imageView.setImageResource(R.drawable.ic_launcher_foreground)
        imageView.contentDescription = context.getString(R.string.contact_photo_description, contactName)

        return rowView
    }

}