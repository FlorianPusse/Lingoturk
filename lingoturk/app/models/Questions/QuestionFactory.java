package models.Questions;

import com.fasterxml.jackson.databind.JsonNode;
import models.AbstractFactory;
import models.LingoExpModel;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.listFiles;

/**
 * Factory, which is used to create different types of questions, using the fitting type
 * and an JsonNode containing the question's data.
 */
public class QuestionFactory extends AbstractFactory{

    public static PartQuestion createQuestion(String type, LingoExpModel experiment, JsonNode node) throws SQLException {
        System.out.println("Create question of type: " + type);

        try {
            Class c = getClass(type);
            if (c == null) {
                throw new ClassNotFoundException();
            }

            PartQuestion question = (PartQuestion) c.newInstance();
            question.experimentID = experiment.getId();

            JsonNode subListNode = node.get("subList");
            if(subListNode != null){
                question.subList = subListNode.asText();
            }else{
                question.subList = "";
            }

            question.save();
            question.setJSONData(experiment, node);
            question.update();

            return question;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | ClassCastException e) {
            return null;
        }
    }

    private static Map<String, Class> classMap = new HashMap<>();
    private static Class getClass(String name) throws ClassNotFoundException {
        if (classMap.containsKey(name)) {
            return classMap.get(name);
        }

        File folder = new File("app/models/Questions");
        Collection<File> files = listFiles(folder, new String[]{"java"}, true);

        for (File f : files) {
            String className = f.getName().replace(".java","");
            if (className.equals(name)) {
                String classIdentifier = convertToClassIdentifier(f.getPath());
                Class<?> c = Class.forName(classIdentifier);

                if (Question.class.isAssignableFrom(c)) {
                    classMap.put(name, c);
                    return c;
                }
            }
        }

        return null;
    }
}
