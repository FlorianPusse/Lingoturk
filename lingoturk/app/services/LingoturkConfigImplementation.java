package services;

import play.Environment;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Implements the LingoturkConfig. Other implementations might be added
 * sometime in the future.
 */
public class LingoturkConfigImplementation implements LingoturkConfig {
    private final Environment environment;

    /**
     * The path prefix used to open files
     */
    private static String prefix = null;

    /**
     * The location of the properties file
     */
    private static final String propertiesLocation = "conf/lingoturk.properties";

    /**
     * The content of the properties file
     */
    private static Properties properties = null;

    @Inject
    public LingoturkConfigImplementation(Environment environment) {
        this.environment = environment;
        String rootPath = this.environment.rootPath().getAbsolutePath();

        if (rootPath.matches(".*[/\\\\]target[/\\\\]universal[/\\\\]stage")) {
            prefix = rootPath.replaceAll("target[/\\\\]universal[/\\\\]stage", "");
        } else {
            prefix = "";
        }

        properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(getPathPrefix() + propertiesLocation)));
            System.out.println("[info] play - Properties loaded");
        } catch (IOException e) {
            System.out.println("[info] play - Couldn't load properties: " + e.getMessage());
        }
    }

    public String getPathPrefix() {
        return prefix;
    }

    public static String staticPathPrefix() {
        return prefix;
    }

    public boolean useBackup() {
        return properties.getProperty("useBackup").equals("true");
    }

    /**
     * Saves the given ip into the "lingoturk.properties" file, which is saved in the conf directory
     *
     * @param ip the ip-string
     */
    public void setStaticIp(String ip) throws IOException {
        properties.setProperty("serverip", ip);
        properties.store(new FileWriter(getPathPrefix() + propertiesLocation), null);
    }

    /**
     * Directly returns the saved ip from the properties file
     *
     * @return the saved IP
     */
    public String getStaticIp() {
        return properties.getProperty("serverip");
    }

    /**
     * Looks up the saved ip from the "lingoturk.properties" file, which is saved in the conf directory
     *
     * @return the saved IP
     */
    public static String staticGetStaticIp() {
        if (properties == null) {
            properties = new Properties();
            try {
                properties.load(new FileInputStream(new File(staticPathPrefix() + propertiesLocation)));
            } catch (IOException ioe) {
                return null;
            }
        }
        return properties.getProperty("serverip");
    }

    /**
     * Looks up the names of all existing experiment types.
     *
     * @return The list of experiment types.
     */
    public List<String> getExperimentNames() {
        List<String> experimentNames = new LinkedList<>();

        File folder = new File(getPathPrefix() + "app/models/Questions");
        File[] folders = folder.listFiles(File::isDirectory);
        for (File f : folders) {
            experimentNames.add(f.getName());
        }

        Collections.sort(experimentNames);

        return experimentNames;
    }
}
