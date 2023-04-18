package com.example.musicfirebase.viewmodels;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.musicfirebase.models.Playlist;
import com.example.musicfirebase.utils.PureLiveData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PlaylistViewModel extends ViewModel {
    private final String TAG = "(mStream)";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final PureLiveData<List<Playlist>> myPlaylists = new PureLiveData<>(new ArrayList<>());

    /**
     * Fetch all the playlists from Firestore and save it into myPlaylists.
     *
     * @param order The sorting order of the fetching. Can be {@link Query.Direction ASCENDING or DESCENDING}.
     */
    // myPlaylists is merely a localised instance of all the playlists from Firestore.
    // This makes updating the UI much easier, as all songs are added at the same time, rather than
    // waiting to iterate through the database.
    @SuppressWarnings("unchecked")
    public void getPlaylistsFromDb(Query.Direction order) {
        db.collection("playlists")
                .orderBy("title", order)
                .get()
                .addOnCompleteListener(task -> {
                    List<Playlist> tempPlaylists = new ArrayList<>();

                    task.getResult().forEach(docSnap -> {
                        Playlist playlist = new Playlist(
                                docSnap.getId(),
                                docSnap.getString("title"),
                                (List<DocumentReference>) docSnap.get("songs"));

                        tempPlaylists.add(playlist);
                    });

                    myPlaylists.setValue(tempPlaylists);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to grab playlists from Firestore"));
    }

    /**
     * Creates a new playlist with a compulsory song.
     * This option is only shown when the user wants to add a song to a new playlist.
     *
     * @param title  The title of the playlist
     * @param songId The compulsory to add
     */
    // In order to get around some Firestore quirks, creating a new playlist requires the user to
    // include a song along with the name of the new playlist. This may seem unintuitive, but it
    // also helps to reduce a significant amount of code that might deem unnecessary.
    public void createNewPlaylist(String title, String songId) {
        Playlist playlist = new Playlist(title);
        db.collection("playlists")
                .add(playlist)
                .addOnSuccessListener(docRef ->
                        addSongToExistingPlaylist(docRef.getId(), songId))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    /**
     * Adds a song to a specified existing playlist.
     *
     * @param playlistId The playlist ID to add the song to
     * @param songId     The song ID to add
     */
    // While constructing an entirely new song object with the same particulars in Firestore may be
    // easier and faster, we would rather get the existing song's document reference / file path
    // in the database. This will make working with Firestore very tedious, but this ensures that
    // we always query the same song object, as it might have flags that should be persistent across
    // the app, e.g. whether the song is liked.
    public void addSongToExistingPlaylist(String playlistId, String songId) {
        // gets specified doc refs
        DocumentReference playlistRef =
                db.collection("playlists")
                        .document(playlistId);
        DocumentReference songRef =
                db.collection("songs")
                        .document(songId);

        // add the song ref to the existing playlist
        playlistRef.update("songs", FieldValue.arrayUnion(songRef))
                .addOnSuccessListener(none -> Log.d(TAG, String.format("(%s) đã thêm vào [%s]",
                        songRef.getId(), playlistRef.getId())))
                .addOnFailureListener(e -> Log.w(TAG, String.format("Thất bại khi thêm (%s) vào [%s]",
                        songRef.getId(), playlistRef.getId()) , e));
    }

    /**
     * Deletes a playlist at a specified path.
     *
     * @param docRef The playlist path to delete
     */
    // We can delete a playlist in Firestore by referring to its document path. Nothing much to say.
    public void deletePlaylistInDb(String docRef) {
        db.collection("playlists")
                .document(docRef)
                .delete()
                .addOnSuccessListener(none -> Log.d(TAG,
                        "Đã xóa " + docRef + " từ Firestore"))
                .addOnFailureListener(e -> Log.w(TAG,
                        "Xóa thất bại " + docRef + " từ Firestore", e));
    }

    public PureLiveData<List<Playlist>> getPlaylists() {
        return myPlaylists;
    }
}
