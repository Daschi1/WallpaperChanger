package de.daschi.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import de.daschi.main.Main;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.references.SubredditReference;
import org.simpleyaml.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

public class Core {

    private static final RedditClient redditClient;

    static {
        final UserAgent userAgent = new UserAgent("Windows", "de.daschi.wallpaperchanger", "v1.0.0a", "Daschi1");
        final Credentials credentials = Core.getCredentials();
        if (credentials == null) {
            System.exit(0);
        }
        final NetworkAdapter networkAdapter = new OkHttpNetworkAdapter(userAgent);
        redditClient = OAuthHelper.automatic(networkAdapter, credentials);
    }

    public static void changeWallpaper(final File file) {
        User32.INSTANCE.SystemParametersInfo(0x0014, 0, file.getAbsolutePath(), 1);
    }

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        void SystemParametersInfo(int one, int two, String s, int three);
    }

    public static Credentials getCredentials() {
        final File directory = new File("WallpaperChanger");
        final File file = new File("WallpaperChanger\\config.yml");
        try {
            if (directory.mkdirs() && file.createNewFile()) {
                final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
                yamlConfiguration.set("username", "");
                yamlConfiguration.set("password", "");
                yamlConfiguration.set("clientId", "");
                yamlConfiguration.set("secret", "");
                yamlConfiguration.set("index", 0);
                yamlConfiguration.set("ignoreHorizontal", true);
                yamlConfiguration.save(file);
            } else {
                final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
                if (file.exists() && yamlConfiguration.contains("username") && yamlConfiguration.contains("password") && yamlConfiguration.contains("clientId") && yamlConfiguration.contains("secret")) {
                    return Credentials.script(yamlConfiguration.getString("username"), yamlConfiguration.getString("password"), yamlConfiguration.getString("clientId"), yamlConfiguration.getString("secret"));
                }
            }
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static void saveCurrentHourImages() {
        final SubredditReference subredditReference = Core.redditClient.subreddit("EarthPorn");
        final DefaultPaginator<Submission> submissionDefaultPaginator = subredditReference.posts().sorting(SubredditSort.TOP).timePeriod(TimePeriod.HOUR).build();
        final List<Submission> submissions = submissionDefaultPaginator.accumulateMerged(3);

        final File file = new File("WallpaperChanger\\config.yml");
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        final String saveUrl = Core.getHourSaveUrl();

        int index = 0;
        for (final Submission submission : submissions) {
            if (submission.getUrl().endsWith(".jpg")) {
                yamlConfiguration.set(saveUrl + index + ".name", submission.getTitle());
                yamlConfiguration.set(saveUrl + index + ".imageUrl", submission.getUrl());
                yamlConfiguration.set(saveUrl + index + ".permanentUrl", "https://www.reddit.com" + submission.getPermalink());
                index++;
            }
        }

        try {
            yamlConfiguration.save(file);
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String getHourSaveUrl() {
        final Calendar calendar = Calendar.getInstance();
        return "images." + calendar.get(Calendar.HOUR_OF_DAY) + "|" + calendar.get(Calendar.DAY_OF_MONTH) + "|" + (calendar.get(Calendar.MONTH) + 1) + "|" + calendar.get(Calendar.YEAR) + ".";
    }

    public static void downloadHourImage(int index) {
        String plainUrl = "";
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new File("WallpaperChanger\\config.yml"));
        final String saveUrl = Core.getHourSaveUrl();

        if (Core.isIgnoreHorizontal()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new URL(yamlConfiguration.getString(saveUrl + index + ".imageUrl")));
                while (!(bufferedImage.getWidth() > bufferedImage.getHeight())) {
                    bufferedImage = ImageIO.read(new URL(yamlConfiguration.getString(saveUrl + ++index + ".imageUrl")));
                }
                plainUrl = yamlConfiguration.getString(saveUrl + index + ".imageUrl");
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        } else {
            plainUrl = yamlConfiguration.getString(saveUrl + index + ".imageUrl");
        }

        Core.downloadImage(plainUrl, "WallpaperChanger\\image.jpg");
    }

    public static void downloadImage(final String plainUrl, final String fileLocation) {
        try {
            final URL url = new URL(plainUrl);
            final InputStream inputStream = new BufferedInputStream(url.openStream());
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final byte[] bytes = new byte[1024];
            int n;
            while (-1 != (n = inputStream.read(bytes))) {
                byteArrayOutputStream.write(bytes, 0, n);
            }
            byteArrayOutputStream.close();
            inputStream.close();
            final byte[] response = byteArrayOutputStream.toByteArray();
            final FileOutputStream fileOutputStream = new FileOutputStream(fileLocation);
            fileOutputStream.write(response);
            fileOutputStream.close();
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void setIgnoreHorizontal(final boolean ignoreHorizontal) {
        try {
            final File file = new File("WallpaperChanger\\config.yml");
            final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            yamlConfiguration.set("ignoreHorizontal", ignoreHorizontal);
            yamlConfiguration.save(file);
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean isIgnoreHorizontal() {
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new File("WallpaperChanger\\config.yml"));
        return yamlConfiguration.getBoolean("ignoreHorizontal");
    }

    public static void setIndex(final int index) {
        try {
            final File file = new File("WallpaperChanger\\config.yml");
            final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            yamlConfiguration.set("index", index);
            yamlConfiguration.save(file);
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }

    public static int getIndex() {
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new File("WallpaperChanger\\config.yml"));
        return yamlConfiguration.getInt("index");
    }

    public static void nextImage() {
        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(new File("WallpaperChanger\\config.yml"));
        final String saveUrl = Core.getHourSaveUrl();

        int tempIndex = 1;
        if (!yamlConfiguration.contains(saveUrl + (Core.getIndex() + tempIndex))) {
            tempIndex = -Core.getIndex();
        }
        if (Core.isIgnoreHorizontal()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new URL(yamlConfiguration.getString(saveUrl + (Core.getIndex() + tempIndex) + ".imageUrl")));
                while (!(bufferedImage.getWidth() > bufferedImage.getHeight())) {
                    tempIndex++;
                    if (!yamlConfiguration.contains(saveUrl + (Core.getIndex() + tempIndex))) {
                        tempIndex = -Core.getIndex();
                    }
                    bufferedImage = ImageIO.read(new URL(yamlConfiguration.getString(saveUrl + (Core.getIndex() + tempIndex) + ".imageUrl")));
                }
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }
        Core.setIndex(Core.getIndex() + tempIndex);
        Core.downloadHourImage(Core.getIndex());
        Core.changeWallpaper(new File("WallpaperChanger\\image.jpg"));
        try {
            Main.updateSystemTray();
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }
}
