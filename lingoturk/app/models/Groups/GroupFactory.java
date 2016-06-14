package models.Groups;

import com.fasterxml.jackson.databind.JsonNode;
import models.AbstractFactory;
import models.LingoExpModel;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.listFiles;

public class GroupFactory extends AbstractFactory{
    public static AbstractGroup createPart(String type, LingoExpModel experiment, JsonNode node) throws SQLException {
        System.out.println("Create group of type: " + type);

        try {
            Class c = getClass(type);
            if(c == null){
                throw new ClassNotFoundException();
            }

            AbstractGroup group = (AbstractGroup) c.newInstance();
            group.save();

            group.addExperimentUsedIn(experiment);
            group.setJSONData(experiment, node);
            group.saveQuestions();

            group.update();

            return group;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String,Class> classMap = new HashMap<>();
    private static Class getClass(String name) throws ClassNotFoundException {
        if(classMap.containsKey(name)){
            return classMap.get(name);
        }

        File folder = new File("app/models/Groups");
        Collection<File> files = listFiles(folder, new String[]{"java"}, true);

        for(File f : files){
            String className = f.getName().replace(".java", "");
            if(className.equals(name)){
                String classIdentifier = convertToClassIdentifier(f.getPath());
                Class<?> c = Class.forName(classIdentifier);
                if(AbstractGroup.class.isAssignableFrom(c)){
                    classMap.put(name,c);
                    return c;
                }
            }
        }

        return null;
    }
}
