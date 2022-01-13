package org.prowl.themeexampleplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * This should be your main info activity - explain to the user how to use their new shiny themes!
 */
public class PluginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This really is just a blank activity - so make sure you have a working info screen as
        // Torque may make use of this in the future!
        setContentView(R.layout.activity_plugin);
    }
}