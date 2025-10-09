package moe.enx.loader;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Method;


/**
 * Helper class to properly initialize dynamically loaded Activities
 * Mimics what Android's ActivityThread does
 */
public class ActivityInitializer {
    private static final String TAG = "ActivityInitializer";

    /**
     * Initialize an Activity instance with proper Android context
     * This is necessary before calling onCreate()
     */
    public static boolean initializeActivity(
            Activity targetActivity,
            Context hostContext,
            String className,
            StringBuilder output) {

        try {
            output.append("Initializing activity: " + className + "...\n");

            // Step 1: Attach base context using ContextWrapper.attachBaseContext()
            Method attachBaseContext = android.content.ContextWrapper.class
                .getDeclaredMethod("attachBaseContext", Context.class);
            attachBaseContext.setAccessible(true);
            attachBaseContext.invoke(targetActivity, hostContext.getApplicationContext());
            output.append("[OK] Base context attached!\n\n");

            // Step 2: Try to attach using Activity.attach() for full initialization
            try {
                // Get the attach method - note: signature varies by Android version
                // This is the Android 15+ signature (may need adjustment for other versions)
                Method attach = findAttachMethod();

                if (attach != null) {
                    attach.setAccessible(true);

                    // Create ActivityInfo
                    ActivityInfo info = createActivityInfo(hostContext, className);

                    // Create Intent
                    Intent intent = new Intent();
                    intent.setClassName(info.packageName, info.name);

                    // Call attach with appropriate parameters
                    Object[] params = buildAttachParams(
                        attach,
                        targetActivity,
                        hostContext,
                        info,
                        intent
                    );

                    output.append("Invoking Activity.attach()...\n");
                    attach.invoke(targetActivity, params);
                    output.append("[OK] Activity.attach() called\n");
                    return true;
                }
            } catch (Exception e) {
                output.append("attach() failed, using simplified initialization: " + e.getMessage() + "\n");
                output.append("Continuing initialization...\n");
                return true; // Partial init
            }

            output.append("[OK] Basic initialization complete\n");
            return true;

        } catch (Exception e) {
            output.append("\n=== ERROR ===\n");
            output.append("Failed to initialize activity: ").append(e).append("\n");
            return false;
        }
    }

    /**
     * Find the attach() method - signature varies by Android version
     */
    private static Method findAttachMethod() {
        Method[] methods = Activity.class.getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("attach")) {
                return m;
            }
        }
        return null;
    }

    /**
     * Build parameters for attach() method call
     */
    private static Object[] buildAttachParams(
            Method attachMethod,
            Activity targetActivity,
            Context hostContext,
            ActivityInfo info,
            Intent intent) {

        Class<?>[] paramTypes = attachMethod.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        // Fill in parameters based on type
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> type = paramTypes[i];

            if (type == Context.class) {
                params[i] = hostContext;
            } else if (type == android.app.ActivityThread.class) {
                params[i] = null; // Can be null for testing
            } else if (type == android.app.Instrumentation.class) {
                try {
                    params[i] = ((Activity) hostContext).getInstrumentation();
                } catch (Exception e) {
                    params[i] = null;
                }
            } else if (type == android.os.IBinder.class) {
                params[i] = null;
            } else if (type == int.class || type == Integer.class) {
                params[i] = 0;
            } else if (type == Application.class) {
                params[i] = hostContext.getApplicationContext();
            } else if (type == Intent.class) {
                params[i] = intent;
            } else if (type == ActivityInfo.class) {
                params[i] = info;
            } else if (type == CharSequence.class) {
                params[i] = info.name;
            } else if (type == Activity.class) {
                params[i] = null; // parent
            } else if (type == String.class) {
                params[i] = null;
            } else if (type == android.content.res.Configuration.class) {
                params[i] = hostContext.getResources().getConfiguration();
            } else {
                params[i] = null; // For any other types
            }
        }

        return params;
    }

    /**
     * Create a minimal ActivityInfo for the dynamic activity
     */
    private static ActivityInfo createActivityInfo(Context context, String className) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = context.getPackageName(); // Do we need to set the packageName to the proper one?
        info.name = className;
        info.applicationInfo = context.getApplicationInfo();
        info.taskAffinity = info.packageName;
        info.parentActivityName = null; // Important: set to null to avoid NPE
        info.theme = 0;
        info.launchMode = ActivityInfo.LAUNCH_SINGLE_TOP;
        info.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        return info;
    }

    /**
     * Call onCreate on a properly initialized activity
     */
    public static void callOnCreate(Activity activity, Bundle savedInstanceState)
            throws Exception {
        Method onCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
        onCreate.setAccessible(true);
        onCreate.invoke(activity, savedInstanceState);
    }

    /**
     * Call the full activity lifecycle
     */
    public static void callLifecycleMethods(Activity activity, StringBuilder output) throws Exception {
        output.append("Invoking onCreate()...\n");
        callOnCreate(activity, new Bundle());
        output.append("[OK] onCreate() executed successfully!\n\n");

        // Optionally call other lifecycle methods
        /*try {
            Method onStart = Activity.class.getDeclaredMethod("onStart");
            onStart.setAccessible(true);
            onStart.invoke(activity);
            Log.d(TAG, "onStart() called");
        } catch (Exception e) {
            Log.w(TAG, "onStart() failed: " + e.getMessage());
        }

        try {
            Method onResume = Activity.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            onResume.invoke(activity);
            Log.d(TAG, "onResume() called");
        } catch (Exception e) {
            Log.w(TAG, "onResume() failed: " + e.getMessage());
        }*/
    }
}
