package org.prowl.themeexampleplugin;

import org.prowl.torque.remote.ITorqueService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * themeExamplePlugin
 * <p>
 * A quickly written plugin to send Themes at the start of Torque so the app has them.
 * <p>
 * Themes are stored in the assets folder with the thumbnail and descriptions at the top level
 * and a matching [folder name] which contains the theme which Torque will ask for
 * <p>
 * Try to implement an 'info' screen in PluginActivity as Torque may make use of this in later
 * revisions (and your users like app icons to tap to get info on what the app/plugin is anyway)
 * <p>
 * Make note of the use of proguard-rules.pro which you will also need to change the packagename when
 * you come to make your own plugin, if you're using this reference code.
 */
public class PluginReceiver extends BroadcastReceiver {

    // The 'simple' name of your plugin!
    private static final String NAME = "MyThemePluginName";

    // Intents and things you shouldn't need to change.
    private static final String ASSET_LOCATION = "content://" + BuildConfig.APPLICATION_ID + "/assets/";
    private static final String INTENT_THEME_QUERY = "org.prowl.torque.THEME_QUERY";
    private static final String INTENT_TORQUE_QUITTING = "org.prowl.torque.APP_QUITTING"; // If you want to use this intent, make sure you uncomment the relevant bit in the AndroidManifest.xml too!

    // We _must_ use a notificationManager in the latest android versions and must use a foreground service
    private static NotificationManager mNM;
    private static final int NOTIFICATION_ID = 5;

    // Deal with the broadcast intent that android sent us (defined in the xml)
    public void onReceive(Context context, Intent intent) {
        Debug.w("Broadcast received:" + intent.getAction());

        // Torque will send the theme_query intent each time the user navigates to the theme
        // selection screen
        if (INTENT_THEME_QUERY.equals(intent.getAction())) {
            // Start the service and then send data to torque.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, PluginService.class));
            } else {
                context.startService(new Intent(context, PluginService.class));
            }
        } else if (INTENT_TORQUE_QUITTING.equals(intent.getAction())) {
            // Main torque app quitting
            // Nothing really to do here, maybe quit as well?
        }
    }


    //===================== A comment telling you the service code is below ================

    /**
     * We need this as we can't bind to the context that the broadcast was sent with.
     */
    public static class PluginService extends Service {

        protected ITorqueService torqueService;

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Debug.w("Theme Plugin Service started");

            // Bind to the torque service
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
            boolean successfulBind = bindService(serviceIntent, connection, 0);
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onCreate() {
            super.onCreate();

            mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Display a notification about us starting.  We put an icon in the status bar.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, getNotification());
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }

        /**
         * Bits of service code. You usually won't need to change this
         */
        private ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                torqueService = ITorqueService.Stub.asInterface(service);

                try {
                    // Version too old.
                    if (torqueService.getVersion() < 35) {
                        // FIXME: You may want to make this a popup message or start your main 'info' activity instead of a log entry in android's log.
                        Debug.w("Your version of Torque is too old! - Please upgrade it!");
                        return;
                    }
                } catch (RemoteException e) {
                    Debug.e(e.getMessage(), e);
                    return;
                }

                // If we got here then everything is good. Now it's time to send some data
                sendThemeData();

                try {
                    // Now that's done, we can close the service.
                    unbindService(connection);
                    stopSelf();
                } catch (Throwable e) {
                    Debug.e(e.getMessage(), e);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };


        /**
         * Now send the theme data to Torque
         */
        public void sendThemeData() {
            Log.d(NAME, "Sending Theme data");

            try {
                // Get the list of 'themes'
                String[] topFiles = getAssets().list("");
                // Now for each theme, get the info and send it on it's way

                for (String topFile : topFiles) {

                    // We cheat a bit here
                    if (topFile.endsWith(".txt")) {
                        String folder = topFile.substring(0, topFile.indexOf("."));
                        Debug.w("Will use theme folder: " + folder);

                        // Load the properties
                        Properties themeDetails = new Properties();
                        themeDetails.load(getAssets().open(folder + ".txt"));

                        // Make the bitmap uri for the thumbnail
                        // Torque will cache this thumbnail for up to an hour. So don't get worried
                        // if you're changing it and it doesn't update.
                        String thumbnailUri = ASSET_LOCATION + folder + ".png";
                        makeExportable(thumbnailUri);

                        // Get the list of files for the theme
                        String[] files = getAssets().list(folder);
                        List<String> uriList = new ArrayList<>();
                        for (String file : files) {
                            // Get the Uri and then give permission for torque to read the file
                            uriList.add(makeExportable(ASSET_LOCATION + folder + "/" + file));
                            Debug.w(" Folder has file: " + file);
                        }

                        // Send the info off to Torque and it should then appear in the themes list!
                        torqueService.putThemeData(BuildConfig.APPLICATION_ID,
                                themeDetails.getProperty("name", folder),
                                themeDetails.getProperty("description", folder),
                                themeDetails.getProperty("author", folder),
                                thumbnailUri,
                                uriList.toArray(new String[0]));
                    }
                }
            } catch (Throwable e) {
                Debug.e(e.getMessage(), e);
            }
        }

        public String makeExportable(String assetUri) {
            Uri contentUri = Uri.parse(assetUri);
            grantUriPermission("org.prowl.torque", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return contentUri.toString();
        }


        /**
         * Show a notification while this service is running.
         */
        private Notification getNotification() {

            Notification.Builder buildr = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)  // the status icon
                    .setTicker(NAME)  // the status text
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentTitle("PID Example title")  // the label of the entry
                    .setContentText("A Torque PID Plugin by author X is being used");  // the contents of the entry

            // We get notification channels on newer platforms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(this, NAME);
                buildr.setChannelId(NAME); // A channel Id for pid plugins
            }

            Notification notification = buildr.build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            return notification;
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private static final void createNotificationChannel(Context context, String name) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(name, "Pid Plugin Notifications", importance);
            channel.setDescription("General PID plugin notifications");

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

        }


    }


}