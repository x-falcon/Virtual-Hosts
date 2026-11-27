package com.github.xfalcon.vhosts.util;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;

public final class SystemBarInsets {
    private SystemBarInsets() { }

    public static void apply(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT < 30) return;
        activity.getWindow().setDecorFitsSystemWindows(false);
        // Include the ActionBar itself; padding only android.R.id.content leaves it overlapping.
        View root = (View) activity.findViewById(android.R.id.content).getParent();
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top,
                    right + bars.right, bottom + bars.bottom);
            return WindowInsets.CONSUMED;
        });
        WindowInsetsController controller = activity.getWindow().getInsetsController();
        if (controller != null) {
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(lightBars, lightBars);
        }
        root.requestApplyInsets();
    }
}
