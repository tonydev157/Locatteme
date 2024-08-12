package com.tonymen.locatteme.view

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityMediaPreviewBinding
import java.util.*

class MediaPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPreviewBinding
    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaUri: Uri? = intent.getStringExtra("MEDIA_URI")?.let { Uri.parse(it) }
        val mediaType: String? = intent.getStringExtra("MEDIA_TYPE")
        val isSent = intent.getBooleanExtra("IS_SENT", false)

        Log.d("MediaPreviewActivity", "mediaUri: $mediaUri, mediaType: $mediaType, isSent: $isSent")

        when (mediaType) {
            "IMAGE" -> {
                if (mediaUri != null) {
                    Glide.with(this)
                        .load(mediaUri)
                        .into(binding.imageViewPreview)
                } else {
                    Toast.makeText(this, "Error: URI de la imagen no válida", Toast.LENGTH_SHORT).show()
                }
                binding.imageViewPreview.visibility = View.VISIBLE
                binding.videoViewPreview.visibility = View.GONE
                binding.audioPreviewContainer.visibility = View.GONE
            }
            "VIDEO" -> {
                if (mediaUri != null) {
                    binding.videoViewPreview.setVideoURI(mediaUri)

                    val mediaController = MediaController(this)
                    mediaController.setAnchorView(binding.videoViewPreview)
                    binding.videoViewPreview.setMediaController(mediaController)

                    binding.videoViewPreview.setOnPreparedListener { mediaPlayer ->
                        binding.videoViewPreview.start()
                    }
                    binding.videoViewPreview.setOnErrorListener { _, _, _ ->
                        Toast.makeText(this, "Error al reproducir el video", Toast.LENGTH_SHORT).show()
                        true
                    }
                    binding.videoViewPreview.visibility = View.VISIBLE
                    binding.imageViewPreview.visibility = View.GONE
                    binding.audioPreviewContainer.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Error: URI del video no válida", Toast.LENGTH_SHORT).show()
                }
            }
            "AUDIO" -> {
                setupAudioPlayer(mediaUri)
                binding.audioPreviewContainer.visibility = View.VISIBLE
                binding.imageViewPreview.visibility = View.GONE
                binding.videoViewPreview.visibility = View.GONE
            }
            else -> {
                Toast.makeText(this, "Tipo de archivo no soportado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        if (isSent) {
            binding.sendButton.visibility = View.GONE
        } else {
            binding.sendButton.visibility = View.VISIBLE
            binding.sendButton.setOnClickListener {
                val resultIntent = intent.apply {
                    putExtra("MEDIA_URI", mediaUri.toString())
                    putExtra("MEDIA_TYPE", mediaType)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Ejecuta el mismo comportamiento que el botón de cancelar
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun setupAudioPlayer(mediaUri: Uri?) {
        mediaUri?.let {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MediaPreviewActivity, it)
                prepare()
                setOnPreparedListener {
                    binding.audioPlayPauseButton.setOnClickListener { togglePlayPause() }
                    binding.audioSeekBar.max = mediaPlayer?.duration ?: 0

                    startUpdatingSeekBar()

                    binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                mediaPlayer?.seekTo(progress)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })

                    mediaPlayer?.setOnCompletionListener {
                        binding.audioPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
                        binding.audioSeekBar.progress = 0
                        stopUpdatingSeekBar()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@MediaPreviewActivity, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    private fun startUpdatingSeekBar() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            binding.audioSeekBar.progress = it.currentPosition
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    private fun stopUpdatingSeekBar() {
        timer?.cancel()
        timer = null
    }

    private fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                binding.audioPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
            } else {
                it.start()
                binding.audioPlayPauseButton.setImageResource(R.drawable.ic_pause)
                startUpdatingSeekBar()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        stopUpdatingSeekBar()
    }
}
