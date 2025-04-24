package com.dev.ron;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class HistoryActivity extends AppCompatActivity {
    TextView historyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        historyText = findViewById(R.id.historyText);
        loadHistory();
    }

    private void loadHistory() {
        try {
            FileInputStream fis = openFileInput("token_history.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            historyText.setText(sb.toString());
        } catch (Exception e) {
            historyText.setText("No history found.");
        }
    }
}