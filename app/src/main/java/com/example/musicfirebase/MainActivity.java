package com.example.musicfirebase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.example.musicfirebase.activities.SignInActivity;
import com.example.musicfirebase.utils.GoogleServices;
import com.example.musicfirebase.viewmodels.PlaylistViewModel;
import com.example.musicfirebase.viewmodels.SongViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    GoogleServices services;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_hub);

        drawerLayout = findViewById(R.id.drawerLayout);

        SongViewModel songVM = new ViewModelProvider(this).get(SongViewModel.class);
        PlaylistViewModel playlistVM = new ViewModelProvider(this).get(PlaylistViewModel.class);

        songVM.getSongsFromDb();
        playlistVM.getPlaylistsFromDb(Query.Direction.ASCENDING);

        // Day UI
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

        Window window = getWindow();
        View decorView = window.getDecorView();
        window.setNavigationBarColor(getColor(R.color.white));
        window.setStatusBarColor(getColor(R.color.white));
        decorView.setSystemUiVisibility(uiOptions);

        BottomNavigationView bottomNav = findViewById(R.id.navBar);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        assert navHostFragment != null;
        NavigationUI.setupWithNavController(bottomNav, navHostFragment.getNavController());

        NavigationView navigationView = findViewById(R.id.drawer_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation item clicks here
                drawerLayout.closeDrawer(GravityCompat.START);

                NavController navController = NavHostFragment.findNavController(navHostFragment);

                switch (item.getItemId()){
                    case R.id.drawer_album:
                        navController.navigate(R.id.nav_playlists);
                        Toast.makeText(MainActivity.this, "Album clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_library:
                        navController.navigate(R.id.nav_library);
                        Toast.makeText(MainActivity.this, "Library clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_like:
                        navController.navigate(R.id.nav_liked);
                        Toast.makeText(MainActivity.this, "Like clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_logout:
                        logout();
                        return true;
                    default:
                        return false;
                }

            }
        });

        // Update the drawer header
        updateDrawerHeader();

    }

    /**
     * Hide the annoying keyboard without a stupid long method!
     *
     * @param v The current view
     */
    public static void hideKeyboardIn(View v) {
        ((InputMethodManager) v.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void updateDrawerHeader() {
        // Retrieve the user information from GoogleSignInAccount object

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        assert account != null;

        String name = account.getDisplayName();
        String email = account.getEmail();
        Uri photoUrl = account.getPhotoUrl();

        Log.d("MainActivity", "User name: " + name);
        Log.d("MainActivity", "User email: " + email);

        // Get the navigation drawer header view
        NavigationView navigationView = findViewById(R.id.drawer_view);
        View headerView = navigationView.getHeaderView(0);

        // Set the user name and email in the header view
        TextView nameTextView = headerView.findViewById(R.id.drawer_user_name);
        TextView emailTextView = headerView.findViewById(R.id.drawer_user_email);
        nameTextView.setText(name);
        emailTextView.setText(email);

        // Load and display the avatar image
        ImageView avatarImageView = headerView.findViewById(R.id.drawer_user_avatar);
        if (photoUrl != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.aklogo) // Placeholder image while loading
                    .error(R.drawable.aklogo) // Error image if loading fails
                    .circleCrop()
                    .into(avatarImageView);
        } else {
            avatarImageView.setImageResource(R.drawable.aklogo);
        }
    }

    private void logout() {
        GoogleServices googleServices = new GoogleServices();
        GoogleSignInOptions gso = googleServices.getGso();
        GoogleSignInClient gsc = googleServices.getGsc(MainActivity.this, gso);

        gsc.signOut().addOnCompleteListener(this, task -> {
            // Sign out successful
            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();

            });
    }
}
