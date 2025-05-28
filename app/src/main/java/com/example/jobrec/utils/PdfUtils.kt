package com.example.jobrec.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.jobrec.PdfViewerActivity

/**
 * Utility class for handling PDF viewing across the app
 */
object PdfUtils {
    
    private const val TAG = "PdfUtils"
    
    /**
     * Opens a PDF using the appropriate viewer based on the input type
     * 
     * @param context The context to start the activity from
     * @param pdfUrl URL of the PDF (for Firebase Storage URLs)
     * @param pdfId Document ID in Firestore (for base64 encoded PDFs)
     * @param title Title to display in the PDF viewer
     */
    fun openPdf(context: Context, pdfUrl: String? = null, pdfId: String? = null, title: String = "Resume") {
        Log.d(TAG, "Opening PDF - URL: $pdfUrl, ID: $pdfId, Title: $title")
        
        if (pdfUrl.isNullOrEmpty() && pdfId.isNullOrEmpty()) {
            Log.e(TAG, "Both PDF URL and ID are null or empty")
            return
        }
        
        val intent = Intent(context, PdfViewerActivity::class.java).apply {
            if (!pdfUrl.isNullOrEmpty()) {
                putExtra(PdfViewerActivity.EXTRA_PDF_URL, pdfUrl)
            }
            if (!pdfId.isNullOrEmpty()) {
                putExtra(PdfViewerActivity.EXTRA_PDF_ID, pdfId)
            }
            putExtra(PdfViewerActivity.EXTRA_TITLE, title)
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Opens a PDF from a URL (Firebase Storage or external URL)
     */
    fun openPdfFromUrl(context: Context, url: String, title: String = "Resume") {
        openPdf(context, pdfUrl = url, title = title)
    }
    
    /**
     * Opens a PDF from Firestore document ID (base64 encoded PDF)
     */
    fun openPdfFromFirestore(context: Context, documentId: String, title: String = "Resume") {
        openPdf(context, pdfId = documentId, title = title)
    }
    
    /**
     * Determines the appropriate method to open a CV/Resume based on the input
     * 
     * @param context The context to start the activity from
     * @param cvReference Either a URL (starts with "http") or a Firestore document ID
     * @param title Title to display in the viewer
     */
    fun openCvOrResume(context: Context, cvReference: String, title: String = "Resume") {
        Log.d(TAG, "Opening CV/Resume with reference: $cvReference")
        
        when {
            cvReference.startsWith("http") -> {
                Log.d(TAG, "Opening as URL")
                openPdfFromUrl(context, cvReference, title)
            }
            cvReference.isNotEmpty() -> {
                Log.d(TAG, "Opening as Firestore document ID")
                openPdfFromFirestore(context, cvReference, title)
            }
            else -> {
                Log.e(TAG, "Invalid CV reference: empty or null")
            }
        }
    }
}
