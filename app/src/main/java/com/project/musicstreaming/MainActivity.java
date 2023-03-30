package com.project.musicstreaming;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.project.musicstreaming.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    static ArrayList<ListData> dataArrayList = new ArrayList<>();
    static MusicAdapter musicAdapter;
    ListData listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();

        String[] songList = {
                "Imagine",
                "When we were young",
                "Lemon Tree",
                "Come Away With Me",
                "Skyfall",
                "Night Change",
                "Hey Jude",
                "Let It Be"
        };

        String[] articleList = {
                "John Lennon",
                "Adele",
                "Fools Garden",
                "Norah Jones",
                "Adele",
                "Adele",
                "The Beatles",
                "The Beatles"
        };

        for(int i = 0; i < songList.length; i++){
            listData = new ListData(songList[i], articleList[i]);
            dataArrayList.add(listData);
        }
        musicAdapter = new MusicAdapter(MainActivity.this, dataArrayList);
    }

    private void init(){
        replaceFragment(new HomeFragment());
        binding.bottomNavigationView.setBackground(null);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.bottom_home:
                    replaceFragment(new HomeFragment());
                    break;
                case R.id.bottom_search:
                    replaceFragment(new SearchFragment());
                    break;
                case R.id.bottom_favorite:
                    replaceFragment(new FavoriteFragment());
                    break;
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

}