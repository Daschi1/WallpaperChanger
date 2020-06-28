package de.daschi.main;

import de.daschi.core.Core;
import org.simpleyaml.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static TrayIcon trayIcon;
    private static final MenuItem title = new MenuItem("");
    private static final MenuItem ignoreVertical = new MenuItem("");
    private static final MenuItem nextImage = new MenuItem("");

    public static void main(final String[] args) throws IOException {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Core.saveCurrentHourImages();
                Core.setIndex(-1);
                Core.nextImage();
                Core.downloadHourImage(Core.getIndex());
                Core.changeWallpaper(new File("WallpaperChanger\\image.jpg"));
                try {
                    Main.updateSystemTray();
                } catch (final IOException exception) {
                    exception.printStackTrace();
                }
            }
        }, 0, 1000 * 60 * 60);

        if (SystemTray.isSupported()) {
            final SystemTray systemTray = SystemTray.getSystemTray();
            final BufferedImage bufferedImage = ImageIO.read(new File("WallpaperChanger\\image.jpg"));

            final PopupMenu popup = new PopupMenu();
            popup.add(Main.title);
            Main.ignoreVertical.addActionListener(e -> {
                Core.setIgnoreVertical(!Core.isIgnoreVertical());
                try {
                    Main.updateSystemTray();
                } catch (final IOException exception) {
                    exception.printStackTrace();
                }
            });
            popup.add(Main.ignoreVertical);
            Main.nextImage.addActionListener(e -> Core.nextImage());
            popup.add(Main.nextImage);
            final MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> System.exit(0));
            popup.add(exit);

            Main.trayIcon = new TrayIcon(bufferedImage, "WallpaperChanger", popup);
            Main.trayIcon.setImageAutoSize(true);
            try {
                systemTray.add(Main.trayIcon);
            } catch (final AWTException exception) {
                exception.printStackTrace();
            }
        }

        Main.updateSystemTray();
    }

    public static void updateSystemTray() throws IOException {
        final String saveUrl = Core.getHourSaveUrl();
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new File("WallpaperChanger\\config.yml"));
        Main.trayIcon.setImage(ImageIO.read(new File("WallpaperChanger\\image.jpg")));
        Main.title.setLabel(yamlConfiguration.getString(saveUrl + Core.getIndex() + ".name"));
        Main.ignoreVertical.setLabel(Core.isIgnoreVertical() ? "Ignore vertical - on" : "Ignore vertical - off");
        Main.nextImage.setLabel("Next image (Currently number " + Core.getIndex() + ")");
    }
}
