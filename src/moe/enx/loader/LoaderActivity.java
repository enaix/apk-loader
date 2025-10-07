package moe.enx.loader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Space;
import android.view.ViewGroup;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.Method;

public class LoaderActivity extends Activity {
    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = "DynamicLoader";
    
    private TextView statusText;
    private EditText activityNameInput;
    private Button pickButton;
    private Button executeButton;
    private String selectedFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(createUI());
        
        pickButton.setOnClickListener(v -> pickFile());
        executeButton.setOnClickListener(v -> loadAndExecute());
    }

    private android.view.View createUI() {
        // Add layout to prevent title bar overlap
        LinearLayout base_lyt = new LinearLayout(this);
        base_lyt.setOrientation(LinearLayout.VERTICAL);
        // Spacer
        Space space = new Space(this);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(1, 320);
        space.setLayoutParams(params);
        base_lyt.addView(space);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        statusText = new TextView(this);
        statusText.setText("Select an APK file to load");
        statusText.setPadding(0, 0, 0, 20);
        statusText.setTextSize(12);
        
        pickButton = new Button(this);
        pickButton.setText("Pick APK File");
        
        TextView activityLabel = new TextView(this);
        activityLabel.setText("Activity Class Name:");
        activityLabel.setPadding(0, 30, 0, 10);
        
        activityNameInput = new EditText(this);
        activityNameInput.setHint("com.example.dynamic.DynamicActivity");
        activityNameInput.setText("com.example.dynamic.DynamicActivity");
        activityNameInput.setPadding(20, 20, 20, 20);
        
        executeButton = new Button(this);
        executeButton.setText("Load & Execute");
        executeButton.setEnabled(false);
        
        layout.addView(statusText);
        layout.addView(pickButton);
        layout.addView(activityLabel);
        layout.addView(activityNameInput);
        layout.addView(executeButton);
        
        scrollView.addView(layout);
        base_lyt.addView(scrollView);
        return base_lyt;
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                selectedFilePath = uri.getPath();
                statusText.setText("Selected: " + selectedFilePath + "\n");
                executeButton.setEnabled(true);
                Log.d(TAG, "Selected file: " + selectedFilePath);
            }
        }
    }

    private void loadAndExecute() {
        StringBuilder output = new StringBuilder();
        output.append("=== Loading APK ===\n");
        output.append("File: ").append(selectedFilePath).append("\n");
        output.append("Activity: ").append(activityNameInput.getText()).append("\n\n");
       
	String activityName = activityNameInput.getText().toString();

        try {
            // Create PathClassLoader
            output.append("Creating PathClassLoader...\n");
            PathClassLoader classLoader = new PathClassLoader(
                selectedFilePath,
                getClassLoader()
            );
            
            // Load the Activity class
            output.append("Loading class: ").append(activityName).append("\n");
            
            Class<?> activityClass = classLoader.loadClass(activityName);
            output.append("[OK] Class loaded: ").append(activityClass.getName()).append("\n\n");
            
            // Create instance
            output.append("Creating instance...\n");
            Object activityInstance = activityClass
                .getDeclaredConstructor()
                .newInstance();
            output.append("[OK] Instance created\n\n");
            
            // Find and invoke onCreate method
            output.append("Invoking onCreate()...\n");
            Method onCreateMethod = activityClass.getMethod(
                "onCreate",
                Bundle.class
            );
            
            onCreateMethod.invoke(activityInstance, new Bundle());
            output.append("[OK] onCreate() executed successfully!\n\n");
            
            // Print local directory contents for debug
            output.append("=== Local Directory Contents ===\n");
            File filesDir = getFilesDir();
            output.append("Path: ").append(filesDir.getAbsolutePath()).append("\n\n");
            
            printDirectoryContents(filesDir, output, 0);
            
            // Also check cache dir
            output.append("\n=== Cache Directory ===\n");
            File cacheDir = getCacheDir();
            output.append("Path: ").append(cacheDir.getAbsolutePath()).append("\n\n");
            printDirectoryContents(cacheDir, output, 0);
            
            statusText.setText(output.toString());
            Log.d(TAG, output.toString());
            
            Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            output.append("\n=== ERROR ===\n");
            output.append("Exception: ").append(e.getClass().getName()).append("\n");
            output.append("Message: ").append(e.getMessage()).append("\n");
            output.append("Cause: ").append(e.getCause()).append("\n");
            
            statusText.setText(output.toString());
            Log.e(TAG, "Error loading APK", e);
            e.printStackTrace();
        }
    }
    
    private void printDirectoryContents(File dir, StringBuilder output, int depth) {
        if (!dir.exists()) {
            output.append("Directory does not exist\n");
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            output.append("(empty)\n");
            return;
        }
        
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        
        for (File file : files) {
            output.append(indent);
            if (file.isDirectory()) {
                output.append("[d] ").append(file.getName()).append("/\n");
                if (depth < 2) { // Limit recursion depth
                    printDirectoryContents(file, output, depth + 1);
                }
            } else {
                output.append("[f] ").append(file.getName());
                output.append(" (").append(file.length()).append(" bytes)\n");
            }
        }
    }
}
