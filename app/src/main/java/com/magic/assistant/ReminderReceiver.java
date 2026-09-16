package com.magic.assistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderMessage = intent.getStringExtra("EXTRA_TITLE");
        if (reminderMessage == null) {
            reminderMessage = "Reminder Alert!";
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "magic_reminders";

        NotificationChannel channel = new NotificationChannel(channelId, "Magic Reminders", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
            .setContentTitle("Magic AI")
            .setContentText(reminderMessage)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify((int) System.currentTimeMillis(), notification.build());
    }
}
