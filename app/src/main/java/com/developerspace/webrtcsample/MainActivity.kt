package com.developerspace.webrtcsample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.developerspace.webrtcsample.databinding.ActivityStartBinding
import com.parse.Parse
import com.parse.ParseObject
import com.parse.ParseQuery

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Constants.isIntiatedNow = true
        Constants.isCallEnded = true
        binding.startMeeting.setOnClickListener {
            if (binding.meetingId.text.toString().trim().isEmpty())
                binding.meetingId.error = "Please enter meeting id"
            else {
                val parseQuery = ParseQuery<ParseObject>("calls2021")
                parseQuery.whereEqualTo("key", binding.meetingId.text.toString())
                parseQuery.findInBackground { objects, e ->
                    run {
                        if (null != e) return@findInBackground
                        if (objects.isNotEmpty()) {
                            val parseObject = objects[0]
                            if (parseObject.containsKey("type") && parseObject.getString("type") == "OFFER" || parseObject.getString(
                                    "type"
                                ) == "ANSWER" || parseObject.getString("type") == "END_CALL"
                            ) {
                                binding.meetingId.error = "Please enter new meeting ID"
                            }
                        } else {
                            val intent = Intent(this@MainActivity, RTCActivity::class.java)
                            intent.putExtra("meetingID", binding.meetingId.text.toString())
                            intent.putExtra("isJoin", false)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
        binding.joinMeeting.setOnClickListener {
            if (binding.meetingId.text.toString().trim().isEmpty())
                binding.meetingId.error = "Please enter meeting id"
            else {
                val intent = Intent(this@MainActivity, RTCActivity::class.java)
                intent.putExtra("meetingID", binding.meetingId.text.toString())
                intent.putExtra("isJoin", true)
                startActivity(intent)
            }
        }

        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("LyeURsBTb01F6o3PQahLMLAoxpiJr5X5f01bmI2u")
                .server("http://101.34.243.201:1337/parse")
                .build()
        )
    }
}