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

    private val mediaSessionCallback = object : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { mediaItem ->
                if (mediaItem.requestMetadata.mediaUri != null) {
                    val metadata = getMetadataFromUri(this@PlaybackService, mediaItem.requestMetadata.mediaUri!!)
                    mediaItem.buildUpon().setMediaMetadata(metadata).build()
                } else {
                    mediaItem
                }
            }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(mediaSessionCallback)
            .build()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun getMetadataFromUri(context: Context, uri: android.net.Uri): MediaMetadata {
        val retriever = MediaMetadataRetriever()
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
