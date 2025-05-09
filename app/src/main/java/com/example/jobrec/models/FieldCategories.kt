package com.example.jobrec.models

object FieldCategories {
    val fields = mapOf(
        "Information Technology" to listOf(
            "Software Development",
            "Web Development",
            "Mobile Development",
            "Data Science",
            "Cybersecurity",
            "Cloud Computing",
            "DevOps",
            "UI/UX Design",
            "System Administration",
            "Database Administration"
        ),
        "Medical" to listOf(
            "General Practice",
            "Nursing",
            "Pharmacy",
            "Physiotherapy",
            "Dentistry",
            "Radiology",
            "Surgery",
            "Pediatrics",
            "Psychology",
            "Emergency Medicine"
        ),
        "Law" to listOf(
            "Corporate Law",
            "Criminal Law",
            "Family Law",
            "Labor Law",
            "Constitutional Law",
            "Intellectual Property",
            "Tax Law",
            "Environmental Law",
            "Human Rights Law",
            "International Law"
        ),
        "Engineering" to listOf(
            "Civil Engineering",
            "Mechanical Engineering",
            "Electrical Engineering",
            "Chemical Engineering",
            "Industrial Engineering",
            "Mining Engineering",
            "Aerospace Engineering",
            "Environmental Engineering",
            "Biomedical Engineering",
            "Software Engineering"
        ),
        "Finance" to listOf(
            "Accounting",
            "Investment Banking",
            "Financial Planning",
            "Risk Management",
            "Tax Advisory",
            "Auditing",
            "Corporate Finance",
            "Insurance",
            "Asset Management",
            "Financial Analysis"
        ),
        "Education" to listOf(
            "Primary Education",
            "Secondary Education",
            "Special Education",
            "Early Childhood Education",
            "Higher Education",
            "Educational Administration",
            "Curriculum Development",
            "Educational Technology",
            "Adult Education",
            "STEM Education"
        )
    )

    val certifications = mapOf(
        "Information Technology" to listOf(
            "AWS Certified Solutions Architect",
            "CompTIA A+",
            "Cisco CCNA",
            "Microsoft Azure Certifications",
            "Google Cloud Certifications",
            "Oracle Certifications",
            "Project Management Professional (PMP)",
            "ITIL Certification",
            "Certified Information Systems Security Professional (CISSP)",
            "Certified Ethical Hacker (CEH)"
        ),
        "Medical" to listOf(
            "Basic Life Support (BLS)",
            "Advanced Cardiac Life Support (ACLS)",
            "First Aid Certification",
            "Registered Nurse (RN)",
            "Medical Laboratory Technician",
            "Pharmacy Technician Certification",
            "Emergency Medical Technician (EMT)",
            "Certified Nursing Assistant (CNA)",
            "Medical Coding Certification",
            "Phlebotomy Certification"
        ),
        "Law" to listOf(
            "Bar Admission",
            "Legal Practice Management Certification",
            "Certified Legal Manager (CLM)",
            "Certified Paralegal",
            "Legal Project Management Certification",
            "E-Discovery Certification",
            "Legal Technology Certification",
            "Contract Management Certification",
            "Legal Ethics Certification",
            "International Law Certification"
        )
    )

    val experienceRanges = listOf(
        "No Experience",
        "Less than 1 year",
        "1-2 years",
        "2-3 years",
        "3-5 years",
        "5-7 years",
        "7-10 years",
        "10+ years"
    )

    fun getGraduationYears(): List<String> {
        val currentYear = java.time.Year.now().value
        return (currentYear downTo currentYear - 50).map { it.toString() }
    }

    val languages = listOf(
        "English",
        "Afrikaans",
        "isiZulu",
        "isiXhosa",
        "Sesotho",
        "Setswana",
        "Sepedi",
        "Tshivenda",
        "Xitsonga",
        "siSwati",
        "isiNdebele"
    )
}
