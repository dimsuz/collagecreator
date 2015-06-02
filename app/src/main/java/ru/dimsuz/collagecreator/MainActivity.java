package ru.dimsuz.collagecreator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import ru.dimsuz.collagecreator.network.InstagramClient;


public class MainActivity extends AppCompatActivity {
    @Inject
    InstagramClient instagramClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CollageCreatorApp.get(this).inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

}
