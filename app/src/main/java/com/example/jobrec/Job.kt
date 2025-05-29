package com.example.jobrec
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp
import java.util.*
data class Job(
    var id: String = "",
    val title: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val city: String = "",
    val salary: String = "",
    val type: String = "",
    val jobType: String = "",
    val description: String = "",
    val requirements: String = "",
    val postedDate: Timestamp = Timestamp.now(),
    val updatedDate: Timestamp = Timestamp.now(),
    val status: String = "active",
    val jobField: String = "",
    val specialization: String = "",
    val province: String = "",
    val experienceLevel: String = "",
    val requiredSkills: List<String> = emptyList(),
    var matchPercentage: Int = 0,
    var matchReasoning: String = ""
) : Parcelable {
    @Exclude
    fun getRequirementsList(): List<String> {
        return requirements.split("\n").filter { it.isNotEmpty() }
    }
    @Exclude
    fun getPostedDateMillis(): Long {
        return postedDate.toDate().time
    }
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Timestamp(parcel.readLong(), parcel.readInt()),
        Timestamp(parcel.readLong(), parcel.readInt()),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readInt(),
        parcel.readString() ?: ""
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(companyId)
        parcel.writeString(companyName)
        parcel.writeString(city)
        parcel.writeString(salary)
        parcel.writeString(type)
        parcel.writeString(jobType)
        parcel.writeString(description)
        parcel.writeString(requirements)
        parcel.writeLong(postedDate.seconds)
        parcel.writeInt(postedDate.nanoseconds)
        parcel.writeLong(updatedDate.seconds)
        parcel.writeInt(updatedDate.nanoseconds)
        parcel.writeString(status)
        parcel.writeString(jobField)
        parcel.writeString(specialization)
        parcel.writeString(province)
        parcel.writeString(experienceLevel)
        parcel.writeStringList(requiredSkills)
        parcel.writeInt(matchPercentage)
        parcel.writeString(matchReasoning)
    }
    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<Job> {
        override fun createFromParcel(parcel: Parcel): Job {
            return Job(parcel)
        }
        override fun newArray(size: Int): Array<Job?> {
            return arrayOfNulls(size)
        }
    }
}