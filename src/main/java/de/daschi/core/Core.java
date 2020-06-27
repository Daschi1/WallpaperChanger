package de.daschi.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;

public class Core {
    public static void changeWallpaper(final File file) {
        User32.INSTANCE.SystemParametersInfo(0x0014, 0, file.getAbsolutePath(), 1);
    }

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        void SystemParametersInfo(int one, int two, String s, int three);
    }
}
