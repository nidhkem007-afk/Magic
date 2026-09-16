package com.magic.assistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("WrongConstant")
public class MainActivity extends AppCompatActivity {

    // ============================================================
    // MAGIC NATIVE ENGINE
    // ============================================================

    static {
        System.loadLibrary("magic-native");
    }

    private native String nativeGenerate(
            String modelPath,
            String prompt
    );

    // ============================================================
    // UI
    // ============================================================

    private DrawerLayout drawerLayout;

    private LinearLayout chatContainer;
    private ScrollView chatScroll;

    private EditText inputCommand;
    private ImageButton btnSend;
    private ImageButton btnMenu;

    private LinearLayout savedChatsContainer;

    // ============================================================
    // MODEL
    // ============================================================

    private ActivityResultLauncher<String> modelPicker;

    // ============================================================
    // LOCAL CHAT TITLES
    // ============================================================

    private final List<String> savedChats =
            new ArrayList<>();

    // ============================================================
    // MODEL PATH
    // ============================================================

    private String getModelPath() {
        File modelsDir = new File(getFilesDir(), "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        return new File(modelsDir, "Qwen2.5-3B-Instruct-Q4_K_M.gguf").getAbsolutePath();
    }

    // ACTIVITY
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );

        getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        );

        initializeViews();
        initializeModelPicker();
        initializeButtons();
        loadSavedChatTitles();
        requestNotificationPermissionIfNeeded();
    }


    // ============================================================
    // INITIALIZE UI
    // ============================================================

    private void initializeViews() {

        drawerLayout =
                findViewById(
                        R.id.drawerLayout
                );

        chatContainer =
                findViewById(
                        R.id.chatContainer
                );

        chatScroll =
                findViewById(
                        R.id.chatScroll
                );

        inputCommand =
                findViewById(
                        R.id.inputCommand
                );

        btnSend =
                findViewById(
                        R.id.btnSetReminder
                );

        btnMenu =
                findViewById(
                        R.id.btnMenu
                );

        savedChatsContainer =
                findViewById(
                        R.id.savedChatsContainer
                );
    }

    // ============================================================
    // MODEL PICKER
    // ============================================================

    private void initializeModelPicker() {

        modelPicker =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            if (uri != null) {
                                copyModelToPrivateStorage(uri);
                            }

                        }
                );
    }

    // ============================================================
    // BUTTONS
    // ============================================================

    private void initializeButtons() {

        // --------------------------------------------------------
        // SIDEBAR
        // --------------------------------------------------------

        btnMenu.setOnClickListener(
                v -> drawerLayout.openDrawer(
                        Gravity.START
                )
        );

        // --------------------------------------------------------
        // SEND
        // --------------------------------------------------------

        btnSend.setOnClickListener(
                v -> sendMessage()
        );

        // --------------------------------------------------------
        // ENTER
        // --------------------------------------------------------

        inputCommand.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEND) {

                        sendMessage();

                        return true;
                    }

                    return false;
                }
        );

        // --------------------------------------------------------
        // NEW CHAT
        // --------------------------------------------------------

        View newChat =
                findViewById(
                        R.id.btnNewChat
                );

        if (newChat != null) {

            newChat.setOnClickListener(
                    v -> createNewChat()
            );
        }

        // --------------------------------------------------------
        // SEARCH
        // --------------------------------------------------------

        View search =
                findViewById(
                        R.id.btnSearchChats
                );

        if (search != null) {

            search.setOnClickListener(
                    v -> showComingSoon(
                            "Chat search"
                    )
            );
        }

        // --------------------------------------------------------
        // MEMORY
        // --------------------------------------------------------

        View memory =
                findViewById(
                        R.id.btnMemory
                );

        if (memory != null) {

            memory.setOnClickListener(
                    v -> showComingSoon(
                            "Memory system"
                    )
            );
        }

        // --------------------------------------------------------
        // PERSONALITY
        // --------------------------------------------------------

        View personality =
                findViewById(
                        R.id.btnPersonality
                );

        if (personality != null) {

            personality.setOnClickListener(
                    v -> showComingSoon(
                            "Personality editor"
                    )
            );
        }

        // --------------------------------------------------------
        // SYSTEM PROMPT
        // --------------------------------------------------------

        View systemPrompt =
                findViewById(
                        R.id.btnSystemPrompt
                );

        if (systemPrompt != null) {

            systemPrompt.setOnClickListener(
                    v -> showComingSoon(
                            "System Prompt editor"
                    )
            );
        }

        // --------------------------------------------------------
        // SETTINGS
        // --------------------------------------------------------

        View settings =
                findViewById(
                        R.id.btnSettings
                );

        if (settings != null) {

            settings.setOnClickListener(
                    v -> showComingSoon(
                            "Magic Settings"
                    )
            );
        }
    }

    // ============================================================
    // SEND MESSAGE
    // ============================================================

    private void sendMessage() {

        String userText =
                inputCommand
                        .getText()
                        .toString()
                        .trim();

        if (userText.isEmpty()) {
            return;
        }

        File modelFile =
                new File(
                        getModelPath()
                );

        // --------------------------------------------------------
        // MODEL NOT INSTALLED
        // --------------------------------------------------------

        if (!modelFile.exists()) {

            addMagicMessage(
                    "My Qwen brain isn't installed yet. "
                            + "Select the GGUF model and I'll be ready. ✨"
            );

            modelPicker.launch("*/*");

            return;
        }

        // --------------------------------------------------------
        // ADD USER MESSAGE
        // --------------------------------------------------------

        addUserMessage(
                userText
        );

        inputCommand.setText("");

        hideKeyboard();

        // --------------------------------------------------------
        // THINKING BUBBLE
        // --------------------------------------------------------

        final TextView thinkingBubble =
                createMagicMessageOnUi(
                        "Thinking... ✦"
                );

        btnSend.setEnabled(false);

        // --------------------------------------------------------
        // RUN QWEN
        // --------------------------------------------------------

        new Thread(() -> {

            try {

                String result =
                        nativeGenerate(
                                getModelPath(),
                                userText
                        );

                if (result == null) {

                    throw new Exception(
                            "Qwen returned no response."
                    );
                }

                result =
                        result.trim();

                // ------------------------------------------------
                // NATIVE ERROR
                // ------------------------------------------------

                if (isNativeError(result)) {

                    final String error =
                            result;

                    runOnUiThread(() -> {

                        removeMessage(
                                thinkingBubble
                        );

                        addMagicMessage(
                                "⚠️ Qwen error\n\n"
                                        + error
                        );

                        btnSend.setEnabled(true);
                    });

                    return;
                }

                final String response =
                        result;

                // ------------------------------------------------
                // REMINDER
                // ------------------------------------------------

                if (isReminderRequest(userText)) {

                    handleReminderResponse(
                            response,
                            thinkingBubble
                    );

                    return;
                }

                // ------------------------------------------------
                // NORMAL MAGIC RESPONSE
                // ------------------------------------------------

                runOnUiThread(() -> {

                    removeMessage(
                            thinkingBubble
                    );

                    addMagicMessage(
                            response
                    );

                    saveChatTitleIfNeeded(
                            userText
                    );

                    btnSend.setEnabled(true);

                });

            } catch (Exception e) {

                e.printStackTrace();

                final String error =
                        e.toString();

                runOnUiThread(() -> {

                    removeMessage(
                            thinkingBubble
                    );

                    addMagicMessage(
                            "⚠️ Magic encountered an error.\n\n"
                                    + error
                    );

                    btnSend.setEnabled(true);

                });
            }

        }).start();
    }

    // ============================================================
    // NATIVE ERROR DETECTION
    // ============================================================

    private boolean isNativeError(
            String result
    ) {

        return result.startsWith("QWEN ")
                || result.startsWith("Qwen ")
                || result.startsWith("Failed ")
                || result.contains("llama_decode")
                || result.contains("native error");
    }

    // ============================================================
    // USER MESSAGE
    // ============================================================

    private void addUserMessage(
            String message
    ) {

        TextView bubble =
                new TextView(
                        MainActivity.this
                );

        bubble.setText(
                message
        );

        bubble.setTextColor(
                Color.WHITE
        );

        bubble.setTextSize(
                16
        );

        bubble.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bubble.setPadding(
                18,
                13,
                18,
                13
        );

        bubble.setBackgroundColor(
                Color.rgb(
                        101,
                        43,
                        190
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        params.gravity =
                Gravity.END;

        params.setMargins(
                55,
                7,
                0,
                7
        );

        bubble.setLayoutParams(
                params
        );

        chatContainer.addView(
                bubble
        );

        scrollToBottom();
    }

    // ============================================================
    // MAGIC MESSAGE
    // ============================================================

    private TextView addMagicMessage(
            String message
    ) {

        if (android.os.Looper.myLooper()
                == android.os.Looper.getMainLooper()) {

            return createMagicMessageOnUi(
                    message
            );
        }

        final TextView[] holder =
                new TextView[1];

        runOnUiThread(() -> {

            holder[0] =
                    createMagicMessageOnUi(
                            message
                    );

        });

        return holder[0];
    }

    // ============================================================
    // MAGIC MESSAGE — MUST RUN ON UI THREAD
    // ============================================================

    private TextView createMagicMessageOnUi(
            String message
    ) {

        TextView bubble =
                new TextView(
                        MainActivity.this
                );

        bubble.setText(
                "✦  MAGIC\n\n"
                        + message
        );

        bubble.setTextColor(
                Color.rgb(
                        235,
                        224,
                        247
                )
        );

        bubble.setTextSize(
                16
        );

        bubble.setPadding(
                18,
                15,
                18,
                15
        );

        bubble.setBackgroundColor(
                Color.rgb(
                        25,
                        15,
                        38
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        params.gravity =
                Gravity.START;

        params.setMargins(
                0,
                7,
                35,
                7
        );

        bubble.setLayoutParams(
                params
        );

        chatContainer.addView(
                bubble
        );

        scrollToBottom();

        return bubble;
    }

    // ============================================================
    // REMOVE MESSAGE
    // ============================================================

    private void removeMessage(
            TextView message
    ) {

        if (message == null) {
            return;
        }

        if (android.os.Looper.myLooper()
                == android.os.Looper.getMainLooper()) {

            chatContainer.removeView(
                    message
            );

        } else {

            runOnUiThread(() ->
                    chatContainer.removeView(
                            message
                    )
            );
        }
    }

    // ============================================================
    // SCROLL
    // ============================================================

    private void scrollToBottom() {

        chatScroll.post(() ->
                chatScroll.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    // ============================================================
    // HIDE KEYBOARD
    // ============================================================

    private void hideKeyboard() {

        InputMethodManager manager =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );

        if (manager != null) {

            manager.hideSoftInputFromWindow(
                    inputCommand.getWindowToken(),
                    0
            );
        }

        inputCommand.clearFocus();
    }

    // ============================================================
    // REMINDER DETECTION
    // ============================================================

    private boolean isReminderRequest(
            String text
    ) {

        String lower =
                text.toLowerCase().trim();

        return lower.contains(
                        "remind me"
                )
                || lower.contains(
                        "set a reminder"
                )
                || lower.contains(
                        "create a reminder"
                )
                || lower.contains(
                        "make a reminder"
                )
                || lower.contains(
                        "schedule a reminder"
                )
                || lower.contains(
                        "reminder for"
                )
                || lower.contains(
                        "reminder in"
                )
                || lower.startsWith(
                        "remind "
                );
    }

    // ============================================================
    // REMINDER RESPONSE
    // ============================================================

    private void handleReminderResponse(
            String response,
            TextView thinkingBubble
    ) {

        try {

            String cleanResult =
                    response
                            .replace(
                                    "```json",
                                    ""
                            )
                            .replace(
                                    "```",
                                    ""
                            )
                            .trim();

            int start =
                    cleanResult.indexOf(
                            "{"
                    );

            int end =
                    cleanResult.lastIndexOf(
                            "}"
                    );

            if (start == -1
                    || end == -1
                    || end <= start) {

                final String errorResponse =
                        cleanResult;

                runOnUiThread(() -> {

                    removeMessage(
                            thinkingBubble
                    );

                    addMagicMessage(
                            "I understood that as a reminder, "
                                    + "but I couldn't read the reminder details.\n\n"
                                    + errorResponse
                    );

                    btnSend.setEnabled(true);

                });

                return;
            }

            String jsonText =
                    cleanResult.substring(
                            start,
                            end + 1
                    );

            JSONObject json =
                    new JSONObject(
                            jsonText
                    );

            String title =
                    json.optString(
                            "title",
                            "Magic Reminder"
                    );

            long seconds =
                    json.optLong(
                            "seconds",
                            1
                    );

            if (seconds < 1) {
                seconds = 1;
            }

            final String reminderTitle =
                    title;

            final long reminderSeconds =
                    seconds;

            runOnUiThread(() -> {

                removeMessage(
                        thinkingBubble
                );

                boolean scheduled =
                        scheduleSafeAlarm(
                                reminderTitle,
                                reminderSeconds
                        );

                if (scheduled) {

                    addMagicMessage(
                            "⏰  REMINDER CREATED\n\n"
                                    + reminderTitle
                                    + "\n\n"
                                    + "I'll remind you in "
                                    + formatDuration(
                                            reminderSeconds
                                    )
                                    + "."
                    );

                }

                btnSend.setEnabled(true);

            });

        } catch (Exception e) {

            e.printStackTrace();

            final String error =
                    e.toString();

            runOnUiThread(() -> {

                removeMessage(
                        thinkingBubble
                );

                addMagicMessage(
                        "⚠️ I couldn't create that reminder.\n\n"
                                + error
                );

                btnSend.setEnabled(true);

            });
        }
    }

    // ============================================================
    // FORMAT REMINDER TIME
    // ============================================================

    private String formatDuration(
            long seconds
    ) {

        if (seconds < 60) {

            return seconds
                    + (seconds == 1
                    ? " second"
                    : " seconds");
        }

        long minutes =
                seconds / 60;

        if (minutes < 60) {

            return minutes
                    + (minutes == 1
                    ? " minute"
                    : " minutes");
        }

        long hours =
                minutes / 60;

        long remainingMinutes =
                minutes % 60;

        if (remainingMinutes == 0) {

            return hours
                    + (hours == 1
                    ? " hour"
                    : " hours");
        }

        return hours
                + (hours == 1
                ? " hour "
                : " hours ")
                + remainingMinutes
                + " minutes";
    }

    // ============================================================
    // REAL ANDROID ALARM
    // ============================================================

    private boolean scheduleSafeAlarm(
            String title,
            long seconds
    ) {

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                Context.ALARM_SERVICE
                        );

                if (alarmManager == null) {

            addMagicMessage(
                    "⚠️ Android's alarm service isn't available."
            );

            return false;
        }

        // ========================================================
        // ANDROID 12+ EXACT ALARM PERMISSION
        // ========================================================

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {

                try {

                    Intent settingsIntent =
                            new Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            );

                    settingsIntent.setData(
                            Uri.parse(
                                    "package:" +
                                            getPackageName()
                            )
                    );

                    startActivity(
                            settingsIntent
                    );

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Please enable exact alarms in Android Settings.",
                            Toast.LENGTH_LONG
                    ).show();
                }

                addMagicMessage(
                        "I need permission to schedule exact alarms. "
                                + "I've opened the Android alarm settings."
                );

                return false;
            }
        }

        // ========================================================
        // REMINDER INTENT
        // ========================================================

        Intent intent =
                new Intent(
                        MainActivity.this,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "title",
                title
        );

        // ========================================================
        // UNIQUE REQUEST CODE
        // ========================================================

        int requestCode =
                (int)
                        (
                                System.currentTimeMillis()
                                        & 0x7fffffff
                        );

        // ========================================================
        // PENDING INTENT
        // ========================================================

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        MainActivity.this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        // ========================================================
        // CALCULATE TRIGGER TIME
        // ========================================================

        long triggerTime =
                System.currentTimeMillis()
                        + (seconds * 1000L);

        // ========================================================
        // SCHEDULE REAL ANDROID ALARM
        // ========================================================

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

        } else {

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }

        return true;
    }

    // ============================================================
    // MODEL INSTALLATION
    // ============================================================

    private void copyModelToPrivateStorage(
            Uri uri
    ) {

        new Thread(() -> {

            try {

                File destination =
                        new File(
                                getModelPath()
                        );

                File parent =
                        destination.getParentFile();

                if (parent != null
                        && !parent.exists()) {

                    parent.mkdirs();
                }

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Installing Qwen... This may take several minutes.",
                                Toast.LENGTH_LONG
                        ).show()
                );

                InputStream input =
                        getContentResolver()
                                .openInputStream(uri);

                if (input == null) {

                    throw new Exception(
                            "Could not open selected file."
                    );
                }

                FileOutputStream output =
                        new FileOutputStream(
                                destination
                        );

                byte[] buffer =
                        new byte[
                                1024 * 1024
                        ];

                int bytesRead;

                while (
                        (bytesRead =
                                input.read(buffer))
                                != -1
                ) {

                    output.write(
                            buffer,
                            0,
                            bytesRead
                    );
                }

                output.flush();
                output.close();
                input.close();

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Qwen installed successfully! ✨",
                            Toast.LENGTH_LONG
                    ).show();

                    addMagicMessage(
                            "My Qwen brain is ready. "
                                    + "Let's create some magic. ✨"
                    );
                });

            } catch (Exception e) {

                e.printStackTrace();

                final String error =
                        e.toString();

                runOnUiThread(() ->
                        addMagicMessage(
                                "⚠️ Model installation failed:\n\n"
                                        + error
                        )
                );
            }

        }).start();
    }

    // ============================================================
    // NEW CHAT
    // ============================================================

    private void createNewChat() {

        chatContainer.removeAllViews();

        TextView welcome =
                new TextView(
                        MainActivity.this
                );

        welcome.setText(
                "✦  MAGIC\n\n"
                        + "New conversation ready.\n\n"
                        + "What are we going to create?"
        );

        welcome.setTextColor(
                Color.rgb(
                        235,
                        224,
                        247
                )
        );

        welcome.setTextSize(
                18
        );

        welcome.setPadding(
                22,
                25,
                22,
                25
        );

        welcome.setGravity(
                Gravity.CENTER
        );

        chatContainer.addView(
                welcome
        );

        inputCommand.setText("");

        drawerLayout.closeDrawer(
                Gravity.START
        );

        inputCommand.requestFocus();

        scrollToBottom();
    }

    // ============================================================
    // SAVE CHAT TITLE
    // ============================================================

    private void saveChatTitleIfNeeded(
            String userText
    ) {

        String title =
                userText.trim();

        if (title.length() > 32) {

            title =
                    title.substring(
                            0,
                            32
                    ) + "…";
        }

        if (!savedChats.contains(title)) {

            savedChats.add(
                    0,
                    title
            );

            if (savedChats.size() > 20) {

                savedChats.remove(
                        savedChats.size() - 1
                );
            }

            getSharedPreferences(
                    "magic_chats",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            "titles",
                            joinTitles()
                    )
                    .apply();

            refreshSavedChats();
        }
    }

    // ============================================================
    // JOIN CHAT TITLES
    // ============================================================

    private String joinTitles() {

        StringBuilder builder =
                new StringBuilder();

        for (String title : savedChats) {

            if (builder.length() > 0) {

                builder.append(
                        "\n"
                );
            }

            builder.append(
                    title.replace(
                            "\n",
                            " "
                    )
            );
        }

        return builder.toString();
    }

    // ============================================================
    // LOAD CHAT TITLES
    // ============================================================

    private void loadSavedChatTitles() {

        String data =
                getSharedPreferences(
                        "magic_chats",
                        MODE_PRIVATE
                )
                        .getString(
                                "titles",
                                ""
                        );

        savedChats.clear();

        if (!data.isEmpty()) {

            String[] titles =
                    data.split(
                            "\n"
                    );

            for (String title : titles) {

                if (!title.trim().isEmpty()) {

                    savedChats.add(
                            title
                    );
                }
            }
        }

        refreshSavedChats();
    }

    // ============================================================
    // REFRESH SAVED CHATS
    // ============================================================

    private void refreshSavedChats() {

        if (savedChatsContainer == null) {
            return;
        }

        savedChatsContainer.removeAllViews();

        if (savedChats.isEmpty()) {

            TextView empty =
                    new TextView(
                            MainActivity.this
                    );

            empty.setText(
                    "○   No conversations yet"
            );

            empty.setTextColor(
                    Color.rgb(
                            120,
                            105,
                            140
                    )
            );

            empty.setTextSize(
                    14
            );

            empty.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            empty.setPadding(
                    14,
                    0,
                    8,
                    0
            );

            savedChatsContainer.addView(
                    empty
            );

            return;
        }

        for (String title : savedChats) {

            TextView item =
                    new TextView(
                            MainActivity.this
                    );

            item.setText(
                    "○   " + title
            );

            item.setTextColor(
                    Color.rgb(
                            201,
                            190,
                            216
                    )
            );

            item.setTextSize(
                    14
            );

            item.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            item.setPadding(
                    14,
                    0,
                    8,
                    0
            );

            item.setMaxLines(
                    2
            );

            savedChatsContainer.addView(
                    item
            );
        }
    }

    // ============================================================
    // COMING SOON
    // ============================================================

    private void showComingSoon(
            String feature
    ) {

        drawerLayout.closeDrawer(
                Gravity.START
        );

        Toast.makeText(
                MainActivity.this,
                feature
                        + " will be added in the next Magic layer. ✦",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ============================================================
    // NOTIFICATION PERMISSION
    // ============================================================

    private void requestNotificationPermissionIfNeeded() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission
                                        .POST_NOTIFICATIONS
                        },
                        1001
                );
            }
        }
    }
}