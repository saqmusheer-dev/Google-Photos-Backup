package com.limra.googlephotosbackup;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 4101;
    private static final int PICK_FOLDER = 4102;
    private final int ink = Color.rgb(31, 35, 61);
    private final int muted = Color.rgb(111, 116, 143);
    private final int purple = Color.rgb(91, 92, 226);
    private LinearLayout root;
    private TextView status, stats, storagePath;
    private ProgressBar progress;
    private Button importButton;
    private int imported = 0, skipped = 0;
    private volatile boolean importing = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247,247,252));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void gap(LinearLayout parent, int height) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private GradientDrawableBuilder bg(int color, int radius) { return new GradientDrawableBuilder(color, dp(radius)); }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(28)); root.setBackgroundColor(Color.rgb(247,247,252));
        scroll.addView(root); setContentView(scroll);

        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("▣", 30, Color.WHITE, true); logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(purple, 18).drawable()); brand.addView(logo, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(12),0,0,0);
        names.addView(text("Photos Backup", 21, ink, true));
        names.addView(text("Your memories. Your control.", 12, muted, false)); brand.addView(names);
        root.addView(brand); gap(root, 26);

        TextView heading = text("Make room for\nnew memories.", 30, ink, true);
        heading.setLineSpacing(dp(2), 1f); root.addView(heading); gap(root, 8);
        root.addView(text("Import your Google Photos Takeout export, keep a local copy, and organize it by date.", 15, muted, false));
        gap(root, 20);

        LinearLayout card = card();
        card.addView(text("FULL-LIBRARY BACKUP", 11, purple, true)); gap(card, 10);
        card.addView(text("Bring your Takeout archive home", 19, ink, true)); gap(card, 7);
        card.addView(text("Export your library from Google Takeout, then select each ZIP file here. Photos and videos are extracted directly into your phone's Pictures folder.", 14, muted, false));
        gap(card, 16);
        Button takeout = button("1   Open Google Takeout", true);
        takeout.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://takeout.google.com/")); startActivity(i); });
        card.addView(takeout, full()); gap(card, 10);
        importButton = button("2   Import Takeout ZIP", true);
        importButton.setOnClickListener(v -> chooseZip()); card.addView(importButton, full());
        gap(card, 10);
        Button folder = button("Choose extracted Takeout folder", false);
        folder.setOnClickListener(v -> chooseFolder()); card.addView(folder, full());
        root.addView(card); gap(root, 18);

        LinearLayout progressCard = card();
        progressCard.addView(text("BACKUP STATUS", 11, purple, true)); gap(progressCard, 10);
        status = text("Ready when you are", 16, ink, true); progressCard.addView(status); gap(progressCard, 12);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgress(0); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(purple));
        progressCard.addView(progress, full()); gap(progressCard, 12);
        stats = text("0 saved  •  0 skipped", 13, muted, false); progressCard.addView(stats);
        root.addView(progressCard); gap(root, 18);

        LinearLayout locationCard = card();
        locationCard.addView(text("YOUR LOCAL ARCHIVE", 11, purple, true)); gap(locationCard, 8);
        locationCard.addView(text("Pictures / GooglePhotosBackup / Year / Month", 15, ink, true)); gap(locationCard, 6);
        storagePath = text("Saved to shared phone storage so you can find your files in a file manager or gallery.", 13, muted, false);
        locationCard.addView(storagePath); root.addView(locationCard); gap(root, 18);

        TextView privacy = text("Privacy first: no Google password is collected. This app does not upload your media or delete anything from your Google account. Check your local files before removing cloud copies.", 12, muted, false);
        privacy.setPadding(dp(3),0,dp(3),0); root.addView(privacy);
    }

    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18),dp(18),dp(18),dp(18)); box.setBackground(bg(Color.WHITE,22).drawable());
        box.setElevation(dp(1)); return box;
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setMinHeight(dp(50));
        b.setTextColor(primary ? Color.WHITE : purple);
        b.setBackground(bg(primary ? purple : Color.rgb(242,242,255), 14).drawable());
        b.setPadding(dp(12),dp(8),dp(12),dp(8)); return b;
    }
    private void chooseZip() {
        if (importing) return;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/zip");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(i, PICK_ZIP);
    }
    private void chooseFolder() {
        if (importing) return;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(i, PICK_FOLDER);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_ZIP) {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
            new Thread(() -> importZip(uri)).start();
        } else if (requestCode == PICK_FOLDER) {
            status.setText("Folder selected. Folder import is not yet available in this starter build; please select the Takeout ZIP archive.");
            Toast.makeText(this,"Please select the Takeout ZIP archive for now.",Toast.LENGTH_LONG).show();
        }
    }

    private void importZip(Uri uri) {
        importing = true; imported = 0; skipped = 0;
        runOnUiThread(() -> { importButton.setEnabled(false); progress.setIndeterminate(true); status.setText("Reading Takeout archive…"); stats.setText("Preparing import"); });
        int savedNow = 0, skippedNow = 0;
        Set<String> namesInThisArchive = new HashSet<>();
        try (InputStream raw = getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
            if (raw == null) throw new Exception("Unable to open selected file.");
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) { zip.closeEntry(); continue; }
                String entryName = entry.getName();
                String lower = entryName.toLowerCase(Locale.ROOT);
                boolean video = lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".3gp") || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mkv");
                boolean image = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".heic") || lower.endsWith(".heif") || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".tif") || lower.endsWith(".tiff") || lower.endsWith(".dng") || lower.endsWith(".raw");
                if (!image && !video) { skippedNow++; zip.closeEntry(); updateProgress(savedNow, skippedNow, entryName); continue; }
                String base = entryName.substring(entryName.lastIndexOf('/') + 1);
                base = base.replaceAll("[^a-zA-Z0-9._() -]", "_");
                if (base.trim().isEmpty()) { skippedNow++; zip.closeEntry(); continue; }
                String unique = base; int suffix = 2;
                while (!namesInThisArchive.add(unique.toLowerCase(Locale.ROOT))) {
                    int dot = base.lastIndexOf('.');
                    unique = dot > 0 ? base.substring(0,dot) + " (" + suffix++ + ")" + base.substring(dot) : base + " (" + suffix++ + ")";
                }
                String datePath = dateFolder(entryName, entry.getTime());
                Uri target = null;
                OutputStream out = null;
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, unique);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(unique, video));
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GooglePhotosBackup/" + datePath);
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    target = getContentResolver().insert(collection, values);
                    if (target == null) throw new Exception("Could not create destination file");
                    out = getContentResolver().openOutputStream(target, "w");
                    if (out == null) throw new Exception("Could not write destination file");
                    int n;
                    while ((n = zip.read(buffer)) > 0) out.write(buffer,0,n);
                    out.flush(); out.close(); out = null;
                    ContentValues ready = new ContentValues(); ready.put(MediaStore.MediaColumns.IS_PENDING,0);
                    getContentResolver().update(target,ready,null,null);
                    savedNow++;
                } catch (Exception fileError) {
                    if (out != null) try { out.close(); } catch (Exception ignored) {}
                    if (target != null) getContentResolver().delete(target,null,null);
                    skippedNow++;
                } finally {
                    zip.closeEntry();
                }
                updateProgress(savedNow, skippedNow, unique);
            }
            final int done = savedNow, skip = skippedNow;
            runOnUiThread(() -> {
                progress.setIndeterminate(false); progress.setProgress(100);
                status.setText("Import complete"); stats.setText(done + " saved  •  " + skip + " skipped");
                storagePath.setText("Imported media is in Pictures/GooglePhotosBackup, organized by year and month. Keep the original Takeout ZIP until you verify the saved files.");
                Toast.makeText(this,"Import complete: " + done + " media files saved.",Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Could not read this archive." : e.getMessage();
            runOnUiThread(() -> { progress.setIndeterminate(false); status.setText("Import stopped"); storagePath.setText(message + " Check that this is a valid Takeout ZIP and that your phone has enough free space."); });
        } finally {
            importing = false;
            runOnUiThread(() -> importButton.setEnabled(true));
        }
    }

    private void updateProgress(int saved, int skip, String name) {
        runOnUiThread(() -> { stats.setText(saved + " saved  •  " + skip + " skipped"); status.setText("Importing: " + name); });
    }
    private String dateFolder(String name, long zipTime) {
        Pattern p = Pattern.compile("(20\\d{2})[-_]?([01]\\d)[-_]?([0-3]\\d)");
        Matcher m = p.matcher(name);
        if (m.find()) {
            String year = m.group(1), month = m.group(2);
            if (Integer.parseInt(month) >= 1 && Integer.parseInt(month) <= 12) return year + "/" + month;
        }
        long time = zipTime > 0 ? zipTime : System.currentTimeMillis();
        return new SimpleDateFormat("yyyy/MM", Locale.US).format(new Date(time));
    }
    private String mimeFor(String name, boolean video) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".heic")) return "image/heic";
        if (n.endsWith(".heif")) return "image/heif";
        if (n.endsWith(".mp4") || n.endsWith(".m4v")) return "video/mp4";
        if (n.endsWith(".mov")) return "video/quicktime";
        if (n.endsWith(".webm")) return "video/webm";
        if (n.endsWith(".3gp")) return "video/3gpp";
        return video ? "video/*" : "image/jpeg";
    }

    private static class GradientDrawableBuilder {
        private final int color, radius;
        GradientDrawableBuilder(int c, int r) { color=c; radius=r; }
        android.graphics.drawable.GradientDrawable drawable() {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setColor(color); d.setCornerRadius(radius); return d;
        }
    }
}
