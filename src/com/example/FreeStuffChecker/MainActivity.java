package com.example.FreeStuffChecker;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.FreeStuffChecker.adapter.ReceivedSMSAuditAdapter;
import com.example.FreeStuffChecker.adapter.SentSMSAuditAdapter;
import com.example.FreeStuffChecker.adapter.impl.NetworkStatusAdapterImpl;
import com.example.FreeStuffChecker.adapter.impl.ReceivedSMSAuditAdapterImpl;
import com.example.FreeStuffChecker.adapter.impl.SentSMSAuditAdapterImpl;
import com.example.FreeStuffChecker.config.InternalSettings;
import com.example.FreeStuffChecker.config.Settings;
import com.example.FreeStuffChecker.listener.CustomSeekBarListener;
import com.example.FreeStuffChecker.model.Layout;
import com.example.FreeStuffChecker.model.ReceivedSMS;
import com.example.FreeStuffChecker.model.SentSMS;
import com.example.FreeStuffChecker.service.SMSBackgroundService;
import com.example.FreeStuffChecker.somecrazyshit.TimesReadableString;

import java.util.*;

public class MainActivity extends Activity implements Alertable{

    private final ReceivedSMSAuditAdapter receivedSMSAuditAdapter = ReceivedSMSAuditAdapterImpl.getInstance();
    private final SentSMSAuditAdapter sentSMSAuditAdapter = SentSMSAuditAdapterImpl.getInstance();
    private final InternalSettings internalSettings = InternalSettings.getInstance();
    private final Settings settings = Settings.getInstance();
    private List<View> allViews = new ArrayList<View>();
    private static TimesReadableString alert = new TimesReadableString(2);

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initialize();
        createUI();
    }

    public static void setAlertText(String text) {
        alert.setText(text);
    }

    @Override
    public void alert() {
        new AlertDialog.Builder(this)
                .setMessage(alert.getText())
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create().show();
    }

    @Override
    public boolean shouldBeAlerted() {
        return alert.getText() != null;
    }

    private void initialize() {
        if (internalSettings.getNetworkConnected() == null) {
            NetworkStatusAdapterImpl.getInstance().checkAndFixNetworkStatus(this);
        }
        if (!isMyServiceRunning(SMSBackgroundService.class)) {
            startService(new Intent(this, SMSBackgroundService.class));
        }
        allViews.add(findViewById(R.id.mainScreen));
        allViews.add(findViewById(R.id.settingsScreen));
    }

    private void createUI() {

        populateSettingsView();
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SMSBackgroundService.getInstance().sendSMSOnButtonClick(view.getContext());
            }
        });

        findViewById(R.id.printSentSMSesButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printSentSMSes();
            }
        });

        findViewById(R.id.printReceivedSMSes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printReceivedSMSes();
            }
        });

        findViewById(R.id.resetLogsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetAllLogs();
            }
        });
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                populateSettingsView();
                changeVisibleLayout(Layout.SETTINGS_SCREEN);
            }
        });

        findViewById(R.id.saveSettingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO save settings in application, think about persisting
                saveSettings();
                changeVisibleLayout(Layout.MAIN_SCREEN);
            }
        });

        ((SeekBar)findViewById(R.id.checkIntervalInput)).setOnSeekBarChangeListener(new CustomSeekBarListener());
        ((SeekBar)findViewById(R.id.minuteAlertThresholdInput)).setOnSeekBarChangeListener(new CustomSeekBarListener());
        ((SeekBar)findViewById(R.id.internetTrafficAlertThresholdInput)).setOnSeekBarChangeListener(new CustomSeekBarListener());
        ((SeekBar)findViewById(R.id.smsCountThresholdInput)).setOnSeekBarChangeListener(new CustomSeekBarListener());

    }

    private void saveSettings() {
        settings.setInterval((long) getValue(R.id.checkIntervalInput));
        settings.setInternetTrafficAlert((long) getValue(R.id.internetTrafficAlertThresholdInput));
        settings.setMinuteAlert((long) getValue(R.id.minuteAlertThresholdInput));
        settings.setSmsCountAlert((long) getValue(R.id.smsCountThresholdInput));
    }

    private int getValue(int checkIntervalInputId) {
        SeekBar seekBar = (SeekBar) findViewById(checkIntervalInputId);
        return seekBar.getProgress();
    }

    private void changeVisibleLayout(Layout layout) {
        switch (layout) {
            case MAIN_SCREEN: {
                setAllLayoutsToGone();
                findViewById(R.id.mainScreen).setVisibility(View.VISIBLE);
                break;
            }
            case SETTINGS_SCREEN: {
                setAllLayoutsToGone();
                findViewById(R.id.settingsScreen).setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void setAllLayoutsToGone() {
        for (View view : allViews) {
            view.setVisibility(View.GONE);
        }
    }

    private void populateSettingsView() {
        ((SeekBar) findViewById(R.id.checkIntervalInput)).setProgress((int) settings.getInterval());
        ((SeekBar) findViewById(R.id.minuteAlertThresholdInput)).setProgress((int) settings.getMinuteAlert());
        ((SeekBar) findViewById(R.id.smsCountThresholdInput)).setProgress((int) settings.getSmsCountAlert());
        ((SeekBar) findViewById(R.id.internetTrafficAlertThresholdInput)).setProgress((int) settings.getInternetTrafficAlert());
    }

    private void printSentSMSes() {
        List<SentSMS> sentSMSes = sentSMSAuditAdapter.findAll(this);
        print(sentSMSes);
    }

    private void printReceivedSMSes() {
        List<ReceivedSMS> receivedSMSes = receivedSMSAuditAdapter.findAll(this);
        print(receivedSMSes);
    }

    private void resetAllLogs() {
        receivedSMSAuditAdapter.clearAll(this);
        sentSMSAuditAdapter.clearAll(this);
    }


    private void print(List<? extends Comparable> list) {
        Collections.sort(list);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.table);
        tableLayout.removeAllViews();
        for (Object sentSMS : list) {
            TextView textView = new TextView(this);
            textView.setText(sentSMS.toString());
            textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(textView);
            tableLayout.addView(tableRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static class MyRunnable implements Runnable{

        private Alertable alertable;

        private MyRunnable(Alertable alertable) {
            this.alertable=alertable;
        }

        @Override
        public void run() {
            while (true){
                if (alertable.shouldBeAlerted()){
                    alertable.alert();
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    //TODO LOGGER
                    return;
                }
            }
        }
    }
}
