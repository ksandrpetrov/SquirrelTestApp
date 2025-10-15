package com.example.heis2025

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // 1. Создаем кастомный MediaSession.Callback
    private val mediaSessionCallback = object : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        // 2. Вся логика onAddMediaItems теперь находится здесь
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { mediaItem ->
                // Ваша логика по извлечению метаданных
                if (mediaItem.requestMetadata.mediaUri != null) {
                    val metadata = getMetadataFromUri(this@PlaybackService, mediaItem.requestMetadata.mediaUri!!)
                    mediaItem.buildUpon().setMediaMetadata(metadata).build()
                } else {
                    mediaItem
                }
            }.toMutableList()
            // Возвращаем обновленный список, обернутый в ListenableFuture
            return Futures.immediateFuture(updatedMediaItems)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        // 3. Передаем наш кастомный колбэк при создании MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(mediaSessionCallback)
            .build()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // 4. УДАЛЯЕМ старый метод onAddMediaItems из класса PlaybackService
    // override fun onAddMediaItems(...) { ... }

    private fun getMetadataFromUri(context: Context, uri: android.net.Uri): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        // Обертка в try...finally для гарантированного освобождения ресурсов
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val albumArt = retriever.embeddedPicture

            return MediaMetadata.Builder()
                .setTitle(title ?: "Unknown Title")
                .setArtist(artist ?: "Unknown Artist")
                .setArtworkData(albumArt, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
        } finally {
            retriever.release()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
