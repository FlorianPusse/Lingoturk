package models;

import java.io.File;

abstract public class AbstractFactory {
    public static String convertToClassIdentifier(String name){
        name = name.replace(".java","");

        if(File.separatorChar == '\\'){
            name = name.replace("app\\","");
            name = name.replaceAll("\\\\",".");
        }else if (File.separatorChar == '/'){
            name = name.replace("app/","");
            name = name.replaceAll("/",".");
        }else{
            throw new RuntimeException("Unknown file separator: " + File.separator);
        }

        return name;
    }
}
