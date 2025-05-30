package com.example.jobrec.chatbot
import android.content.Context
import android.util.Log
import com.example.jobrec.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
class ChatbotRepository(private val context: Context) {
    private val huggingFaceService = HuggingFaceService()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChatbotRepository"
    private val huggingFaceToken: String
        get() = retrieveHuggingFaceToken()
    private val helpTopics = mapOf(
        "what is jobrec" to "JobRec is a job recruitment app that connects job seekers with employers. It allows students to search for jobs, submit applications, and communicate with potential employers. Companies can post job openings, review applications, and connect with qualified candidates.",
        "what is careerworx" to "Careerworx is a job recruitment app that connects job seekers with employers. It allows students to search for jobs, submit applications, and communicate with potential employers. Companies can post job openings, review applications, and connect with qualified candidates.",
        "about jobrec" to "CareerWorx is a comprehensive job recruitment platform designed to streamline the job search and hiring process. It offers features for both job seekers and employers, including job listings, application management, messaging, and profile customization.",
        "how does jobrec work" to "CareerWorx works by connecting job seekers with employers. As a student, you can create a profile, search for jobs, and submit applications. Employers can post job openings, review applications, and communicate with candidates. The app facilitates the entire recruitment process from job posting to hiring.",
        "app features" to "JobRec offers many features including job search with filters, application tracking, saved jobs, messaging with employers, profile management, CV/resume uploads, and notifications for job matches. Companies can post jobs, search for candidates, review applications, and communicate with applicants.",
        "job search" to "To search for jobs, go to the Search tab and use the filters to narrow down results. You can filter by job type, location, experience level, and more to find positions that match your qualifications and preferences.",
        "profile" to "To update your profile, go to the Profile tab and tap on the fields you want to edit. Your profile showcases your skills, experience, and education to potential employers, so keep it up-to-date!",
        "applications" to "You can view your job applications in the Applications tab. You'll see the status of each application (pending, reviewed, accepted, or rejected) and can track your progress throughout the hiring process.",
        "saved jobs" to "To save a job for later, tap the bookmark icon on any job listing. You can view all your saved jobs in the Saved Jobs section, making it easy to apply when you're ready.",
        "chat" to "You can chat with employers after your application has been accepted. Go to the Messages section to view your conversations. This feature allows direct communication to discuss job details or arrange interviews.",
        "login" to "If you're having trouble logging in, make sure you're using the correct email and password. You can reset your password on the login screen by tapping the 'Forgot Password' link.",
        "signup" to "To create a new account, go to the signup page and fill in your details including name, email, password, and education information. You'll need to verify your email before you can use all features.",
        "cv" to "To update your CV, go to the Profile tab and scroll down to the CV section. You can edit your CV details including education, experience, skills, and achievements at any time.",
        "resume" to "To update your resume, go to the Profile tab and scroll down to the CV/Resume section. You can edit your details and save the changes at any time to keep your information current for employers.",
        "change cv" to "To change your CV, navigate to the Profile section from the bottom navigation bar, then scroll to find the CV section where you can make and save your updates.",
        "update cv" to "To update your CV, go to your Profile, find the CV section, and tap the edit button. Make your changes and save them to ensure employers see your latest information.",
        "edit profile" to "To edit your profile information, go to the Profile tab and tap on any field you want to update. Make your changes and tap Save to keep your profile current.",
        "upload resume" to "To upload a new resume, go to your Profile, scroll to the CV/Resume section, and tap the upload button to select a file from your device. Supported formats include PDF and Word documents.",
        "filter jobs" to "To filter jobs, go to the Search tab and use the filter options at the top of the screen to narrow down results by location, job type, experience level, and more.",
        "job recommendations" to "JobRec provides personalized job recommendations based on your profile, skills, and previous applications. These appear on your home screen to help you discover relevant opportunities.",
        "job alerts" to "Job alerts notify you when new positions matching your interests are posted. You can manage your alert preferences in the Profile section.",
        "company profile" to "If you're a company user, you can edit your company profile by going to the Profile tab in the bottom navigation bar. A complete profile helps attract qualified candidates.",
        "post job" to "To post a new job, go to the Company Dashboard and tap the 'Post Job' button. Fill in all the required details about the position including title, description, requirements, and compensation.",
        "review applications" to "As a company, you can review applications by going to the Applications section. You can filter applications by status and view candidate details including their CV and cover letter.",
        "candidate search" to "Companies can search for candidates by going to the Search Candidates section. You can filter by skills, experience, education, and other criteria to find qualified applicants.",
        "company dashboard" to "The Company Dashboard provides an overview of your job postings, applications received, and recent activity. It's your central hub for managing recruitment activities.",
        "delete account" to "To delete your account, please contact our support team at support@jobrec.com. Note that this action is permanent and will remove all your data from our system.",
        "change password" to "To change your password, go to the Profile section and tap on the 'Change Password' option. You'll need to enter your current password and then create a new one.",
        "notifications" to "You can manage your notification settings in the Profile section under 'Notification Preferences'. You can choose which alerts you want to receive and how you receive them.",
        "privacy" to "JobRec takes your privacy seriously. Your personal information is only shared with employers when you apply for a job. You can review our full privacy policy in the app settings.",
        "data security" to "JobRec uses encryption and secure servers to protect your data. We never share your information with third parties without your consent.",
        "contact employer" to "You can contact employers through the chat feature after your application has been accepted. This allows for direct communication about job details, interviews, or any questions.",
        "messaging" to "The messaging feature allows direct communication between candidates and employers. You can access your conversations from the Messages section of the app.",
        "schedule interview" to "Employers can schedule interviews through the chat feature. You'll receive a notification when an interview is proposed, and you can accept or suggest an alternative time.",
        "app not working" to "If you're experiencing issues with the app, try these steps: 1) Restart the app, 2) Check your internet connection, 3) Update to the latest version, 4) Restart your device. If problems persist, contact support.",
        "bug report" to "To report a bug, please email support@jobrec.com with details about the issue, including what you were doing when it occurred and any error messages you received.",
        "slow app" to "If the app is running slowly, try clearing your cache, ensuring you have a stable internet connection, and closing other apps running in the background.",
        "contact support" to "For support, email support@careerworx.com or use the Contact section in the app. Our team is available Monday-Friday, 9am-5pm to assist with any issues or questions.",
        "feedback" to "We value your feedback! You can submit suggestions or comments through the Feedback option in the app settings or by emailing feedback@careerworx.com.",
        "help center" to "The Help Center contains articles and guides on using Careerworx. Access it from the menu to find answers to common questions and learn about app features.",
        "profile setup" to "To set up your profile effectively, make sure to include a professional photo, detailed education history, work experience with accomplishments, relevant skills, and certifications. A complete profile increases your chances of being noticed by employers.",
        "profile tips" to "Here are some profile tips: 1) Use a professional photo, 2) List all relevant skills and certifications, 3) Provide detailed work experience with measurable achievements, 4) Include education details with GPA if it's strong, 5) Keep your profile updated regularly.",
        "improve profile" to "To improve your profile, add specific achievements in your work experience, list relevant skills that match job descriptions, include certifications, ensure your education section is complete, and add a professional photo.",
        "proofread resume" to "When proofreading your resume, check for: 1) Spelling and grammar errors, 2) Consistent formatting and font usage, 3) Proper verb tense (past tense for previous jobs), 4) Quantifiable achievements rather than just duties, 5) Tailored content that matches the jobs you're applying for.",
        "resume mistakes" to "Common resume mistakes to avoid: 1) Typos and grammatical errors, 2) Using generic descriptions instead of specific achievements, 3) Including irrelevant experience, 4) Having an unprofessional email address, 5) Using an outdated or unprofessional format.",
        "cv format" to "A good CV format includes: 1) Contact information at the top, 2) A brief professional summary, 3) Work experience in reverse chronological order with achievements, 4) Education details, 5) Skills section, 6) Certifications, and 7) References (or 'Available upon request').",
        "application tips" to "When applying for jobs: 1) Tailor your resume to each position, 2) Write a customized cover letter addressing the company's needs, 3) Research the company before applying, 4) Follow all application instructions carefully, 5) Proofread everything before submitting.",
        "cover letter" to "A good cover letter should: 1) Address the hiring manager by name if possible, 2) Mention the specific job you're applying for, 3) Highlight relevant skills and experience, 4) Explain why you're interested in the company, 5) Include a call to action, and 6) Be concise (no more than one page).",
        "interview preparation" to "To prepare for an interview: 1) Research the company thoroughly, 2) Practice answers to common questions, 3) Prepare examples of your achievements, 4) Have questions ready to ask the interviewer, 5) Dress professionally, 6) Plan your route to arrive early, 7) Bring copies of your resume."
    )
    suspend fun getChatbotResponse(query: String): String {
        val localResponse = getLocalResponse(query)
        if (localResponse != null) {
            saveInteraction(query, localResponse)
            return localResponse
        }
        val fuzzyMatch = getFuzzyMatchResponse(query)
        if (fuzzyMatch != null) {
            saveInteraction(query, fuzzyMatch)
            return fuzzyMatch
        }
        return try {
            val response = huggingFaceService.generateResponse(query, huggingFaceToken)
            if (response.contains("Error:") || response.isBlank() || response.contains("404")) {
                val fallbackResponse = getFallbackResponse(query)
                saveInteraction(query, fallbackResponse)
                return fallbackResponse
            }
            saveInteraction(query, response)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chatbot response", e)
            val fallbackResponse = getFallbackResponse(query)
            saveInteraction(query, fallbackResponse)
            return fallbackResponse
        }
    }
    private fun getFallbackResponse(query: String): String {
        val lowerQuery = query.lowercase()
        return when {
            lowerQuery.contains("what is") || lowerQuery.contains("about") || lowerQuery.contains("tell me about") -> {
                if (lowerQuery.contains("jobrec") || lowerQuery.contains("app") || lowerQuery.contains("this app")) {
                    "JobRec is a job recruitment app that connects students with employers. It allows you to search for jobs, submit applications, save favorite positions, and communicate with potential employers. Companies can post job openings, review applications, and connect with qualified candidates."
                } else {
                    "I'm here to help you navigate the JobRec app. You can ask me about how the app works, job searching, applications, profile management, and more. What would you like to know?"
                }
            }
            lowerQuery.contains("how") && (lowerQuery.contains("work") || lowerQuery.contains("use")) &&
                    (lowerQuery.contains("jobrec") || lowerQuery.contains("app")) ->
                "JobRec works by connecting job seekers with employers. As a student, you can create a profile, search for jobs using filters, and submit applications. Employers can post job openings, review applications, and communicate with candidates. The app handles the entire recruitment process from job posting to hiring."
            lowerQuery.contains("feature") || lowerQuery.contains("what can") || lowerQuery.contains("functionality") ->
                "JobRec offers many features including: job search with filters, application tracking, saved jobs, messaging with employers, profile management, CV/resume uploads, and notifications for job matches. Companies can post jobs, search for candidates, review applications, and communicate with applicants."
            lowerQuery.contains("cv") || lowerQuery.contains("resume") ->
                "To update your CV or resume, go to the Profile tab and scroll down to the CV section. You can edit your details including education, experience, skills, and achievements. Keep your CV updated to improve your chances of getting hired!"
            lowerQuery.contains("profile") || lowerQuery.contains("edit") || lowerQuery.contains("change") ->
                "To edit your profile information, go to the Profile tab from the bottom navigation bar and tap on any field you want to update. Your profile showcases your skills and experience to employers, so keep it complete and up-to-date."
            lowerQuery.contains("job") || lowerQuery.contains("search") || lowerQuery.contains("find") ->
                "To search for jobs, use the Search tab at the bottom of the screen. You can filter results by job type, location, experience level, and more to find positions that match your qualifications and preferences."
            lowerQuery.contains("apply") || lowerQuery.contains("application") ->
                "To apply for a job, open the job details and tap the 'Apply' button. You'll need to fill out the application form and may need to upload your resume. You can track the status of your applications in the Applications tab."
            lowerQuery.contains("company") || lowerQuery.contains("employer") -> {
                if (lowerQuery.contains("contact") || lowerQuery.contains("message") || lowerQuery.contains("chat")) {
                    "You can contact employers through the chat feature after your application has been accepted. This allows for direct communication about job details, interviews, or any questions you may have."
                } else if (lowerQuery.contains("view") || lowerQuery.contains("profile")) {
                    "You can view company profiles by tapping on the company name in any job listing. This shows you information about the company, their current job openings, and company details."
                } else {
                    "Companies use JobRec to post job openings, review applications, search for candidates, and communicate with applicants. If you're a company user, you can access these features from the Company Dashboard."
                }
            }
            lowerQuery.contains("account") || lowerQuery.contains("password") || lowerQuery.contains("login") -> {
                if (lowerQuery.contains("delete") || lowerQuery.contains("remove")) {
                    "To delete your account, please contact our support team at support@jobrec.com. Note that this action is permanent and will remove all your data from our system."
                } else if (lowerQuery.contains("password")) {
                    "To change your password, go to the Profile section and tap on the 'Change Password' option. You'll need to enter your current password and then create a new one."
                } else {
                    "You can manage your account settings in the Profile section. This includes updating your personal information, changing your password, and managing notification preferences."
                }
            }
            lowerQuery.contains("help") || lowerQuery.contains("support") || lowerQuery.contains("contact") ->
                "For support, email support@jobrec.com or use the Contact section in the app. Our team is available Monday-Friday, 9am-5pm to assist with any issues or questions you might have."
            else -> "I'm here to help you navigate the JobRec app. You can ask me about how the app works, job searching, applications, profile management, and more. How can I assist you today?"
        }
    }
    private fun getFuzzyMatchResponse(query: String): String? {
        val lowerQuery = query.lowercase()
        if (lowerQuery.contains("what") || lowerQuery.contains("how") || lowerQuery.contains("tell")) {
            if (lowerQuery.contains("jobrec") || lowerQuery.contains("app") || lowerQuery.contains("this app")) {
                for (topic in listOf("what is jobrec", "about jobrec", "how does jobrec work", "app features")) {
                    val response = helpTopics[topic]
                    if (response != null) {
                        return response
                    }
                }
            }
        }
        val featureKeywords = mapOf(
            "search" to listOf("job search", "filter jobs"),
            "profile" to listOf("profile", "edit profile"),
            "cv" to listOf("cv", "resume", "change cv", "update cv"),
            "apply" to listOf("applications", "apply"),
            "save" to listOf("saved jobs"),
            "message" to listOf("chat", "messaging", "contact employer"),
            "company" to listOf("company profile", "post job", "review applications"),
            "account" to listOf("login", "signup", "change password", "delete account")
        )
        for ((keyword, topics) in featureKeywords) {
            if (lowerQuery.contains(keyword)) {
                for (topic in topics) {
                    val response = helpTopics[topic]
                    if (response != null) {
                        return response
                    }
                }
            }
        }
        for ((topic, response) in helpTopics) {
            val topicWords = topic.split(" ")
            for (word in topicWords) {
                if (word.length > 3 && lowerQuery.contains(word)) {
                    return response
                }
            }
        }
        if (lowerQuery.startsWith("how do i") || lowerQuery.startsWith("how can i") ||
            lowerQuery.startsWith("how to") || lowerQuery.startsWith("what is") ||
            lowerQuery.startsWith("where can i") || lowerQuery.startsWith("can i")) {
            val questionPart = lowerQuery
                .replace("how do i", "")
                .replace("how can i", "")
                .replace("how to", "")
                .replace("what is", "")
                .replace("where can i", "")
                .replace("can i", "")
                .trim()
            val questionWords = questionPart.split(" ").filter { it.length > 3 }
            for (word in questionWords) {
                for ((topic, response) in helpTopics) {
                    if (topic.contains(word)) {
                        return response
                    }
                }
            }
        }
        return null
    }
    private fun getLocalResponse(query: String): String? {
        val lowerQuery = query.lowercase()
        if (lowerQuery.contains("hello") || lowerQuery.contains("hi ") || lowerQuery == "hi" ||
            lowerQuery == "hey" || lowerQuery.contains("greetings")) {
            return "Hello! How can I help you with JobRec today?"
        }
        if (lowerQuery.contains("thank") || lowerQuery.contains("thanks") || lowerQuery.contains("appreciate")) {
            return "You're welcome! I'm happy to help. Is there anything else you'd like to know about JobRec?"
        }
        if (lowerQuery.contains("bye") || lowerQuery.contains("goodbye") || lowerQuery.contains("see you") ||
            lowerQuery.contains("exit") || lowerQuery.contains("close")) {
            return "Goodbye! Feel free to come back if you have more questions. Have a great day!"
        }
        if ((lowerQuery.contains("what") || lowerQuery.contains("tell me about")) &&
            (lowerQuery.contains("jobrec") || lowerQuery.contains("this app") || lowerQuery.contains("the app"))) {
            return helpTopics["what is jobrec"] ?: helpTopics["about jobrec"]
        }
        if (lowerQuery.contains("how") && lowerQuery.contains("work") &&
            (lowerQuery.contains("jobrec") || lowerQuery.contains("app") || lowerQuery.contains("it"))) {
            return helpTopics["how does jobrec work"]
        }
        if ((lowerQuery.contains("what") && lowerQuery.contains("feature")) ||
            (lowerQuery.contains("what") && lowerQuery.contains("do")) ||
            lowerQuery.contains("functionality")) {
            return helpTopics["app features"]
        }
        for ((topic, response) in helpTopics) {
            if (lowerQuery == topic || lowerQuery == "how to $topic" || lowerQuery == "how do i $topic") {
                return response
            }
            if (lowerQuery.contains(topic)) {
                return response
            }
        }
        if (lowerQuery.contains("cv") || lowerQuery.contains("resume")) {
            if (lowerQuery.contains("change") || lowerQuery.contains("update") || lowerQuery.contains("edit")) {
                return helpTopics["change cv"] ?: helpTopics["update cv"]
            }
            if (lowerQuery.contains("upload") || lowerQuery.contains("add")) {
                return helpTopics["upload resume"]
            }
            if (lowerQuery.contains("proofread") || lowerQuery.contains("check") || lowerQuery.contains("review")) {
                return helpTopics["proofread resume"]
            }
            if (lowerQuery.contains("mistake") || lowerQuery.contains("error") || lowerQuery.contains("wrong")) {
                return helpTopics["resume mistakes"]
            }
            if (lowerQuery.contains("format") || lowerQuery.contains("structure") || lowerQuery.contains("layout")) {
                return helpTopics["cv format"]
            }
            return helpTopics["cv"] ?: helpTopics["resume"]
        }
        if (lowerQuery.contains("profile")) {
            if (lowerQuery.contains("edit") || lowerQuery.contains("update") || lowerQuery.contains("change")) {
                return helpTopics["edit profile"]
            }
            if (lowerQuery.contains("company")) {
                return helpTopics["company profile"]
            }
            if (lowerQuery.contains("setup") || lowerQuery.contains("create") || lowerQuery.contains("new")) {
                return helpTopics["profile setup"]
            }
            if (lowerQuery.contains("tip") || lowerQuery.contains("advice") || lowerQuery.contains("suggestion")) {
                return helpTopics["profile tips"]
            }
            if (lowerQuery.contains("improve") || lowerQuery.contains("enhance") || lowerQuery.contains("better")) {
                return helpTopics["improve profile"]
            }
            return helpTopics["profile"]
        }
        if (lowerQuery.contains("job") && (lowerQuery.contains("search") || lowerQuery.contains("find") || lowerQuery.contains("look"))) {
            if (lowerQuery.contains("filter") || lowerQuery.contains("narrow")) {
                return helpTopics["filter jobs"]
            }
            return helpTopics["job search"]
        }
        if (lowerQuery.contains("apply") || lowerQuery.contains("application")) {
            if (lowerQuery.contains("how")) {
                return "To apply for a job, open the job details and click the 'Apply' button. You'll need to fill out the application form and may need to upload your resume."
            }
            if (lowerQuery.contains("track") || lowerQuery.contains("status") || lowerQuery.contains("view")) {
                return helpTopics["applications"]
            }
            if (lowerQuery.contains("tip") || lowerQuery.contains("advice") || lowerQuery.contains("help")) {
                return helpTopics["application tips"]
            }
            if (lowerQuery.contains("cover letter") || lowerQuery.contains("introduction")) {
                return helpTopics["cover letter"]
            }
            if (lowerQuery.contains("interview") || lowerQuery.contains("meeting") || lowerQuery.contains("prepare")) {
                return helpTopics["interview preparation"]
            }
        }
        when {
            lowerQuery.contains("forgot password") || lowerQuery.contains("reset password") -> {
                return helpTopics["login"] ?: "You can reset your password on the login screen by clicking the 'Forgot Password' link and following the instructions sent to your email."
            }
            lowerQuery.contains("contact support") || lowerQuery.contains("help desk") || lowerQuery.contains("customer service") -> {
                return helpTopics["contact support"] ?: "You can contact our support team at support@jobrec.com or through the Contact section in the app."
            }
            lowerQuery.contains("what can you do") || lowerQuery.contains("how can you help") || lowerQuery.contains("your capabilities") -> {
                return "I can help you navigate the JobRec app, answer questions about job searching, applications, profile management, and provide guidance on using all features of the app. What would you like to know about?"
            }
            lowerQuery.contains("who made") || lowerQuery.contains("who created") || lowerQuery.contains("developer") -> {
                return "JobRec was developed by a team of talented developers focused on creating an efficient platform to connect job seekers with employers. The app is designed to streamline the recruitment process for both students and companies."
            }
            lowerQuery.contains("purpose") || lowerQuery.contains("goal") || lowerQuery.contains("aim") -> {
                return "The purpose of JobRec is to simplify the job search and recruitment process. We aim to help students find suitable employment opportunities while enabling companies to discover qualified candidates efficiently."
            }
        }
        return null
    }
    private suspend fun saveInteraction(query: String, response: String) {
        try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val interaction = hashMapOf(
                "userId" to userId,
                "query" to query,
                "response" to response,
                "timestamp" to Timestamp.now(),
                "id" to UUID.randomUUID().toString()
            )
            db.collection("chatbot_interactions")
                .add(interaction)
                .await()
            Log.d(TAG, "Saved chatbot interaction")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chatbot interaction", e)
        }
    }
    private fun retrieveHuggingFaceToken(): String {
        return BuildConfig.HUGGING_FACE_TOKEN
    }
}
