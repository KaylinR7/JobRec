package com.example.jobrec.chatbot
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class ChatbotActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var messageInput: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatbotRepository: ChatbotRepository
    private val messages = mutableListOf<ChatMessage>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)
        chatbotRepository = ChatbotRepository(this)
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        addBotMessage("Hi there! I'm your CareerWorx assistant. How can I help you today?")
    }
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
    }
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "CareerWorx Assistant"
        }
    }
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
        updateEmptyView()
    }
    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }
    private fun sendMessage(message: String) {
        addUserMessage(message)
        messageInput.text?.clear()
        addTypingIndicator()
        lifecycleScope.launch {
            try {
                kotlinx.coroutines.delay(800)
                val response = chatbotRepository.getChatbotResponse(message)
                removeTypingIndicator()
                addBotMessage(response)
            } catch (e: Exception) {
                removeTypingIndicator()
                addBotMessage("I'm sorry, I'm having trouble right now. Please try again later.")
            }
        }
    }
    private fun addTypingIndicator() {
        val typingMessage = ChatMessage(
            id = "typing_indicator",
            message = "Typing...",
            isFromUser = false,
            timestamp = Timestamp.now()
        )
        messages.add(typingMessage)
        updateRecyclerView()
    }
    private fun removeTypingIndicator() {
        val typingIndex = messages.indexOfFirst { it.id == "typing_indicator" }
        if (typingIndex != -1) {
            messages.removeAt(typingIndex)
            chatAdapter.notifyItemRemoved(typingIndex)
        }
    }
    private fun addUserMessage(message: String) {
        val chatMessage = ChatMessage(
            message = message,
            isFromUser = true,
            timestamp = Timestamp.now()
        )
        messages.add(chatMessage)
        updateRecyclerView()
    }
    private fun addBotMessage(message: String) {
        val chatMessage = ChatMessage(
            message = message,
            isFromUser = false,
            timestamp = Timestamp.now()
        )
        messages.add(chatMessage)
        updateRecyclerView()
    }
    private fun updateRecyclerView() {
        chatAdapter.notifyItemInserted(messages.size - 1)
        messagesRecyclerView.scrollToPosition(messages.size - 1)
        updateEmptyView()
    }
    private fun updateEmptyView() {
        if (messages.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            messagesRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            messagesRecyclerView.visibility = View.VISIBLE
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
