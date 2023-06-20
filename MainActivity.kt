#written primarily by ChatGPT at the direction of Mark Dailey, aka MarkTheRusty

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var contactButton: Button
    private lateinit var newButton: Button
    private lateinit var backButton: Button

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFile: String? = null
    private var messageList: ArrayList<String> = ArrayList()

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val REQUEST_CONTACT_PICK = 201
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)

    private var currentIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        recordButton = findViewById(R.id.recordButton)
        playButton = findViewById(R.id.playButton)
        contactButton = findViewById(R.id.contactButton)
        newButton = findViewById(R.id.newButton)
        backButton = findViewById(R.id.backButton)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        recordButton.setOnClickListener {
            if (permissionToRecordAccepted) {
                if (recordButton.text == "Record") {
                    startRecording()
                    recordButton.text = "Stop"
                } else {
                    stopRecording()
                    recordButton.text = "Record"
                }
            } else {
                Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show()
            }
        }

        playButton.setOnClickListener {
            if (outputFile != null) {
                if (playButton.text == "Play") {
                    startPlayback()
                    playButton.text = "Stop"
                } else {
                    stopPlayback()
                    playButton.text = "Play"
                }
            }
        }

        contactButton.setOnClickListener {
            pickContact()
        }

        newButton.setOnClickListener {
            showNewConversationDialog()
        }

        backButton.setOnClickListener {
            showIndex()
        }

        // Load the messages index by default
        showIndex()
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder()
        outputFile = Environment.getExternalStorageDirectory().absolutePath + "/recording.3gp"
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            try {
                prepare()
                start()
                Toast.makeText(this@MainActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

        // Send the recorded audio as an MMS message to the selected contact
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra("address", "<recipient_phone_number>")
        intent.putExtra("sms

_body", "")
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$outputFile"))
        intent.type = "audio/3gp"
        startActivity(intent)
    }

    private fun startPlayback() {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.apply {
            try {
                setDataSource(outputFile)
                prepare()
                start()
                Toast.makeText(this@MainActivity, "Playback started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        Toast.makeText(this, "Playback stopped", Toast.LENGTH_SHORT).show()
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, REQUEST_CONTACT_PICK)
    }

    private fun showIndex() {
        currentIndex = -1
        listView.adapter = MessageAdapter(this, messageList)
        recordButton.visibility = View.VISIBLE
        playButton.visibility = View.VISIBLE
        contactButton.visibility = View.VISIBLE
        newButton.visibility = View.VISIBLE
        backButton.visibility = View.GONE
        listView.setOnItemClickListener { _, _, position, _ ->
            currentIndex = position
            showConversation()
        }
    }

    private fun showConversation() {
        if (currentIndex >= 0 && currentIndex < messageList.size) {
            listView.adapter = ConversationAdapter(this, messageList[currentIndex])
            recordButton.visibility = View.GONE
            playButton.visibility = View.GONE
            contactButton.visibility = View.GONE
            newButton.visibility = View.GONE
            backButton.visibility = View.VISIBLE
        }
    }
          private fun stopRecording() {
    mediaRecorder?.apply {
        stop()
        release()
    }
    mediaRecorder = null
    Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

    // Retrieve the recipient phone number from the conversation
    val recipientPhoneNumber = if (currentIndex >= 0 && currentIndex < messageList.size) {
        messageList[currentIndex]
    } else {
        "<recipient_phone_number>"
    }

    // Format the recipient phone number as needed
    val formattedRecipientPhoneNumber = formatPhoneNumber(recipientPhoneNumber)

    // Send the recorded audio as an MMS message to the recipient phone number
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra("address", formattedRecipientPhoneNumber)
    intent.putExtra("sms_body", "")
    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$outputFile"))
    intent.type = "audio/3gp"
    startActivity(intent)
}


    private fun showNewConversationDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_conversation, null)
        builder.setView(dialogView)
            .setTitle("New Conversation")
            .setPositiveButton("Start") { _, _ ->
                val phoneNumberEditText = dialogView.findViewById<EditText>(R.id.phoneNumberEditText)
                val phoneNumber = phoneNumberEditText.text.toString().trim()
                createConversation(phoneNumber)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun createConversation(phoneNumber: String) {
        // TODO: Add code to create a new conversation based on the phone number
        // For now, we will just simulate adding a new conversation to the list
        messageList.add(phoneNumber)
        currentIndex = messageList.size - 1
        showConversation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONTACT_PICK && resultCode == Activity.RESULT_OK) {
            val contactUri = data?.data
            // Perform further operations with the selected contact
        }
    }
}
