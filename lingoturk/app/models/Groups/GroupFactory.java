package models.Groups;

import com.fasterxml.jackson.databind.JsonNode;
import models.AbstractFactory;
import models.LingoExpModel;
import services.LingoturkConfigImplementation;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.listFiles;

/**
 * Factory for Group objects
 */
public class GroupFactory extends AbstractFactory {

    /**
     * Creates a new group of type {@code type} and data {@code node} for experiment {@code experiment}
     *
     * @param type       The type of the group
     * @param experiment The experiment that this group will be added to
     * @param node       The data for this node in JSON form
     * @return Returns the created group
     */
    public static AbstractGroup createPart(String type, LingoExpModel experiment, JsonNode node) throws SQLException {
        System.out.println("Create group of type: " + type);

        try {
            Class c = getClass(type);
            if (c == null) {
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

    /**
     * Caches already looked up name to class conversion
     */
    private static Map<String, Class> classMap = new HashMap<>();

    /**
     * Finds the corresponding group for an experiment type
     *
     * @param name The name of the experiment type
     * @return The group class that belongs to this type
     * @throws ClassNotFoundException If a class name could not be converted to a class
     */
    private static Class getClass(String name) throws ClassNotFoundException {
        if (classMap.containsKey(name)) {
            return classMap.get(name);
        }

        File folder = new File(LingoturkConfigImplementation.staticPathPrefix() + "app/models/Groups");
        Collection<File> files = listFiles(folder, new String[]{"java"}, true);

        for (File f : files) {
            String className = f.getName().replace(".java", "");
            if (className.equals(name)) {
                String classIdentifier = convertToClassIdentifier(f.getPath());
                Class<?> c = Class.forName(classIdentifier);
                if (AbstractGroup.class.isAssignableFrom(c)) {
                    classMap.put(name, c);
                    return c;
                }
            }
        }

        return null;
    }
}
