package models;

import java.io.File;

abstract public class AbstractFactory {
    public static String convertToClassIdentifier(String name){
        name = name.replace(".java","");
        name = name.replace("app\\","");

        if(File.separatorChar == '\\'){
            name = name.replaceAll("\\\\",".");
        }else if (File.separatorChar == '/'){
            name = name.replaceAll("/",".");
        }else{
            throw new RuntimeException("Unknown file separator: " + File.separator);
        }

        return name;
    }
}
