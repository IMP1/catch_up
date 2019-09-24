package net.imp1.catchup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class EditContactMethodActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact_method)
        val contact_id = intent.getLongExtra(Contact.ID, 0L)
        // TODO: Get contact details from ID
        // TODO: Populate contact info with details
    }
}
