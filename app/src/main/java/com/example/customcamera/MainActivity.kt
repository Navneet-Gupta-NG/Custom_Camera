package com.example.customcamera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var takeTestButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        takeTestButton = findViewById(R.id.takeTestButton)

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val name = sharedPreferences.getString("name", "")
        val email = sharedPreferences.getString("email", "")

        if (!name.isNullOrEmpty() && !email.isNullOrEmpty()) {
            nameEditText.setText(name)
            emailEditText.setText(email)
        }

        fun String.isValidEmail() =
            isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

        takeTestButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()

            val editor = sharedPreferences.edit()
            editor.putString("name", name)
            editor.putString("email", email)
            editor.apply()

            if (email.isValidEmail()) {
                val intent = Intent(this, SingleImageScreen::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
