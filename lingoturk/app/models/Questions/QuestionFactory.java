package models.Questions;

import com.fasterxml.jackson.databind.JsonNode;
import models.AbstractFactory;
import models.LingoExpModel;
import services.LingoturkConfigImplementation;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.listFiles;

/**
 * Factory, which is used to create different types of questions, using the fitting type
 * and an JsonNode containing the question's data.
 */
public class QuestionFactory extends AbstractFactory {

    /**
     * Creates a new question of type {@code type} and data {@code node} for experiment {@code experiment}
     *
     * @param type       The type of the question
     * @param experiment The experiment that this question will be added to
     * @param node       The data for this node in JSON form
     * @return Returns the created question
     */
    public static Question createQuestion(String type, LingoExpModel experiment, JsonNode node) {
        System.out.println("Create question of type: " + type);

        // Check if a subclass exists. If it does not fall back to the default type
        Question question = null;
        try {
            Class c = getClass(type);
            if (c != null) {
                question = (Question) c.newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassCastException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (question == null) {
            question = new Question();
        }
        question.experimentType = type + "Experiment";
        question.experimentID = experiment.getId();

        JsonNode subListNode = node.get("subList");
        if (subListNode != null) {
            question.subList = subListNode.asText();
        } else {
            question.subList = "";
        }

        question.save();
        question.setJSONData(experiment, node);
        question.update();

        return question;
    }

    /**
     * Caches already looked up name to class conversion
     */
    private static Map<String, Class> classMap = new HashMap<>();

    /**
     * Finds the corresponding question for an experiment type
     *
     * @param name The name of the experiment type
     * @return The question class that belongs to this type
     * @throws ClassNotFoundException If a class name could not be converted to a class
     */
    private static Class getClass(String name) throws ClassNotFoundException {
        if (classMap.containsKey(name)) {
            return classMap.get(name);
        }

        File folder = new File(LingoturkConfigImplementation.staticPathPrefix() + "app/models/Questions");
        Collection<File> files = listFiles(folder, new String[]{"java"}, true);

        for (File f : files) {
            String className = f.getName().replace("Question.java", "");
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
