package com.example.jobrec
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
data class Company(
    var id: String = "",
    val companyName: String = "",
    val registrationNumber: String = "",
    val industry: String = "",
    val companySize: String = "",
    val location: String = "",
    val website: String = "",
    val description: String = "",
    val contactPersonName: String = "",
    val contactPersonEmail: String = "",
    val contactPersonPhone: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val status: String = "active",
    val createdDate: Timestamp = Timestamp.now(),
    val userId: String = "",
    val emailVerified: Boolean = false,
    val verificationCode: String = "",
    val passwordResetCode: String = "",
    val passwordResetExpiry: Long = 0
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
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Timestamp(parcel.readLong(), parcel.readInt()),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(companyName)
        parcel.writeString(registrationNumber)
        parcel.writeString(industry)
        parcel.writeString(companySize)
        parcel.writeString(location)
        parcel.writeString(website)
        parcel.writeString(description)
        parcel.writeString(contactPersonName)
        parcel.writeString(contactPersonEmail)
        parcel.writeString(contactPersonPhone)
        parcel.writeString(email)
        parcel.writeString(profileImageUrl)
        parcel.writeString(status)
        parcel.writeLong(createdDate.seconds)
        parcel.writeInt(createdDate.nanoseconds)
        parcel.writeString(userId)
        parcel.writeByte(if (emailVerified) 1 else 0)
        parcel.writeString(verificationCode)
        parcel.writeString(passwordResetCode)
        parcel.writeLong(passwordResetExpiry)
    }
    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<Company> {
        override fun createFromParcel(parcel: Parcel): Company {
            return Company(parcel)
        }
        override fun newArray(size: Int): Array<Company?> {
            return arrayOfNulls(size)
        }
    }
}