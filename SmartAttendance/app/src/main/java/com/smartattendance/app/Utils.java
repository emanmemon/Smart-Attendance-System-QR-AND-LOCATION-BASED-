package com.smartattendance.app;

import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.security.SecureRandom;
import java.util.function.Consumer;

public class Utils {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

    public static String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        return sb.toString();
    }

    public static void inputDialog(Context ctx, String title, String hint, Consumer<String> onOk) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setSingleLine(true);
        FrameLayout box = new FrameLayout(ctx);
        int p = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        box.setPadding(p, 8, p, 0);
        box.addView(et);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("OK", (d, w) -> {
                    String s = et.getText().toString().trim();
                    if (!s.isEmpty()) onOk.accept(s);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
