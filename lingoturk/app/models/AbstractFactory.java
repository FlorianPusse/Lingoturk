package models;

import services.LingoturkConfigImplementation;

import java.io.File;

/**
 * Converts a name to a class path, i.e. replaces file separators by dots
 */
abstract public class AbstractFactory {
    public static String convertToClassIdentifier(String name) {
        String pathPrefix = LingoturkConfigImplementation.staticPathPrefix();

        name = name.replace(".java", "").replace(pathPrefix,"");

        if (File.separatorChar == '\\') {
            name = name.replace("app\\", "");
            name = name.replaceAll("\\\\", ".");
        } else if (File.separatorChar == '/') {
            name = name.replace("app/", "");
            name = name.replaceAll("/", ".");
        } else {
            throw new RuntimeException("Unknown file separator: " + File.separator);
        }

        return name;
    }
}
