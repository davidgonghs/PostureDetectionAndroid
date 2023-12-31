package com.posturedetection.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import com.posturedetection.android.data.LoginUser
import com.posturedetection.android.databinding.ActivityEditItemBinding
import com.posturedetection.android.util.ActivityCollector
import com.posturedetection.android.widget.TitleLayout

class EditItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditItemBinding

    private var loginUser: LoginUser = LoginUser.getInstance()

    private lateinit var tlTitle: TitleLayout

    private lateinit var etEditContent : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCollector.addActivity(this)

//        val sp: SharedPreferences = getSharedPreferences("account_settings", MODE_PRIVATE)
//        var account_settings_json = sp.getString("account_settings", null)
//        ChangeLanguageUtil().changeLanguage(account_settings_json?:"")


        tlTitle = binding.tlTitle
        etEditContent = binding.etEditContent
        var title = intent.getStringExtra("title")

        tlTitle.setTextView_title("Edit "+title)

        //following the extra then set the text
        when(title){
            "Name" -> etEditContent.setText(loginUser.username)
            "Email" -> etEditContent.setText(loginUser.email)
            "Phone" -> etEditContent.setText(loginUser.phone)
            else -> etEditContent.setText("Unknown")
        }

        //set input to login user
        tlTitle.iv_save.setOnClickListener {
            var input = etEditContent.text.toString()
            when(title){
                "Name" -> loginUser.username = input
                "Email" -> loginUser.email = input
                "Phone" -> loginUser.phone = input
            }
            setResult(RESULT_OK)
            finish()
        }

        tlTitle.iv_backward.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }
}