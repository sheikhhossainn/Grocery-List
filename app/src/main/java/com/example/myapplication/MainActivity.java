package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText itemNameEditText, itemCostEditText;
    private Button addItemButton, finalizeListButton, saveListButton, showHistoryButton, clearScreenButton, clearSavedHistoryButton;
    private ListView listView;
    private TextView totalCostTextView, historyTextView;

    private ArrayList<String> itemList;
    private ArrayAdapter<String> arrayAdapter;
    private int totalCost = 0;
    private boolean isFinalized = false;
    private ArrayList<String> historyList = new ArrayList<>();
    private int selectedItemPosition = -1; // Track the selected item for editing
    private static final String HISTORY_FILE = "history.txt"; // File to store history

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemCostEditText = findViewById(R.id.itemCostEditText);
        addItemButton = findViewById(R.id.addItemButton);
        finalizeListButton = findViewById(R.id.finalizeListButton);
        saveListButton = findViewById(R.id.saveListButton);
        showHistoryButton = findViewById(R.id.showHistoryButton);
        clearScreenButton = findViewById(R.id.clearHomeItemsButton);
        clearSavedHistoryButton = findViewById(R.id.clearHistoryButton);
        listView = findViewById(R.id.listView);
        totalCostTextView = findViewById(R.id.totalCostTextView);
        historyTextView = findViewById(R.id.historyTextView);
        historyTextView.setVisibility(View.GONE);  // Hide history by default

        // Initialize list and adapter
        itemList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listView.setAdapter(arrayAdapter);

        // Load previously saved history
        loadHistoryFromFile();

        // Add item button functionality
        addItemButton.setOnClickListener(v -> {
            String itemName = itemNameEditText.getText().toString();
            String itemCostString = itemCostEditText.getText().toString();

            if (itemName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter an item.", Toast.LENGTH_SHORT).show();
                return;
            }

            int itemCost = itemCostString.isEmpty() ? 0 : Integer.parseInt(itemCostString);

            // Editing mode (if an item was selected)
            if (selectedItemPosition >= 0) {
                String oldItem = itemList.get(selectedItemPosition);
                String[] oldDetails = oldItem.split(" - ");
                totalCost -= Integer.parseInt(oldDetails[1].replace(" TAKA", ""));  // Deduct the old cost

                itemList.set(selectedItemPosition, itemName + " - " + itemCost + " TAKA");  // Update item
                selectedItemPosition = -1;  // Reset selection
                addItemButton.setText("Add Item");  // Reset button text
            } else {
                itemList.add(itemName + " - " + itemCost + " TAKA");  // Add new item
            }

            totalCost += itemCost;  // Update total cost
            totalCostTextView.setText("Total Cost: " + totalCost + " TAKA");
            itemNameEditText.setText("");
            itemCostEditText.setText("");

            arrayAdapter.notifyDataSetChanged();
        });

        // Finalize list button
        finalizeListButton.setOnClickListener(v -> {
            if (itemList.isEmpty()) {
                Toast.makeText(MainActivity.this, "No items to finalize.", Toast.LENGTH_SHORT).show();
                return;
            }

            isFinalized = true;
            saveListButton.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "List finalized. Tap an item to edit.", Toast.LENGTH_SHORT).show();
        });

        // Save list button
        saveListButton.setOnClickListener(v -> {
            if (isFinalized) {
                // Save history to file without showing it immediately
                saveHistoryToFile();
                Toast.makeText(MainActivity.this, "List saved.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Finalize the list first before saving.", Toast.LENGTH_SHORT).show();
            }
        });

        // Show history button
        showHistoryButton.setOnClickListener(v -> {
            if (historyList.isEmpty()) {
                Toast.makeText(MainActivity.this, "No history to show.", Toast.LENGTH_SHORT).show();
            } else {
                // Show history only when "Show History" is clicked
                StringBuilder historyDisplay = new StringBuilder();
                for (String history : historyList) {
                    historyDisplay.append(history).append("\n\n");
                }
                historyTextView.setText(historyDisplay.toString());
                historyTextView.setVisibility(View.VISIBLE);
                historyTextView.setMovementMethod(new ScrollingMovementMethod());
            }
        });

        // Clear home screen items button
        clearScreenButton.setOnClickListener(v -> {
            if (itemList.isEmpty()) {
                Toast.makeText(MainActivity.this, "No items to clear.", Toast.LENGTH_SHORT).show();
            } else {
                itemList.clear();
                arrayAdapter.notifyDataSetChanged();
                totalCost = 0;
                totalCostTextView.setText("Total Cost: 0 TAKA");
                Toast.makeText(MainActivity.this, "Home screen items cleared.", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear saved history button
        clearSavedHistoryButton.setOnClickListener(v -> clearHistoryFile());

        // Handle item selection for editing
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (isFinalized) {
                String selectedItem = itemList.get(position);
                String[] itemDetails = selectedItem.split(" - ");
                itemNameEditText.setText(itemDetails[0]);
                itemCostEditText.setText(itemDetails[1].replace(" TAKA", ""));
                selectedItemPosition = position;
                addItemButton.setText("Update Item");  // Change button text to indicate editing mode
            }
        });
    }

    // Save history to internal storage
    private void saveHistoryToFile() {
        String timestamp = new SimpleDateFormat("dd/MM/yyyy 'Time:' hh:mm a", Locale.getDefault()).format(new Date());
        StringBuilder itemDetails = new StringBuilder();
        for (String item : itemList) {
            itemDetails.append(item).append("\n");
        }
        String historyEntry = "Date: " + timestamp + "\nItems:\n" + itemDetails.toString() + "\nTotal Cost: " + totalCost + " TAKA";

        historyList.add(historyEntry);

        try (FileOutputStream fos = openFileOutput(HISTORY_FILE, MODE_APPEND)) {
            fos.write((historyEntry + "\n\n").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load history from file on app start
    private void loadHistoryFromFile() {
        try (FileInputStream fis = openFileInput(HISTORY_FILE);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            StringBuilder historyContent = new StringBuilder();

            while ((line = br.readLine()) != null) {
                historyContent.append(line).append("\n");
            }

            historyList.add(historyContent.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clear the history file
    private void clearHistoryFile() {
        boolean deleted = deleteFile(HISTORY_FILE);
        historyList.clear();
        historyTextView.setText("");
        historyTextView.setVisibility(View.GONE);

        if (deleted) {
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error clearing history", Toast.LENGTH_SHORT).show();
        }
    }
}
