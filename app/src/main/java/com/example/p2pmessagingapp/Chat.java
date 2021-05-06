package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Chat extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle bundle= getIntent().getExtras();

        if(bundle!=null){
            String ip=bundle.getString("ip");
            String port= bundle.getString("port");
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu_layout,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fileMenu:
                return true;
            case R.id.backgroundMenu:

                // showHelp();
                return true;
            case R.id.saveMenu:

                // showHelp();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}