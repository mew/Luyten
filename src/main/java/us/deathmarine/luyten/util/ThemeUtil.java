package us.deathmarine.luyten.util;

import com.github.weisj.darklaf.settings.ThemeSettings;
import com.github.weisj.darklaf.theme.*;
import us.deathmarine.luyten.Luyten;

import java.util.HashMap;

public class ThemeUtil {
    public static final HashMap<String, Theme> themeMap = new HashMap<>();

    public static void onThemeChange() {
        try {
            if (Luyten.mainWindowRef.get() != null) {
                ThemeSettings themeSettings = ThemeSettings.getInstance();
                System.out.println("Theme changed to " + themeSettings.getTheme().getDisplayName());
                Luyten.mainWindowRef.get().getModel().updateTheme();
            }
        } catch (Exception ignored) {}
    }

    static {
        for (Theme theme : new Theme[] {
                new DarculaTheme(),
                new HighContrastDarkTheme(),
                new HighContrastLightTheme(),
                new IntelliJTheme(),
                new OneDarkTheme(),
                new SolarizedDarkTheme(),
                new SolarizedLightTheme(),
        }) {
            themeMap.put(theme.getDisplayName(), theme);
        }
    }
}
