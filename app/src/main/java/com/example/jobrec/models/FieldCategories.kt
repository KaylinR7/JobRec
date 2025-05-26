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
        "Healthcare" to listOf(
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
        ),
        "Business" to listOf(
            "Business Administration",
            "Entrepreneurship",
            "Operations Management",
            "Project Management",
            "Human Resources",
            "Supply Chain Management",
            "Business Analysis",
            "Strategic Planning",
            "Consulting",
            "International Business"
        ),
        "Marketing" to listOf(
            "Digital Marketing",
            "Content Marketing",
            "Social Media Marketing",
            "SEO/SEM",
            "Brand Management",
            "Market Research",
            "Public Relations",
            "Advertising",
            "Email Marketing",
            "Product Marketing"
        ),
        "Sales" to listOf(
            "Account Management",
            "Business Development",
            "Inside Sales",
            "Field Sales",
            "Sales Management",
            "Retail Sales",
            "Technical Sales",
            "Pharmaceutical Sales",
            "Real Estate Sales",
            "Telemarketing"
        ),
        "Customer Service" to listOf(
            "Call Center Support",
            "Technical Support",
            "Customer Success",
            "Client Relations",
            "Help Desk",
            "Customer Experience",
            "Account Services",
            "Customer Retention",
            "Complaint Resolution",
            "Front Desk"
        ),
        "Manufacturing" to listOf(
            "Production Management",
            "Quality Control",
            "Assembly Line",
            "Machining",
            "Fabrication",
            "Process Improvement",
            "Inventory Management",
            "Maintenance",
            "Safety Management",
            "Lean Manufacturing"
        ),
        "Construction" to listOf(
            "Project Management",
            "Architecture",
            "Carpentry",
            "Electrical",
            "Plumbing",
            "HVAC",
            "Masonry",
            "Roofing",
            "Site Supervision",
            "Estimating"
        ),
        "Transportation" to listOf(
            "Logistics",
            "Fleet Management",
            "Truck Driving",
            "Shipping",
            "Air Transport",
            "Maritime Transport",
            "Public Transit",
            "Freight Forwarding",
            "Dispatch",
            "Warehouse Management"
        ),
        "Hospitality" to listOf(
            "Hotel Management",
            "Food Service",
            "Event Planning",
            "Tourism",
            "Catering",
            "Front Desk",
            "Housekeeping",
            "Restaurant Management",
            "Concierge Services",
            "Cruise Line Operations"
        ),
        "Other" to listOf(
            "Agriculture",
            "Arts & Entertainment",
            "Government",
            "Non-Profit",
            "Real Estate",
            "Research",
            "Sports & Recreation",
            "Telecommunications",
            "Utilities",
            "Miscellaneous"
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
        "Healthcare" to listOf(
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
        ),
        "Engineering" to listOf(
            "Professional Engineer (PE)",
            "Engineer in Training (EIT)",
            "Certified Engineering Technician",
            "Project Management Professional (PMP)",
            "Six Sigma Certification",
            "LEED Certification",
            "AutoCAD Certification",
            "Certified Manufacturing Engineer",
            "Certified Quality Engineer",
            "Certified Safety Professional"
        ),
        "Finance" to listOf(
            "Certified Public Accountant (CPA)",
            "Chartered Financial Analyst (CFA)",
            "Certified Financial Planner (CFP)",
            "Certified Management Accountant (CMA)",
            "Financial Risk Manager (FRM)",
            "Chartered Alternative Investment Analyst (CAIA)",
            "Certified Internal Auditor (CIA)",
            "Certified Fraud Examiner (CFE)",
            "Enrolled Agent (EA)",
            "Series 7 License"
        ),
        "Education" to listOf(
            "Teaching License/Certification",
            "Special Education Certification",
            "TESOL/TEFL Certification",
            "School Administrator License",
            "School Counselor Certification",
            "Reading Specialist Certification",
            "Gifted Education Certification",
            "Educational Technology Certification",
            "Early Childhood Education Certification",
            "National Board Certification"
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
