package com.developerspace.webrtcsample

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Log.INFO
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parse.*
import kotlinx.android.synthetic.main.activity_start.*

class MainActivity : AppCompatActivity() {

    val db = Firebase.firestore

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        Constants.isIntiatedNow = true
        Constants.isCallEnded = true
        start_meeting.setOnClickListener {
            if (meeting_id.text.toString().trim().isNullOrEmpty())
                meeting_id.error = "Please enter meeting id"
            else {
                val query = ParseQuery.getQuery<ParseObject>("calls")
                query.getInBackground(meeting_id.text.toString()).onSuccessTask {
                    Log.e("parse platform", it.result.toString())
                    return@onSuccessTask it
                }.onSuccess {
                    Log.e("parse platform", it.result.toString())
                }
                db.collection("calls")
                    .document(meeting_id.text.toString())
                    .get()
                    .addOnSuccessListener {
                        Log.e("firebase", it.toString())
                        Toast.makeText(this, "firebase$it", 10).show()
                        if (it["type"] == "OFFER" || it["type"] == "ANSWER" || it["type"] == "END_CALL") {
                            meeting_id.error = "Please enter new meeting ID"
                        } else {
                            val intent = Intent(this@MainActivity, RTCActivity::class.java)
                            intent.putExtra("meetingID", meeting_id.text.toString())
                            intent.putExtra("isJoin", false)
                            startActivity(intent)
                        }
                    }
                    .addOnFailureListener {
                        meeting_id.error = "Please enter new meeting ID"
                    }
            }
        }
        join_meeting.setOnClickListener {
            if (meeting_id.text.toString().trim().isNullOrEmpty())
                meeting_id.error = "Please enter meeting id"
            else {
                val intent = Intent(this@MainActivity, RTCActivity::class.java)
                intent.putExtra("meetingID", meeting_id.text.toString())
                intent.putExtra("isJoin", true)
                startActivity(intent)
            }
        }
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("LyeURsBTb01F6o3PQahLMLAoxpiJr5X5f01bmI2u")
                .server("http://101.34.243.201:1337/parse/")
                .build()
        )
    }
}