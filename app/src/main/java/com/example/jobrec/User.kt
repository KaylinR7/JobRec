package com.example.jobrec

import android.os.Parcel
import android.os.Parcelable

data class User(
    val id: String = "",
    val idNumber: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val province: String = "",
    val address: String = "",
    val summary: String = "",
    val skills: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val languages: List<Language> = emptyList(),
    val references: List<Reference> = emptyList(),
    val profileImageUrl: String? = null,
    val role: String = "user",
    val achievements: String = "",
    val linkedin: String = "",
    val github: String = "",
    val portfolio: String = "",
    val yearsOfExperience: String = "",
    val certificate: String = "",
    val expectedSalary: String = "",
    val field: String = "",
    val subField: String = ""
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
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createTypedArrayList(Education.CREATOR) ?: emptyList(),
        parcel.createTypedArrayList(Experience.CREATOR) ?: emptyList(),
        parcel.createTypedArrayList(Language.CREATOR) ?: emptyList(),
        parcel.createTypedArrayList(Reference.CREATOR) ?: emptyList(),
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(idNumber)
        parcel.writeString(name)
        parcel.writeString(surname)
        parcel.writeString(email)
        parcel.writeString(phoneNumber)
        parcel.writeString(province)
        parcel.writeString(address)
        parcel.writeString(summary)
        parcel.writeStringList(skills)
        parcel.writeStringList(hobbies)
        parcel.writeTypedList(education)
        parcel.writeTypedList(experience)
        parcel.writeTypedList(languages)
        parcel.writeTypedList(references)
        parcel.writeString(profileImageUrl)
        parcel.writeString(role)
        parcel.writeString(achievements)
        parcel.writeString(linkedin)
        parcel.writeString(github)
        parcel.writeString(portfolio)
        parcel.writeString(yearsOfExperience)
        parcel.writeString(certificate)
        parcel.writeString(expectedSalary)
        parcel.writeString(field)
        parcel.writeString(subField)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(institution)
        parcel.writeString(degree)
        parcel.writeString(fieldOfStudy)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Education> {
        override fun createFromParcel(parcel: Parcel): Education {
            return Education(parcel)
        }

        override fun newArray(size: Int): Array<Education?> {
            return arrayOfNulls(size)
        }
    }
}

data class Experience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(company)
        parcel.writeString(position)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Experience> {
        override fun createFromParcel(parcel: Parcel): Experience {
            return Experience(parcel)
        }

        override fun newArray(size: Int): Array<Experience?> {
            return arrayOfNulls(size)
        }
    }
}

data class Language(
    val name: String = "",
    val proficiency: String = "" // Basic, Intermediate, Advanced, Native
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(proficiency)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Language> {
        override fun createFromParcel(parcel: Parcel): Language {
            return Language(parcel)
        }

        override fun newArray(size: Int): Array<Language?> {
            return arrayOfNulls(size)
        }
    }
}

data class Reference(
    val name: String = "",
    val position: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(position)
        parcel.writeString(company)
        parcel.writeString(email)
        parcel.writeString(phone)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Reference> {
        override fun createFromParcel(parcel: Parcel): Reference {
            return Reference(parcel)
        }

        override fun newArray(size: Int): Array<Reference?> {
            return arrayOfNulls(size)
        }
    }
}