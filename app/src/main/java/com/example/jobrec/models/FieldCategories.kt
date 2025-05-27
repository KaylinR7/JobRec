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

    fun getCertificationsForField(field: String): List<String> {
        return certifications[field] ?: emptyList()
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

    val skills = mapOf(
        "Information Technology" to listOf(
            "Java", "Python", "JavaScript", "C++", "C#", "PHP", "Ruby", "Swift", "Kotlin",
            "React", "Angular", "Vue.js", "Node.js", "Express.js", "Django", "Flask",
            "HTML", "CSS", "Bootstrap", "Tailwind CSS", "SASS", "LESS",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle", "SQL Server",
            "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes", "Jenkins",
            "Git", "GitHub", "GitLab", "Bitbucket", "SVN",
            "Linux", "Windows Server", "MacOS", "Ubuntu", "CentOS",
            "Agile", "Scrum", "DevOps", "CI/CD", "TDD", "BDD",
            "Machine Learning", "Data Analysis", "Big Data", "AI", "Deep Learning",
            "Cybersecurity", "Penetration Testing", "Network Security", "Firewall Management",
            "UI/UX Design", "Figma", "Adobe XD", "Sketch", "Photoshop", "Illustrator"
        ),
        "Healthcare" to listOf(
            "Patient Care", "Medical Diagnosis", "Treatment Planning", "Emergency Response",
            "Medical Records Management", "HIPAA Compliance", "Medical Terminology",
            "Pharmacology", "Anatomy", "Physiology", "Pathology", "Radiology",
            "Surgery", "Anesthesia", "Intensive Care", "Pediatric Care",
            "Geriatric Care", "Mental Health", "Counseling", "Therapy",
            "Medical Equipment Operation", "Laboratory Testing", "Blood Draw",
            "IV Administration", "Wound Care", "Infection Control",
            "CPR", "First Aid", "BLS", "ACLS", "PALS",
            "Medical Coding", "Medical Billing", "Insurance Claims",
            "Electronic Health Records", "Epic", "Cerner", "Meditech"
        ),
        "Law" to listOf(
            "Legal Research", "Case Analysis", "Contract Drafting", "Legal Writing",
            "Litigation", "Negotiation", "Mediation", "Arbitration",
            "Corporate Law", "Criminal Law", "Family Law", "Labor Law",
            "Intellectual Property", "Tax Law", "Environmental Law", "Immigration Law",
            "Legal Compliance", "Regulatory Affairs", "Due Diligence",
            "Court Procedures", "Deposition", "Trial Advocacy", "Appellate Practice",
            "Legal Technology", "E-Discovery", "Document Review",
            "Client Relations", "Legal Ethics", "Professional Responsibility",
            "Paralegal Skills", "Legal Administration", "Case Management"
        ),
        "Engineering" to listOf(
            "AutoCAD", "SolidWorks", "MATLAB", "LabVIEW", "Simulink",
            "Project Management", "Technical Drawing", "3D Modeling", "CAD Design",
            "Structural Analysis", "Finite Element Analysis", "Circuit Design",
            "PLC Programming", "SCADA", "Control Systems", "Automation",
            "Quality Control", "Six Sigma", "Lean Manufacturing", "ISO Standards",
            "Materials Science", "Thermodynamics", "Fluid Mechanics", "Mechanics",
            "Electrical Systems", "Power Systems", "Electronics", "Microcontrollers",
            "Civil Engineering", "Structural Engineering", "Geotechnical Engineering",
            "Environmental Engineering", "Chemical Processes", "Process Design",
            "Safety Engineering", "Risk Assessment", "Compliance", "Regulations"
        ),
        "Finance" to listOf(
            "Financial Analysis", "Financial Modeling", "Budgeting", "Forecasting",
            "Investment Analysis", "Portfolio Management", "Risk Management",
            "Financial Reporting", "GAAP", "IFRS", "Tax Preparation", "Auditing",
            "Excel", "QuickBooks", "SAP", "Oracle Financials", "Sage",
            "Bloomberg Terminal", "Reuters", "Capital IQ", "FactSet",
            "Accounting", "Bookkeeping", "Accounts Payable", "Accounts Receivable",
            "Cash Flow Management", "Credit Analysis", "Loan Processing",
            "Insurance", "Underwriting", "Claims Processing", "Actuarial Analysis",
            "Compliance", "Regulatory Reporting", "SOX Compliance", "AML",
            "Banking", "Corporate Finance", "Investment Banking", "Private Equity"
        ),
        "Education" to listOf(
            "Curriculum Development", "Lesson Planning", "Classroom Management",
            "Student Assessment", "Educational Technology", "Online Learning",
            "Learning Management Systems", "Moodle", "Blackboard", "Canvas",
            "Special Education", "IEP Development", "Differentiated Instruction",
            "Behavior Management", "Student Counseling", "Parent Communication",
            "Educational Research", "Data Analysis", "Student Progress Tracking",
            "STEM Education", "Language Arts", "Mathematics", "Science", "Social Studies",
            "Early Childhood Education", "Adult Education", "ESL Teaching",
            "Educational Administration", "School Leadership", "Policy Development",
            "Teacher Training", "Professional Development", "Mentoring"
        ),
        "Business" to listOf(
            "Strategic Planning", "Business Analysis", "Project Management", "Operations Management",
            "Process Improvement", "Change Management", "Leadership", "Team Management",
            "Microsoft Office", "Excel", "PowerPoint", "Word", "Outlook",
            "CRM Software", "Salesforce", "HubSpot", "Zoho", "Pipedrive",
            "ERP Systems", "SAP", "Oracle", "NetSuite", "QuickBooks",
            "Data Analysis", "Business Intelligence", "Reporting", "KPI Tracking",
            "Supply Chain Management", "Inventory Management", "Vendor Management",
            "Human Resources", "Recruitment", "Performance Management", "Training",
            "Consulting", "Client Relations", "Presentation Skills", "Communication",
            "Entrepreneurship", "Business Development", "Market Analysis", "Competitive Analysis"
        ),
        "Marketing" to listOf(
            "Digital Marketing", "Social Media Marketing", "Content Marketing", "Email Marketing",
            "SEO", "SEM", "Google Ads", "Facebook Ads", "LinkedIn Ads", "Instagram Marketing",
            "Google Analytics", "Google Tag Manager", "Facebook Analytics", "Adobe Analytics",
            "Content Creation", "Copywriting", "Graphic Design", "Video Production",
            "Brand Management", "Brand Strategy", "Brand Development", "Brand Positioning",
            "Market Research", "Consumer Behavior", "Survey Design", "Focus Groups",
            "Public Relations", "Media Relations", "Press Releases", "Crisis Management",
            "Marketing Automation", "HubSpot", "Mailchimp", "Marketo", "Pardot",
            "CRM", "Lead Generation", "Lead Nurturing", "Conversion Optimization",
            "Adobe Creative Suite", "Photoshop", "Illustrator", "InDesign", "Canva"
        ),
        "Sales" to listOf(
            "Sales Prospecting", "Lead Generation", "Cold Calling", "Email Outreach",
            "Relationship Building", "Client Relations", "Account Management", "Customer Retention",
            "Sales Presentations", "Product Demonstrations", "Proposal Writing", "Contract Negotiation",
            "CRM Software", "Salesforce", "HubSpot", "Pipedrive", "Zoho CRM",
            "Sales Forecasting", "Pipeline Management", "Territory Management", "Sales Reporting",
            "B2B Sales", "B2C Sales", "Inside Sales", "Field Sales", "Retail Sales",
            "Technical Sales", "Consultative Selling", "Solution Selling", "Value-Based Selling",
            "Sales Training", "Team Leadership", "Performance Management", "Coaching",
            "Market Analysis", "Competitive Analysis", "Product Knowledge", "Industry Knowledge"
        ),
        "Customer Service" to listOf(
            "Customer Support", "Technical Support", "Help Desk", "Call Center Operations",
            "Live Chat Support", "Email Support", "Phone Support", "Ticket Management",
            "Customer Relationship Management", "CRM Software", "Zendesk", "Freshdesk", "ServiceNow",
            "Problem Solving", "Troubleshooting", "Conflict Resolution", "De-escalation",
            "Product Knowledge", "Technical Knowledge", "Software Support", "Hardware Support",
            "Customer Success", "Account Management", "Customer Retention", "Upselling",
            "Communication Skills", "Active Listening", "Empathy", "Patience",
            "Data Entry", "Documentation", "Reporting", "Quality Assurance",
            "Multilingual Support", "Cross-cultural Communication", "Time Management"
        ),
        "Manufacturing" to listOf(
            "Production Planning", "Manufacturing Processes", "Quality Control", "Quality Assurance",
            "Lean Manufacturing", "Six Sigma", "Kaizen", "5S Methodology", "Continuous Improvement",
            "Assembly Line Operations", "Machine Operation", "Equipment Maintenance", "Troubleshooting",
            "Safety Protocols", "OSHA Compliance", "Workplace Safety", "Risk Assessment",
            "Inventory Management", "Supply Chain", "Materials Management", "Procurement",
            "CAD Software", "AutoCAD", "SolidWorks", "Technical Drawing", "Blueprint Reading",
            "CNC Programming", "Machining", "Welding", "Fabrication", "Tool and Die",
            "Process Optimization", "Production Scheduling", "Capacity Planning", "Workflow Management",
            "ISO Standards", "GMP", "FDA Regulations", "Documentation", "Record Keeping"
        ),
        "Construction" to listOf(
            "Project Management", "Construction Management", "Site Supervision", "Safety Management",
            "Blueprint Reading", "Technical Drawing", "CAD Software", "AutoCAD", "Revit",
            "Carpentry", "Framing", "Roofing", "Electrical Work", "Plumbing", "HVAC",
            "Masonry", "Concrete Work", "Drywall", "Painting", "Flooring", "Tiling",
            "Heavy Equipment Operation", "Crane Operation", "Excavator", "Bulldozer", "Forklift",
            "Building Codes", "Permits", "Inspections", "Compliance", "Regulations",
            "Cost Estimation", "Budgeting", "Scheduling", "Resource Planning", "Procurement",
            "Quality Control", "Material Testing", "Structural Analysis", "Surveying",
            "OSHA Safety", "Fall Protection", "Hazmat Handling", "First Aid", "CPR"
        ),
        "Transportation" to listOf(
            "Logistics Management", "Supply Chain Management", "Fleet Management", "Route Planning",
            "Commercial Driving", "CDL License", "DOT Regulations", "Safety Compliance",
            "Warehouse Operations", "Inventory Management", "Shipping", "Receiving", "Packaging",
            "Freight Forwarding", "Import/Export", "Customs Documentation", "International Trade",
            "GPS Navigation", "Transportation Management Systems", "WMS", "ERP Systems",
            "Vehicle Maintenance", "Preventive Maintenance", "Troubleshooting", "Repair",
            "Customer Service", "Dispatch", "Communication", "Problem Solving",
            "Hazmat Handling", "Dangerous Goods", "Safety Protocols", "Emergency Response",
            "Air Transport", "Maritime Transport", "Rail Transport", "Intermodal Transport"
        ),
        "Hospitality" to listOf(
            "Customer Service", "Guest Relations", "Front Desk Operations", "Reservations",
            "Hotel Management", "Property Management Systems", "PMS Software", "Revenue Management",
            "Food Service", "Restaurant Operations", "Kitchen Management", "Food Safety",
            "Event Planning", "Event Coordination", "Wedding Planning", "Conference Management",
            "Housekeeping", "Cleaning Protocols", "Laundry Operations", "Maintenance",
            "Tourism", "Tour Guiding", "Travel Planning", "Destination Knowledge",
            "Catering", "Menu Planning", "Food Preparation", "Beverage Service",
            "Concierge Services", "Local Knowledge", "Recommendation Services", "Problem Solving",
            "Point of Sale Systems", "Cash Handling", "Payment Processing", "Inventory Management",
            "Multilingual Communication", "Cultural Awareness", "Hospitality Standards"
        ),
        "Other" to listOf(
            "Communication", "Problem Solving", "Critical Thinking", "Analytical Skills",
            "Time Management", "Organization", "Attention to Detail", "Multitasking",
            "Leadership", "Team Collaboration", "Adaptability", "Flexibility",
            "Customer Service", "Client Relations", "Interpersonal Skills", "Conflict Resolution",
            "Microsoft Office", "Excel", "Word", "PowerPoint", "Email", "Internet Research",
            "Data Entry", "Documentation", "Record Keeping", "Filing", "Administrative Support",
            "Project Coordination", "Scheduling", "Planning", "Resource Management",
            "Research", "Analysis", "Reporting", "Presentation Skills", "Writing Skills",
            "Government Regulations", "Compliance", "Policy Development", "Public Service",
            "Non-Profit Management", "Fundraising", "Grant Writing", "Volunteer Management"
        )
    )
}
