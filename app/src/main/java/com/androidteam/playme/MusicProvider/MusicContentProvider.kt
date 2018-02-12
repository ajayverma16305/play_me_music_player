package com.androidteam.playme.MusicProvider

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import android.text.format.DateFormat
import com.androidteam.playme.Listeners.OnAudioResourcesReadyListener
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.*

/**
 * Created by AJAY VERMA on 30/01/18.
 * Company : CACAO SOLUTIONS
 */
class MusicContentProvider(private val selfWeak : WeakReference<Context>,private val audioResourceReadyListener
        : OnAudioResourcesReadyListener) : AsyncTask<Void, Void, ArrayList<MusicContent>?>(){

    override fun doInBackground(vararg p0: Void?): ArrayList<MusicContent> {
        return getAllMusicPathList(selfWeak.get()!!)
    }

    private fun getAllMusicPathList(context: Context): ArrayList<MusicContent> {
        val itemList : ArrayList<MusicContent> = ArrayList()

        val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicResolver = context.contentResolver
        val musicCursor = musicResolver.query(songUri, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            var cursorAlbum: Cursor?

            //get columns
            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val songDuration = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val albumName = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val dateAdded = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dateModified = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val size = musicCursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val numberOfTracks = musicCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            do {
                val albumId = java.lang.Long.valueOf(musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)))
                cursorAlbum = musicResolver
                        .query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART),
                                MediaStore.Audio.Albums._ID + "=" + albumId, null, null)

                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)
                val thisAlbumName = musicCursor.getString(albumName)
                val thisDateAdded = getDate(musicCursor.getString(dateAdded).toLong())
                val thisDateModified = getDate(musicCursor.getString(dateModified).toLong())

                var thiNumberOfTracks : String? = null
                thiNumberOfTracks = if(numberOfTracks != -1){
                    musicCursor.getString(musicCursor.getColumnIndex(numberOfTracks.toString()))
                } else {
                    "0"
                }

                val sizeInMb : String = getStringSizeLengthFile(musicCursor.getLong(size))

                val duration : Long = musicCursor.getLong(songDuration)
                var imagePath : String? = null
                var imageData : String? = null

                if (null != cursorAlbum && cursorAlbum.moveToFirst()) {
                    imagePath = cursorAlbum.getString(cursorAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
                    imageData = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                }

                if(imagePath == null){
                    imagePath = ""
                }

                if(imageData == null){
                    imageData = ""
                }

                itemList.add(MusicContent(imagePath,
                        thisId, thisTitle, thisArtist, convertDuration(duration),
                        imageData, thisAlbumName, thisDateAdded, thisDateModified, thiNumberOfTracks!!, sizeInMb))

                cursorAlbum?.close()

            } while (musicCursor.moveToNext())

        }
        musicCursor.close()
        return itemList
    }

    private fun getDate(time: Long): String {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = (time * 1000)
        return DateFormat.format("dd-MM-yyyy", cal).toString()
    }

    private fun getStringSizeLengthFile(size: Long): String {

        val df = DecimalFormat("0.00")

        val sizeKb = 1024.0f
        val sizeMo = sizeKb * sizeKb
        val sizeGo = sizeMo * sizeKb
        val sizeTerra = sizeGo * sizeKb


        if (size < sizeMo)
            return df.format(size / sizeKb) + " KB"
        else if (size < sizeGo)
            return df.format(size / sizeMo) + " MB"
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + " GB"

        return ""
    }

    private fun convertDuration(duration: Long): String {
        var out: String? = null
        var hours: Long = 0
        try {
            hours = duration / 3600000
        } catch (e: Exception) {
            e.message
        }

        val remaining_minutes = (duration - hours * 3600000) / 60000
        var minutes = remaining_minutes.toString()
        if (minutes == "0") {
            minutes = "00"
        }
        val remaining_seconds = duration - hours * 3600000 - remaining_minutes * 60000
        var seconds = remaining_seconds.toString()
        if (seconds.length < 2) {
            seconds = "00"
        } else {
            seconds = seconds.substring(0, 2)
        }

        out = if (hours > 0) {
            hours.toString() + ":" + minutes + ":" + seconds
        } else {
            minutes + ":" + seconds
        }
        return out
    }

    override fun onPostExecute(result: ArrayList<MusicContent>?) {
        audioResourceReadyListener.resourcesList(result)
    }
}

