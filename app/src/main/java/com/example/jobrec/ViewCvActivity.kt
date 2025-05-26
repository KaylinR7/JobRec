package com.example.jobrec
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.graphics.Typeface
class ViewCvActivity : AppCompatActivity() {
    private lateinit var cvContentText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_cv)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Resume"
        }
        cvContentText = findViewById(R.id.cvContentText)
        val cvContent = intent.getStringExtra("cvContent")
        if (cvContent != null) {
            cvContentText.text = formatCvContent(cvContent)
        } else {
            cvContentText.text = "No resume content available"
        }
    }
    private fun formatCvContent(content: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val lines = content.split("\n")
        for (line in lines) {
            when {
                line.endsWith(":") -> {
                    val spannable = SpannableString(line + "\n")
                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, line.length, 0)
                    builder.append(spannable)
                }
                line.contains(" at ") || line.contains(" - ") -> {
                    val spannable = SpannableString(line + "\n")
                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, line.length, 0)
                    builder.append(spannable)
                }
                else -> {
                    builder.append(line + "\n")
                }
            }
        }
        return builder
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 