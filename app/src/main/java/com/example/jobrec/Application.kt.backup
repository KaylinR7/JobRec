package com.example.jobrec

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class Application(
    var id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val userId: String = "",
    val applicantName: String = "",
    val applicantEmail: String = "",
    val applicantPhone: String = "",
    val appliedDate: Timestamp = Timestamp.now(),
    val status: String = "pending",
    val coverLetter: String = "",
    val cvUrl: String = "",
    val notes: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Timestamp(parcel.readLong(), parcel.readInt()),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(jobId)
        parcel.writeString(jobTitle)
        parcel.writeString(companyName)
        parcel.writeString(userId)
        parcel.writeString(applicantName)
        parcel.writeString(applicantEmail)
        parcel.writeString(applicantPhone)
        parcel.writeLong(appliedDate.seconds)
        parcel.writeInt(appliedDate.nanoseconds)
        parcel.writeString(status)
        parcel.writeString(coverLetter)
        parcel.writeString(cvUrl)
        parcel.writeString(notes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Application> {
        override fun createFromParcel(parcel: Parcel): Application {
            return Application(parcel)
        }

        override fun newArray(size: Int): Array<Application?> {
            return arrayOfNulls(size)
        }
    }
}