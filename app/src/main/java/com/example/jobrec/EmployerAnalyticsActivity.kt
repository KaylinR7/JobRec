package com.example.jobrec
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
class EmployerAnalyticsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewsChart: LineChart
    private lateinit var applicationsChart: LineChart
    private lateinit var demographicsChart: PieChart
    private lateinit var totalViewsText: TextView
    private lateinit var totalApplicationsText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_analytics)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Analytics"
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        totalViewsText = findViewById(R.id.totalViewsText)
        totalApplicationsText = findViewById(R.id.totalApplicationsText)
        viewsChart = findViewById(R.id.viewsChart)
        applicationsChart = findViewById(R.id.applicationsChart)
        demographicsChart = findViewById(R.id.demographicsChart)
        loadAnalyticsData()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun loadAnalyticsData() {
        val companyId = auth.currentUser?.uid ?: return
        db.collection("companies").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val totalViews = document.getLong("totalViews") ?: 0L
                val totalApplications = document.getLong("totalApplications") ?: 0L
                totalViewsText.text = totalViews.toString()
                totalApplicationsText.text = totalApplications.toString()
            }
        db.collection("companies").document(companyId)
            .collection("views")
            .get()
            .addOnSuccessListener { documents ->
                val entries = mutableListOf<Entry>()
                val xAxisLabels = mutableListOf<String>()
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                var index = 0f
                documents.documents.sortedBy { it.id }.forEach { document ->
                    val timestamp = document.getTimestamp("timestamp")?.toDate()
                    val count = document.getLong("count") ?: 0L
                    if (timestamp != null) {
                        entries.add(Entry(index, count.toFloat()))
                        xAxisLabels.add(dateFormat.format(timestamp))
                        index++
                    }
                }
                setupViewsChart(entries, xAxisLabels)
            }
        db.collection("companies").document(companyId)
            .collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                val entries = mutableListOf<Entry>()
                val xAxisLabels = mutableListOf<String>()
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                var index = 0f
                documents.documents.sortedBy { it.id }.forEach { document ->
                    val timestamp = document.getTimestamp("timestamp")?.toDate()
                    val count = document.getLong("count") ?: 0L
                    if (timestamp != null) {
                        entries.add(Entry(index, count.toFloat()))
                        xAxisLabels.add(dateFormat.format(timestamp))
                        index++
                    }
                }
                setupApplicationsChart(entries, xAxisLabels)
            }
        db.collection("companies").document(companyId)
            .collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                val ageGroups = mutableMapOf<String, Int>()
                val educationLevels = mutableMapOf<String, Int>()
                val experienceRanges = mutableMapOf<String, Int>()
                documents.documents.forEach { document ->
                    val age = document.getLong("age")?.toInt() ?: 0
                    val education = document.getString("education") ?: "Unknown"
                    val experience = document.getLong("experience")?.toInt() ?: 0
                    val ageGroup = when {
                        age < 25 -> "18-24"
                        age < 35 -> "25-34"
                        age < 45 -> "35-44"
                        else -> "45+"
                    }
                    ageGroups[ageGroup] = (ageGroups[ageGroup] ?: 0) + 1
                    educationLevels[education] = (educationLevels[education] ?: 0) + 1
                    val experienceRange = when {
                        experience < 2 -> "0-2 years"
                        experience < 5 -> "2-5 years"
                        experience < 10 -> "5-10 years"
                        else -> "10+ years"
                    }
                    experienceRanges[experienceRange] = (experienceRanges[experienceRange] ?: 0) + 1
                }
                setupDemographicsChart(ageGroups, educationLevels, experienceRanges)
            }
    }
    private fun setupViewsChart(entries: List<Entry>, xAxisLabels: List<String>) {
        val dataSet = LineDataSet(entries, "Views")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.valueTextColor = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(true)
        val lineData = LineData(dataSet)
        viewsChart.data = lineData
        viewsChart.description.isEnabled = false
        viewsChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        viewsChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        viewsChart.axisRight.isEnabled = false
        viewsChart.animateY(1000)
        viewsChart.invalidate()
    }
    private fun setupApplicationsChart(entries: List<Entry>, xAxisLabels: List<String>) {
        val dataSet = LineDataSet(entries, "Applications")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[1]
        dataSet.valueTextColor = ColorTemplate.MATERIAL_COLORS[1]
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(true)
        val lineData = LineData(dataSet)
        applicationsChart.data = lineData
        applicationsChart.description.isEnabled = false
        applicationsChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        applicationsChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        applicationsChart.axisRight.isEnabled = false
        applicationsChart.animateY(1000)
        applicationsChart.invalidate()
    }
    private fun setupDemographicsChart(
        ageGroups: Map<String, Int>,
        educationLevels: Map<String, Int>,
        experienceRanges: Map<String, Int>
    ) {
        val entries = mutableListOf<PieEntry>()
        ageGroups.forEach { (label, value) ->
            entries.add(PieEntry(value.toFloat(), "Age: $label"))
        }
        val dataSet = PieDataSet(entries, "Candidate Demographics")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = android.graphics.Color.WHITE
        val pieData = PieData(dataSet)
        demographicsChart.data = pieData
        demographicsChart.description.isEnabled = false
        demographicsChart.legend.isEnabled = true
        demographicsChart.setEntryLabelColor(android.graphics.Color.BLACK)
        demographicsChart.animateY(1000)
        demographicsChart.invalidate()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 