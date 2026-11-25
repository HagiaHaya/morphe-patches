package app.template.extension.jakdojade;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import java.util.Collections;

public final class GameShortcut {
    private static final String ID = "morphe-mini-game";
    private static final String GAME_ACTIVITY = "com.citynav.jakdojade.pl.android.common.ui.GameActivity";

    private GameShortcut() {}

    public static void publish(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> addShortcut(app), "JakdojadeGameShortcut").start();
    }

    private static synchronized void addShortcut(Context app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        ShortcutManager manager = app.getSystemService(ShortcutManager.class);
        if (manager == null) return;

        try {
            for (ShortcutInfo shortcut : manager.getDynamicShortcuts()) {
                if (ID.equals(shortcut.getId())) return;
            }

            boolean polish = "pl".equals(app.getResources().getConfiguration().getLocales().get(0).getLanguage());
            Intent intent = new Intent(Intent.ACTION_VIEW).setClassName(app, GAME_ACTIVITY);
            ShortcutInfo shortcut = new ShortcutInfo.Builder(app, ID)
                    .setShortLabel(polish ? "Minigra" : "Mini-game")
                    .setIcon(Icon.createWithResource(app, app.getApplicationInfo().icon))
                    .setIntent(intent)
                    .build();
            manager.addDynamicShortcuts(Collections.singletonList(shortcut));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.w("JakdojadeGameShortcut", "Could not add the mini-game shortcut", e);
        }
    }
}
