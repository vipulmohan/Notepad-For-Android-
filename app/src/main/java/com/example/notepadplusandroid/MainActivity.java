package com.example.notepadplusandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private SelectionAwareEditText editor;
    private TextView lineNumbers;
    private TextView statusBar;
    private LinearLayout tabs;

    private boolean dark = true;
    private boolean internalChange = false;

    private final ArrayList<Doc> docs = new ArrayList<>();
    private int current = -1;

    private static final int OPEN = 1001;
    private static final int SAVE = 1002;

    private static class Doc {
        String title = "new";
        String text = "";
        Uri uri;
        boolean dirty;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editor = findViewById(R.id.editor);
        lineNumbers = findViewById(R.id.lineNumbers);
        statusBar = findViewById(R.id.statusBar);
        tabs = findViewById(R.id.tabs);

        findViewById(R.id.newButton).setOnClickListener(v -> newDocument());
        findViewById(R.id.openButton).setOnClickListener(v -> openDocument());
        findViewById(R.id.saveButton).setOnClickListener(v -> saveDocument());
        findViewById(R.id.saveAsButton).setOnClickListener(v -> saveAs());
        findViewById(R.id.findButton).setOnClickListener(v -> showFind());
        findViewById(R.id.themeButton).setOnClickListener(v -> toggleTheme());

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!internalChange && current >= 0) {
                    docs.get(current).text = s.toString();
                    docs.get(current).dirty = true;
                }
                updateLineNumbers();
                updateStatus();
                refreshTabs();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        editor.setOnSelectionChangedListener((start, end) -> updateStatus());

        newDocument();
    }

    private void newDocument() {
        Doc d = new Doc();
        d.title = "new " + (docs.size() + 1);
        docs.add(d);
        current = docs.size() - 1;
        loadCurrent();
    }

    private void loadCurrent() {
        if (current < 0 || current >= docs.size()) return;

        internalChange = true;
        editor.setText(docs.get(current).text);
        editor.setSelection(editor.length());
        internalChange = false;

        refreshTabs();
        updateLineNumbers();
        updateStatus();
        editor.requestFocus();
    }

    private void refreshTabs() {
        tabs.removeAllViews();

        for (int i = 0; i < docs.size(); i++) {
            final int index = i;
            Button button = new Button(this);
            button.setText(docs.get(i).title + (docs.get(i).dirty ? " •" : ""));
            button.setAllCaps(false);
            button.setTextSize(12);
            button.setSingleLine(true);
            button.setTextColor(Color.WHITE);
            button.setBackgroundColor(
                    index == current
                            ? Color.rgb(52, 56, 65)
                            : Color.rgb(34, 36, 42)
            );

            button.setOnClickListener(v -> {
                current = index;
                loadCurrent();
            });

            button.setOnLongClickListener(v -> {
                closeDocument(index);
                return true;
            });

            tabs.addView(button, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));
        }
    }

    private void closeDocument(int index) {
        if (!docs.get(index).dirty) {
            reallyClose(index);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage("Save changes before closing?")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Discard", (dialog, which) -> reallyClose(index))
                .setPositiveButton("Save", (dialog, which) -> {
                    current = index;
                    saveDocument();
                })
                .show();
    }

    private void reallyClose(int index) {
        docs.remove(index);

        if (docs.isEmpty()) {
            newDocument();
            return;
        }

        current = Math.min(current, docs.size() - 1);
        loadCurrent();
    }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN);
    }

    private void saveDocument() {
        if (current < 0) return;

        if (docs.get(current).uri == null) {
            saveAs();
        } else {
            writeUri(docs.get(current).uri);
        }
    }

    private void saveAs() {
        if (current < 0) return;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, docs.get(current).title);
        startActivityForResult(intent, SAVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();

        try {
            if (requestCode == OPEN) {
                Doc d = new Doc();
                d.text = readUri(uri);

                String name = uri.getLastPathSegment();
                if (name != null && name.contains("/")) {
                    name = name.substring(name.lastIndexOf('/') + 1);
                }

                d.title = name == null || name.isEmpty() ? "opened file" : name;
                d.uri = uri;
                d.dirty = false;

                docs.add(d);
                current = docs.size() - 1;
                loadCurrent();
            } else if (requestCode == SAVE) {
                docs.get(current).uri = uri;

                String name = uri.getLastPathSegment();
                if (name != null && name.contains("/")) {
                    name = name.substring(name.lastIndexOf('/') + 1);
                }

                if (name != null && !name.isEmpty()) {
                    docs.get(current).title = name;
                }

                writeUri(uri);
            }
        } catch (Exception e) {
            toast("File error: " + e.getMessage());
        }
    }

    private String readUri(Uri uri) throws Exception {
        try (
                InputStream input = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            if (input == null) throw new IllegalStateException("Unable to open file");

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void writeUri(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new IllegalStateException("Unable to save file");

            output.write(docs.get(current).text.getBytes(StandardCharsets.UTF_8));
            docs.get(current).dirty = false;

            refreshTabs();
            updateStatus();
            toast("Saved");
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage());
        }
    }

    private void showFind() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(30, 0, 30, 0);

        EditText find = new EditText(this);
        find.setHint("Find");
        box.addView(find);

        EditText replace = new EditText(this);
        replace.setHint("Replace");
        box.addView(replace);

        new AlertDialog.Builder(this)
                .setTitle("Find & Replace")
                .setView(box)
                .setNegativeButton("Close", null)
                .setNeutralButton("Replace All",
                        (dialog, which) ->
                                replaceAll(find.getText().toString(), replace.getText().toString()))
                .setPositiveButton("Find Next",
                        (dialog, which) -> findNext(find.getText().toString()))
                .show();
    }

    private void findNext(String query) {
        if (query == null || query.isEmpty()) return;

        String text = editor.getText().toString();
        int start = Math.max(0, editor.getSelectionEnd());
        int position = text.indexOf(query, start);

        if (position < 0) position = text.indexOf(query);

        if (position >= 0) {
            editor.requestFocus();
            editor.setSelection(position, position + query.length());
        } else {
            toast("Not found");
        }
    }

    private void replaceAll(String query, String replacement) {
        if (query == null || query.isEmpty()) return;

        String result = editor.getText().toString().replace(query, replacement);
        editor.setText(result);
        toast("Replaced");
    }

    private void updateLineNumbers() {
        String text = editor.getText().toString();
        int count = 1;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }

        StringBuilder numbers = new StringBuilder();

        for (int i = 1; i <= count; i++) {
            numbers.append(i).append('\n');
        }

        lineNumbers.setText(numbers.toString());
    }

    private void updateStatus() {
        int position = Math.max(0, editor.getSelectionStart());
        String text = editor.getText().toString();

        int line = 1;
        int column = 1;

        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        statusBar.setText(
                "Ln " + line + ", Col " + column +
                        " • " + language() + " • UTF-8"
        );
    }

    private String language() {
        if (current < 0) return "Plain Text";

        String name = docs.get(current).title.toLowerCase(Locale.ROOT);

        if (name.endsWith(".java")) return "Java";
        if (name.endsWith(".c") || name.endsWith(".h")
                || name.endsWith(".cpp") || name.endsWith(".hpp")) return "C/C++";
        if (name.endsWith(".py")) return "Python";
        if (name.endsWith(".js")) return "JavaScript";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "HTML";
        if (name.endsWith(".css")) return "CSS";
        if (name.endsWith(".xml")) return "XML";
        if (name.endsWith(".json")) return "JSON";
        if (name.endsWith(".md")) return "Markdown";

        return "Plain Text";
    }

    private void toggleTheme() {
        dark = !dark;

        int background = dark
                ? Color.rgb(16, 17, 20)
                : Color.WHITE;

        int foreground = dark
                ? Color.rgb(232, 234, 240)
                : Color.rgb(30, 30, 30);

        editor.setBackgroundColor(background);
        editor.setTextColor(foreground);

        toast(dark ? "Dark mode" : "Light mode");
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
