package com.example.jobrec

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfViewerActivity : AppCompatActivity() {


    private lateinit var toolbar: Toolbar
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "PdfViewerActivity"
        const val EXTRA_PDF_URL = "pdf_url"
        const val EXTRA_PDF_ID = "pdf_id"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        setupToolbar()
        initViews()
        loadPdf()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Resume"
        }
    }

    private fun initViews() {
        // No views to initialize - we directly open external PDF viewer
    }

    private fun loadPdf() {
        val pdfUrl = intent.getStringExtra(EXTRA_PDF_URL)
        val pdfId = intent.getStringExtra(EXTRA_PDF_ID)

        when {
            !pdfUrl.isNullOrEmpty() -> {
                if (pdfUrl.startsWith("http")) {
                    loadPdfFromUrl(pdfUrl)
                } else {
                    // Treat as document ID
                    loadPdfFromFirestore(pdfUrl)
                }
            }
            !pdfId.isNullOrEmpty() -> {
                loadPdfFromFirestore(pdfId)
            }
            else -> {
                Log.e(TAG, "No PDF URL or ID provided")
                Toast.makeText(this, "No PDF to display", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadPdfFromUrl(url: String) {
        Log.d(TAG, "Loading PDF from URL: $url")
        try {
            // For URL-based PDFs, open directly with external viewer
            openWithExternalViewer(url)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading PDF from URL", e)
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadPdfFromFirestore(documentId: String) {
        Log.d(TAG, "Loading PDF from Firestore document: $documentId")

        db.collection("cvs")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val base64String = document.getString("cvBase64")
                    val content = document.getString("content")

                    when {
                        !base64String.isNullOrEmpty() -> {
                            Log.d(TAG, "Found base64 PDF data")
                            loadPdfFromBase64(base64String)
                        }
                        !content.isNullOrEmpty() -> {
                            Log.d(TAG, "Found text content, showing as text")
                            showTextContent(content)
                        }
                        else -> {
                            Log.e(TAG, "Document exists but has no PDF data or content")
                            Toast.makeText(this, "No PDF content available", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    Log.e(TAG, "Document does not exist: $documentId")
                    Toast.makeText(this, "PDF not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading PDF from Firestore", e)
                Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadPdfFromBase64(base64String: String) {
        try {
            Log.d(TAG, "Decoding base64 PDF data")
            val pdfBytes = Base64.decode(base64String, Base64.DEFAULT)

            // Create temporary file in external cache directory for better access
            val tempFile = File(externalCacheDir ?: cacheDir, "temp_cv_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(tempFile)
            fos.write(pdfBytes)
            fos.close()

            Log.d(TAG, "Created temporary PDF file: ${tempFile.absolutePath}")

            // Open the temporary file with external PDF viewer
            openTempFileWithExternalViewer(tempFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading PDF from base64", e)
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showTextContent(content: String) {
        // If it's text content, redirect to ViewCvActivity
        val intent = Intent(this, ViewCvActivity::class.java).apply {
            putExtra("cvContent", content)
        }
        startActivity(intent)
        finish()
    }

    private fun openWithExternalViewer(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create chooser to show all available apps
            val chooserIntent = Intent.createChooser(intent, "Open PDF with...")

            if (chooserIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
                finish()
            } else {
                // Fallback: try to open with any app that can handle the file
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "*/*")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val fallbackChooser = Intent.createChooser(fallbackIntent, "Open file with...")

                if (fallbackChooser.resolveActivity(packageManager) != null) {
                    startActivity(fallbackChooser)
                    finish()
                } else {
                    Toast.makeText(this, "No app found to open PDF. Please install a PDF viewer from the Play Store.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening with external viewer", e)
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openTempFileWithExternalViewer(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create chooser to show all available apps
            val chooserIntent = Intent.createChooser(intent, "Open PDF with...")

            if (chooserIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
                finish()
            } else {
                // Fallback: try to open with any app that can handle the file
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val fallbackChooser = Intent.createChooser(fallbackIntent, "Open file with...")

                if (fallbackChooser.resolveActivity(packageManager) != null) {
                    startActivity(fallbackChooser)
                    finish()
                } else {
                    Toast.makeText(this, "No app found to open PDF. Please install a PDF viewer from the Play Store.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening temporary file with external viewer", e)
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
