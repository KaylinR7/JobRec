package com.example.jobrec.utils
import android.view.View
import android.widget.TextView
import com.example.jobrec.R
import com.google.android.material.button.MaterialButton
class AdminPagination(
    private val paginationView: View,
    private val pageSize: Int = 5,
    private val onPageChange: (Int) -> Unit
) {
    private var currentPage = 1
    private var totalItems = 0
    private var totalPages = 1
    private val paginationInfoText: TextView = paginationView.findViewById(R.id.paginationInfoText)
    private val prevPageButton: MaterialButton = paginationView.findViewById(R.id.prevPageButton)
    private val nextPageButton: MaterialButton = paginationView.findViewById(R.id.nextPageButton)
    init {
        setupButtons()
        updatePaginationInfo()
    }
    private fun setupButtons() {
        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePaginationInfo()
                onPageChange(currentPage)
            }
        }
        nextPageButton.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePaginationInfo()
                onPageChange(currentPage)
            }
        }
    }
    fun updateItemCount(totalItems: Int) {
        this.totalItems = totalItems
        this.totalPages = if (totalItems == 0) 1 else (totalItems + pageSize - 1) / pageSize
        if (currentPage > totalPages) {
            currentPage = 1
        }
        updatePaginationInfo()
    }
    fun getCurrentPage(): Int = currentPage
    fun getPageSize(): Int = pageSize
    fun getStartIndex(): Int = (currentPage - 1) * pageSize
    fun getEndIndex(): Int = minOf(currentPage * pageSize, totalItems)
    private fun updatePaginationInfo() {
        paginationInfoText.text = "Page $currentPage of $totalPages ($totalItems items)"
        prevPageButton.isEnabled = currentPage > 1
        nextPageButton.isEnabled = currentPage < totalPages
    }
    fun resetToFirstPage() {
        currentPage = 1
        updatePaginationInfo()
    }
    fun <T> getPageItems(allItems: List<T>): List<T> {
        val startIndex = getStartIndex()
        val endIndex = minOf(getEndIndex(), allItems.size)
        return if (startIndex < endIndex) {
            allItems.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
}
