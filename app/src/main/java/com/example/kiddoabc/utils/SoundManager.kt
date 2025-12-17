package com.example.kiddoabc.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.IOException

/**
 * Gestionnaire des sons de l'application
 */
class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()

    init {
        setupSoundPool()
    }

    /**
     * Configure le SoundPool pour les sons courts
     */
    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    /**
     * Précharge un son dans le SoundPool
     */
    fun preloadSound(soundFileName: String) {
        try {
            val assetFileDescriptor = context.assets.openFd("sounds/$soundFileName")
            val soundId = soundPool?.load(assetFileDescriptor, 1)
            if (soundId != null && soundId > 0) {
                soundMap[soundFileName] = soundId
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Joue un son court (pour les lettres)
     */
    fun playSound(soundFileName: String, onCompletion: (() -> Unit)? = null) {
        val soundId = soundMap[soundFileName]
        if (soundId != null) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            // SoundPool n'a pas de callback de completion, utiliser un délai approximatif si nécessaire
            onCompletion?.invoke()
        } else {
            // Si le son n'est pas préchargé, le charger et le jouer avec MediaPlayer
            playWithMediaPlayer(soundFileName, onCompletion)
        }
    }

    /**
     * Joue un son avec MediaPlayer (pour les sons plus longs)
     */
    private fun playWithMediaPlayer(soundFileName: String, onCompletion: (() -> Unit)? = null) {
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                val afd = context.assets.openFd("sounds/$soundFileName")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                setOnPreparedListener {
                    start()
                }

                setOnCompletionListener {
                    onCompletion?.invoke()
                    releaseMediaPlayer()
                }

                setOnErrorListener { _, _, _ ->
                    releaseMediaPlayer()
                    false
                }

                prepareAsync()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Joue un son d'encouragement
     */
    fun playEncouragementSound(score: Float) {
        val soundFile = when {
            score >= 90 -> "excellent.mp3"
            score >= 80 -> "very_good.mp3"
            score >= 70 -> "good.mp3"
            score >= 60 -> "not_bad.mp3"
            else -> "keep_trying.mp3"
        }
        playWithMediaPlayer(soundFile)
    }

    /**
     * Arrête tous les sons
     */
    fun stopAll() {
        releaseMediaPlayer()
        soundPool?.autoPause()
    }

    /**
     * Libère le MediaPlayer
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    /**
     * Libère toutes les ressources
     */
    fun release() {
        releaseMediaPlayer()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}