package com.example.audiorecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.Audio
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding

    private lateinit var records : ArrayList<AudioRecord>
    private lateinit var mAdapter : Adapter
    private lateinit var db : AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        records = ArrayList()

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        mAdapter = Adapter(records)

        binding.recyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)

        }
    }


    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

}