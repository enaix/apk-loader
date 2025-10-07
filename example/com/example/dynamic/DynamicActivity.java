package com.example.dynamic;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class DynamicActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DynamicLoad", "DynamicActivity loaded dynamically!");
        System.out.println("Dynamic execution successful!");
    }
}
