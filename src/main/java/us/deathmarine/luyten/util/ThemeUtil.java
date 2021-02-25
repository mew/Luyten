package us.deathmarine.luyten.util;

import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.settings.ThemeSettings;
import com.github.weisj.darklaf.theme.*;
import org.objectweb.asm.tree.ClassNode;
import us.deathmarine.luyten.Luyten;

import javax.swing.*;
import java.util.HashMap;

public class ThemeUtil {
    public static final HashMap<String, Theme> THEME_MAP = new HashMap<>();
    private static final IconLoader ICON_LOADER = IconLoader.get(Luyten.class);

    public static final Icon PACKAGE_ICON = ICON_LOADER.getIcon("package.svg", true);

    /* Java Icons */
    private static final Icon ABSTRACT_ICON = ICON_LOADER.getIcon("abstractClass.svg");
    private static final Icon ANNOTATION_ICON = ICON_LOADER.getIcon("annotationType.svg");
    private static final Icon CLASS_ICON = ICON_LOADER.getIcon("class.svg");
    private static final Icon ENUM_ICON = ICON_LOADER.getIcon("enum.svg");
    private static final Icon INTERFACE_ICON = ICON_LOADER.getIcon("interface.svg");
    private static final Icon MAIN_CLASS_ICON = ICON_LOADER.getIcon("mainClass.svg");

    /* Kotlin Icons */
    public static final Icon KOTLIN_FILE_ICON = ICON_LOADER.getIcon("kotlin_file.svg");
    private static final Icon KOTLIN_CLASS_ICON = ICON_LOADER.getIcon("classKotlin.svg");
    private static final Icon KOTLIN_ABSTRACT_ICON = ICON_LOADER.getIcon("abstractClassKotlin.svg");
    private static final Icon KOTLIN_ANNOTATION_ICON = ICON_LOADER.getIcon("annotationKotlin.svg");
    private static final Icon KOTLIN_ENUM_ICON = ICON_LOADER.getIcon("enumKotlin.svg");
    private static final Icon KOTLIN_INTERFACE_ICON = ICON_LOADER.getIcon("interfaceKotlin.svg");
    private static final Icon KOTLIN_OBJECT_ICON = ICON_LOADER.getIcon("objectKotlin.svg");

    /* Scala Icons */
    private static final Icon SCALA_CLASS_ICON = ICON_LOADER.getIcon("classScala.svg");
    private static final Icon SCALA_ABSTRACT_ICON = ICON_LOADER.getIcon("abstractClassScala.svg");
    private static final Icon SCALA_CLASS_OBJECT_ICON = ICON_LOADER.getIcon("classObjectScala.svg");

    public static void onThemeChange() {
        try {
            if (Luyten.mainWindowRef.get() != null) {
                ThemeSettings themeSettings = ThemeSettings.getInstance();
                System.out.println("Theme changed to " + themeSettings.getTheme().getDisplayName());
                Luyten.mainWindowRef.get().getModel().updateTheme();
            }
        } catch (Exception ignored) {}
    }

    public static Icon getIconFromClassNode(ClassNode cn) {
        int access = cn.access;
        boolean kotlin = false;
        boolean scala = false;
        try {
            if (!cn.visibleAnnotations.isEmpty()) {
                kotlin = cn.visibleAnnotations.stream().anyMatch(it -> it.desc.equals("Lkotlin/Metadata;"));
                scala = cn.visibleAnnotations.stream().anyMatch(it -> it.desc.equals("Lscala/reflect/ScalaSignature;"));
            }
        } catch (Exception ignored) { }
        if (kotlin && cn.name.contains("$Companion")) {
            return KOTLIN_OBJECT_ICON;
        } else if (cn.name.endsWith("$")) {
            // should be only scala objects
            return SCALA_CLASS_OBJECT_ICON;
        } else if (AccessHelper.isEnum(access)) {
            return kotlin ? KOTLIN_ENUM_ICON : ENUM_ICON;
        } else if (AccessHelper.isAnnotation(access)) {
            return kotlin ? KOTLIN_ANNOTATION_ICON : ANNOTATION_ICON;
        } else if (AccessHelper.isInterface(access)) {
            return kotlin ? KOTLIN_INTERFACE_ICON : INTERFACE_ICON;
        } else {
            if (cn.methods.stream().anyMatch(method ->
                // is main class
                method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
            ) {
                return MAIN_CLASS_ICON;
            } else if (AccessHelper.isAbstract(access)) {
                return kotlin ? KOTLIN_ABSTRACT_ICON : scala ? SCALA_ABSTRACT_ICON : ABSTRACT_ICON;
            } else {
                return kotlin ? KOTLIN_CLASS_ICON : scala ? SCALA_CLASS_ICON : CLASS_ICON;
            }
        }
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
            THEME_MAP.put(theme.getDisplayName(), theme);
        }
    }
}
