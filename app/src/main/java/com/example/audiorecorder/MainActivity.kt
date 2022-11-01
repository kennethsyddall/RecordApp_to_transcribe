package com.example.audiorecorder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.audiorecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.jar.Manifest

const val REQUEST_CODE = 200
class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding

    private var permission = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false

    private var duration = ""

    private lateinit var vibrator: Vibrator

    private lateinit var timer: Timer

    private lateinit var db: AppDatabase

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permission[0]) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()


        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecording()
            // TODO
            Toast.makeText(this,"Record Saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.setOnClickListener {
            stopRecording()
            File("$dirPath$filename.mp3")
            Toast.makeText(this,"Record Deleted", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun save(){
        val dateTimeNow = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formatted = dateTimeNow.format(formatter)
        val newFilename: String = formatted.toString()

        if(newFilename != filename){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$filename.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.mp3"
        var timestamp:Long = Date().time

        var record = AudioRecord(newFilename, filePath, timestamp, duration)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseRecording(){
        recorder?.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)

        timer.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resumeRecording(){
        recorder?.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)

        timer.start()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")

            try {
                prepare()
            }catch (e: IOException){}

            start()
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE

    }

    private fun stopRecording(){
        timer.stop()

        recorder?.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disabled)

        binding.tvTimer.text = "00:00.00"
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)

    }
}