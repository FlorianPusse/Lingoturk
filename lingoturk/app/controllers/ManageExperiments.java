package controllers;


import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.LingoExpModel;
import models.Groups.AbstractGroup;
import models.Groups.GroupFactory;
import models.Questions.DiscourseConnectivesExperiment.CheaterDetectionQuestion;
import models.Questions.ExampleQuestion;
import models.Questions.LinkingV1Experiment.Prolific.Combination;
import models.Questions.LinkingV1Experiment.Prolific.LinkingGroup;
import models.Questions.PartQuestion;
import models.Questions.Question;
import models.Questions.QuestionFactory;
import models.Repository;
import models.Worker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Crypto;
import play.mvc.*;
import play.twirl.api.Html;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;
import views.html.index;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import static models.AbstractFactory.convertToClassIdentifier;
import static org.apache.commons.io.FileUtils.listFiles;
import static play.libs.Json.stringify;

public class ManageExperiments extends Controller {

    public static Result changeExperimentFields(String experimentName){
        return ok(views.html.ManageExperiments.changeExperimentFields.render(experimentName));
    }

    /* Render experiment creation page */
    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    public static Result submitNewFields() throws IOException {
        JsonNode json = request().body().asJson();
        String experimentType = json.get("type").asText();

        for (Iterator<Map.Entry<String, JsonNode>> typeIterator = json.get("types").fields(); typeIterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = typeIterator.next();
            JsonNode classDetails = entry.getValue();
            String className = entry.getKey();
            String path = classDetails.get("path").asText();
            File fallBack = new File("app/models/Questions/" + experimentType + "Experiment/" + className + ".java");

            if(!path.equals("") || (path.equals("") && fallBack.exists())){
                File classFile = (path.equals("") ? fallBack : new File("app/" + path.replace(".","/") + ".java"));
                String fileContent = FileUtils.readFileToString(classFile);

                Pattern pattern = Pattern.compile("/\\* BEGIN OF VARIABLES BLOCK \\*/.*/\\* END OF VARIABLES BLOCK \\*/",Pattern.DOTALL);
                Matcher matcher = pattern.matcher(fileContent);
                matcher.find();
                fileContent = matcher.replaceAll("// _VARIABLES_PLACEHOLDER_");

                List<String[]> fields = new LinkedList<>();
                for(Iterator<JsonNode> fieldIterator = entry.getValue().get("fields").iterator(); fieldIterator.hasNext();){
                    JsonNode fieldNode = fieldIterator.next();
                    String fieldName = fieldNode.get("name").asText();
                    String fieldType = fieldNode.get("type").asText();
                    fields.add(new String[]{fieldName, fieldType});
                }

                FileUtils.writeStringToFile(classFile, setFields(fileContent, experimentType, fields), StandardCharsets.UTF_8);
            }else{
                String newClass = FileUtils.readFileToString(new File("template/templateClass.java"));
                newClass = newClass.replace("_EXPERIMENT_",experimentType);
                newClass = newClass.replace("_CLASSNAME_", className);

                List<String[]> fields = new LinkedList<>();
                for(Iterator<JsonNode> fieldIterator = entry.getValue().get("fields").iterator(); fieldIterator.hasNext();){
                    JsonNode fieldNode = fieldIterator.next();
                    String fieldName = fieldNode.get("name").asText();
                    String fieldType = fieldNode.get("type").asText();
                    fields.add(new String[]{fieldName, fieldType});
                }

                FileUtils.writeStringToFile(fallBack, setFields(newClass, experimentType, fields), StandardCharsets.UTF_8);
            }
        }

        return ok();
    }

    public static Result getExperimentDetails(String experimentName) throws ClassNotFoundException, IOException {
        Properties experimentProperties = new Properties();
        experimentProperties.load(new FileReader("app/models/Questions/" + experimentName + "Experiment/experiment.properties"));

        String questionName = experimentProperties.getProperty("questionType");
        String groupName = experimentProperties.getProperty("groupType");

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        Set<String> observedTypes = new HashSet<>();
        addFieldType(groupName,objectBuilder,observedTypes,false,true);
        addFieldType(questionName,objectBuilder,observedTypes,true,false);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(objectBuilder.build().toString());
        return ok(stringify(actualObj));
    }

    static String setFields(String fileContent, String experimentName, List<String[]> fields){
        String setJsonFunction = "    @Override\n" +
                "    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {\n";

        String createdFields = "/* BEGIN OF VARIABLES BLOCK */\n\n";

        for(String[] field : fields){
            String fieldType = field[1].trim();

            if(fieldType.equals("java.lang.String") || fieldType.equals("String")){
                createdFields += "\t@Basic\n";
                createdFields += "\t@Column(name=\"" + experimentName + "_" + field[0] +  "\", columnDefinition = \"TEXT\")\n";
                createdFields += "\tpublic " + field[1] + " " + field[0] + " = \"\";\n\n";

                setJsonFunction += "\t\tJsonNode " + field[0] + "Node = questionNode.get(\"" + field[0] + "\");\n";
                setJsonFunction += "\t\tif (" + field[0] + "Node != null){\n";
                setJsonFunction += "\t\t\tthis." + field[0] + " = " + field[0] + "Node.asText();\n";
                setJsonFunction += "\t\t}\n\n";
            }else if(fieldType.equals("java.lang.Integer") || fieldType.equals("Integer") || fieldType.equals("int")){
                createdFields += "\t@Basic\n";
                createdFields += "\t@Column(name=\"" + experimentName + "_" + field[0] + "\")\n";
                createdFields += "\tpublic " + field[1] + " " + field[0] + " = -1;\n\n";

                setJsonFunction += "\t\tJsonNode " + field[0] + "Node = questionNode.get(\"" + field[0] + "\");\n";
                setJsonFunction += "\t\tif (" + field[0] + "Node != null){\n";
                setJsonFunction += "\t\t\tthis." + field[0] + " = " + field[0] + "Node.asInt();\n";
                setJsonFunction += "\t\t}\n\n";
            }else if(fieldType.equals("java.lang.Float") || fieldType.equals("Float") || fieldType.equals("float")){
                createdFields += "\t@Basic\n";
                createdFields += "\t@Column(name=\"" + experimentName + "_" + field[0] + "\")\n";
                createdFields += "\tpublic " + field[1] + " " + field[0] + " = -1.0f;\n\n";

                setJsonFunction += "\t\tJsonNode " + field[0] + "Node = questionNode.get(\"" + field[0] + "\");\n";
                setJsonFunction += "\t\tif (" + field[0] + "Node != null){\n";
                setJsonFunction += "\t\t\tthis." + field[0] + " = (float)" + field[0] + "Node.asDouble();\n";
                setJsonFunction += "\t\t}\n\n";
            }else if(fieldType.equals("java.lang.Boolean") || fieldType.equals("Boolean") || fieldType.equals("boolean")){
                createdFields += "\t@Basic\n";
                createdFields += "\t@Column(name=\"" + experimentName + "_" + field[0] + "\")\n";
                createdFields += "\tpublic " + field[1] + " " + field[0] + " = false;\n\n";

                setJsonFunction += "\t\tJsonNode " + field[0] + "Node = questionNode.get(\"" + field[0] + "\");\n";
                setJsonFunction += "\t\tif (" + field[0] + "Node != null){\n";
                setJsonFunction += "\t\t\tthis." + field[0] + " = " + field[0] + "Node.asBoolean();\n";
                setJsonFunction += "\t\t}\n\n";
            } else if(fieldType.startsWith("java.util.List")){
                createdFields += "\t@OneToMany(cascade = CascadeType.ALL)\n";
                createdFields += "\tpublic " + field[1] + " " + field[0] + " = new LinkedList<>();\n\n";
            }else{
                throw new RuntimeException("Unknown fieldType: " + fieldType);
            }
        }

        setJsonFunction += "    }\n\n";
        createdFields += setJsonFunction;
        createdFields += "\t/* END OF VARIABLES BLOCK */\n";

        return fileContent.replaceAll("// _VARIABLES_PLACEHOLDER_", createdFields);
    }

    private static void addFieldType(String className, JsonObjectBuilder objectBuilder, Set<String> observedTypes, boolean isQuestionType, boolean isGroupType) throws ClassNotFoundException {
        JsonArrayBuilder fieldBuilder = Json.createArrayBuilder();
        Class c = Class.forName(className);

        if(observedTypes.contains(c.getSimpleName())){
            return;
        }

        for (Field s : getFields(c)) {
            JsonObjectBuilder questionBuilder = Json.createObjectBuilder();
            if (s.getGenericType().getTypeName().startsWith("java.util.List")) {
                if(s.getGenericType() instanceof ParameterizedType){
                    ParameterizedType pType = (ParameterizedType) s.getGenericType();
                    String typeName = pType.getActualTypeArguments()[0].getTypeName();
                    if(!observedTypes.contains(typeName)){
                        observedTypes.add(typeName);
                        addFieldType(pType.getActualTypeArguments()[0].getTypeName(),objectBuilder,observedTypes,false,false);
                    }
                }
            }
            fieldBuilder.add(questionBuilder.add("type", s.getGenericType().toString().replace("class ",""))
                    .add("name", s.getName())
                    .add("simpleTypeName",s.getType().getSimpleName())
                    .build());
        }
        objectBuilder.add(c.getSimpleName(), Json.createObjectBuilder()
                .add("fields",fieldBuilder.build())
                .add("isQuestionType",isQuestionType)
                .add("isGroupType",isGroupType)
                .add("path",c.getName())
                .build());
    }

    private static List<Field> getFields(Class c) throws ClassNotFoundException {
        String[] unusedFields = new String[]{"finder", "_idGetSet", "random", "questions", "id", "availability", "experimentID"};
        List<Field> fieldNames = new LinkedList<>();
        do {
            for (Field f : c.getDeclaredFields()) {
                String fieldName = f.getName();
                if (!(fieldName.toUpperCase().contains("_EBEAN_") || fieldName.toUpperCase().startsWith("FILLER") || Arrays.asList(unusedFields).contains(fieldName) || Modifier.isStatic(f.getModifiers()))) {
                    fieldNames.add(f);
                }
            }
        } while ((c = c.getSuperclass()) != null);

        return fieldNames;
    }

    /* Render experiment creation page */
    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    public static Result submitResults() {
        JsonNode json = request().body().asJson();
        String experimentType = json.get("experimentType").asText();
        Class<?> experimentClass = ManageExperiments.lookupExperimentType(experimentType);

        if (experimentClass == null) {
            return internalServerError("Unknown experiment type: " + experimentType);
        }

        try {
            Question question = (Question) experimentClass.newInstance();
            Method m = experimentClass.getMethod("writeResults", JsonNode.class);
            m.invoke(question, json);
        } catch (NoSuchMethodException e) {
            return internalServerError("Function write: " + experimentType);
        } catch (InstantiationException e) {
            return internalServerError("No default constructor for: " + experimentType);
        } catch (IllegalAccessException e) {
            return internalServerError("writeResults method is not accessible: " + experimentType);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return internalServerError("writeResults method throws exception: \n" + e.getCause().getMessage());
        }

        return ok();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result submitResult() throws SQLException {
        JsonNode json = request().body().asJson();
        if (json != null) {
            String assignmentID = json.get("assignmentID").asText();
            if (assignmentID != null && !assignmentID.equals("ASSIGNMENT_ID_NOT_AVAILABLE_TEST")) {
                AsynchronousJob.addAssignmentID(assignmentID);
            }
        }

        return ok();
    }

    @Security.Authenticated(Secured.class)
    public static Result createExperiment(String name) {
        try {
            Class<?> c = Class.forName("views.html.ExperimentCreation." + name + ".create" + name);
            Method m = c.getMethod("render");
            Html webpage = (Html) m.invoke(null);
            return ok(webpage);
        } catch (ClassNotFoundException e) {
            // Remove "Experiment" at the end of name
            return ok(views.html.ExperimentCreation.createExperiment.render(name.substring(0,name.length() - "Experiment".length())));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
            return internalServerError("Wrong type for name: " + name);
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result experimentCreationInterface() {
        return ok(views.html.ManageExperiments.experimentCreationInterface.render());
    }

    private static Set<String> listExperimentTypes() {
        return null;
    }

    private static boolean typeNameExists(String path, String name) {
        File filePath = new File(path);

        if (!filePath.exists()) {
            throw new IllegalStateException("Path \"" + path + "\" does not exist.");
        }

        for (File f : filePath.listFiles()) {
            if (f.isDirectory()) {
                if (f.getName().equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isTypeNameAvailable(String name) {
        boolean dir1 = typeNameExists("app/models/Groups/", name);
        boolean dir2 = typeNameExists("app/models/Questions/", name);
        boolean dir3 = typeNameExists("app/views/ExperimentCreation/", name);
        boolean dir4 = typeNameExists("app/views/ExperimentRendering/", name);
        boolean dir5 = typeNameExists("public/javascripts/ExperimentCreation/", name);
        boolean dir6 = typeNameExists("public/javascripts/ExperimentRendering/", name);
        boolean dir7 = typeNameExists("public/stylesheets/ExperimentRendering/", name);

        return !(dir1 || dir2 || dir3 || dir4 || dir5 || dir6 || dir7);
    }

    private static void copyExperimentFile(String sourcepath, String destination, String oldName, String newName) throws IOException {
        File sourceFile = new File(sourcepath);
        File destinationFile = new File(destination);

        String fileContent = FileUtils.readFileToString(sourceFile);
        fileContent = fileContent.replaceAll(oldName, newName);

        FileUtils.writeStringToFile(destinationFile, fileContent, StandardCharsets.UTF_8);
    }

    private static void copyExperimentDirectory(String sourcepath, String destination, String oldName, String newName) throws IOException {
        if (sourcepath.charAt(sourcepath.length() - 1) != '/') {
            sourcepath = sourcepath + '/';
        }

        if (destination.charAt(destination.length() - 1) != '/') {
            destination = destination + '/';
        }

        File sourceFile = new File(sourcepath);
        for (File f : sourceFile.listFiles()) {
            if (f.isDirectory()) {
                copyExperimentDirectory(sourcepath + f.getName(), destination + f.getName(), oldName, newName);
            } else {
                copyExperimentFile(sourcepath + f.getName(), destination + f.getName().replaceAll(oldName, newName), oldName, newName);
            }
        }

        File destinationFile = new File(destination);
        if (!destinationFile.exists()) {
            destinationFile.mkdirs();
        }
    }

    private static void createExperimentType(String name, List<String[]> questionFields, String reusedGroupName) {
        try {
            //copyExperimentFile("template/templateExperimentCreation.scala.html",
             //       "app/views/ExperimentCreation/" + name + "Experiment/create" + name + "Experiment.scala.html", "_TEMPLATE_", name);

            copyExperimentFile("template/templateExperimentRendering.scala.html",
                    "app/views/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.scala.html", "_TEMPLATE_", name);

            if(reusedGroupName != null) {
                 copyExperimentFile("template/templateQuestion.java",
                        "app/models/Questions/" + name + "Experiment/" + name + "Question.java", "_TEMPLATE_", name);
            }else{
                copyExperimentFile("template/templateGroup.java",
                        "app/models/Groups/" + name + "Experiment/" + name + "Group.java", "_TEMPLATE_", name);

                copyExperimentFile("template/templateQuestion.java",
                        "app/models/Questions/" + name + "Experiment/" + name + "Question.java", "_TEMPLATE_", name);
            }

            File questionFile = new File("app/models/Questions/" + name + "Experiment/" + name + "Question.java");
            String fileContent = FileUtils.readFileToString(questionFile);
            fileContent = setFields(fileContent,name, questionFields);
            FileUtils.writeStringToFile(questionFile, fileContent, StandardCharsets.UTF_8);

            copyExperimentFile("template/templateResultQuery.sql",
                    "app/models/Questions/" + name + "Experiment/resultQuery.sql", "_TEMPLATE_", name);

            //copyExperimentFile("template/templateExperimentCreation.js",
             //       "public/javascripts/ExperimentCreation/" + name + "Experiment/create" + name + "Experiment.js", "_TEMPLATE_", name);

            copyExperimentFile("template/templateExperimentRendering.js",
                    "public/javascripts/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.js", "_TEMPLATE_", name);

            copyExperimentFile("template/templateExperimentRendering.css",
                    "public/stylesheets/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.css", "_TEMPLATE_", name);

            File propertiesFile = new File("app/models/Questions/" + name + "Experiment/experiment.properties");
            BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile));
            writer.write("questionType = models.Questions." + name + "Experiment." + name + "Question\n");
            if(reusedGroupName != null){
                writer.write("groupType = models.Groups." + reusedGroupName);
            }else{
                writer.write("groupType = models.Groups." + name + "Experiment." + name + "Group");
            }
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Error occured while copying experiment files: " + e.getMessage());
        }
    }

    private static void copyExperimentType(String sourceExperiment, String targetExperiment) {
        try {
            String sourceExperimentName = sourceExperiment + "Experiment/";
            String targetExperimentName = targetExperiment + "Experiment/";

            // This directory may exist but it doesn't have to, copy only if it does
            File groupFile = new File("app/models/Groups/" + sourceExperimentName);
            if (groupFile.exists() && groupFile.isDirectory()) {
                copyExperimentDirectory("app/models/Groups/" + sourceExperimentName, "app/models/Groups/" + targetExperimentName, sourceExperiment, targetExperiment);
            }

            // All other directories have to exist -> Otherwise, IOException will be thrown
            copyExperimentDirectory("app/models/Questions/" + sourceExperimentName, "app/models/Questions/" + targetExperimentName, sourceExperiment, targetExperiment);
            copyExperimentDirectory("app/views/ExperimentCreation/" + sourceExperimentName, "app/views/ExperimentCreation/" + targetExperimentName, sourceExperiment, targetExperiment);
            copyExperimentDirectory("app/views/ExperimentRendering/" + sourceExperimentName, "app/views/ExperimentRendering/" + targetExperimentName, sourceExperiment, targetExperiment);
            copyExperimentDirectory("public/javascripts/ExperimentCreation/" + sourceExperimentName, "public/javascripts/ExperimentCreation/" + targetExperimentName, sourceExperiment, targetExperiment);
            copyExperimentDirectory("public/javascripts/ExperimentRendering/" + sourceExperimentName, "public/javascripts/ExperimentRendering/" + targetExperimentName, sourceExperiment, targetExperiment);
            copyExperimentDirectory("public/stylesheets/ExperimentRendering/" + sourceExperimentName, "public/stylesheets/ExperimentRendering/" + targetExperimentName, sourceExperiment, targetExperiment);
        } catch (IOException e) {
            throw new IllegalStateException("Error occured while copying experiment files: " + e.getMessage());
        }
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Result createNewExperimentType() {
        JsonNode json = request().body().asJson();

        String newTypeName = json.get("newTypeName").asText().trim();

        if (newTypeName.equals("")) {
            return internalServerError("Input name is empty!");
        }

        if (!StringUtils.isAlphanumeric(newTypeName)) {
            return internalServerError("Name is not alphanumeric!");
        }

        if (newTypeName.equals("template")) {
            return internalServerError("Name \"template\" is reserved!");
        }

        // Test if type name already exists or relevant directories are corrupt
        try {
            if (!isTypeNameAvailable(newTypeName + "Experiment")) {
                return internalServerError("Name already exists!");
            }
        } catch (IllegalStateException ex) {
            return internalServerError(ex.getMessage());
        }

        // Everything is ok. Test if completely new experiment should be created or another one reused.
        String experimentType = json.get("experimentType").asText();

        List<String[]> questionFields = new LinkedList<>();
        for(Iterator<JsonNode> questionFieldIterator = json.get("questionFields").iterator(); questionFieldIterator.hasNext();){
            JsonNode questionField = questionFieldIterator.next();
            questionFields.add(new String[]{questionField.get("name").asText(), questionField.get("type").asText()});
        }

        switch (experimentType) {
            case "COPY":
                String sourceTypeName = json.get("sourceTypeName").asText();
                if (isTypeNameAvailable(sourceTypeName + "Experiment")) {
                    return internalServerError("Source experiment name does not exist.");
                }
                copyExperimentType(sourceTypeName, newTypeName);
                break;
            case "NEW":
                createExperimentType(newTypeName,questionFields, null);
                break;
            case "REUSE":
                String sourceGroup = json.get("sourceGroupName").asText();
                try {
                    createExperimentType(newTypeName,questionFields, sourceGroup);
                } catch (IllegalStateException e) {
                    return internalServerError(e.getMessage());
                }
                break;
            default:
                return internalServerError("Unknown experiment type.");
        }

        return ok();
    }

    private static Map<String, Class> classMap = new HashMap<>();

    public static Class lookupExperimentType(String experimentName) {
        if (classMap.containsKey(experimentName)) {
            return classMap.get(experimentName);
        }

        File folder = new File("app/models/Questions/" + experimentName + "/");
        if (folder.exists() && folder.isDirectory()) {
            Collection<File> files = listFiles(folder, new String[]{"java"}, false);
            for (File f : files) {
                String classIdentifier = convertToClassIdentifier(f.getPath());
                Class<?> c = null;
                try {
                    c = Class.forName(classIdentifier);
                } catch (ClassNotFoundException e) {
                    // Is not a correct implemented file -> Play will take care of that
                }

                if (PartQuestion.class.isAssignableFrom(c)) {
                    classMap.put(experimentName, c);
                    return c;
                }
            }
        }

        return null;
    }

    @Security.Authenticated(Secured.class)
    public static Result editInstructions(int expId) {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        return ok(views.html.ManageExperiments.editInstructions.render(expId, expModel.getAdditionalExplanations()));
    }

    @Security.Authenticated(Secured.class)
    public static Result submitNewInstructions() throws SQLException {
        DynamicForm requestData = Form.form().bindFromRequest();
        int expId = Integer.parseInt(requestData.get("expId"));
        String instructions = requestData.get("instructions");
        LingoExpModel expModel = LingoExpModel.byId(expId);
        expModel.setAdditionalExplanations(instructions);
        return ok(views.html.ManageExperiments.editInstructions.render(expId, expModel.getAdditionalExplanations()));
    }

    public static List<String> getExperimentNames() {
        List<String> experimentNames = new LinkedList<>();

        File folder = new File("app/models/Questions");
        File[] folders = folder.listFiles(File::isDirectory);
        for (File f : folders) {
            experimentNames.add(f.getName());
        }

        return experimentNames;
    }

    public static Result returnJSON(int expID) throws SQLException, IOException {
        LingoExpModel exp = LingoExpModel.byId(expID);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(exp.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    public static Result getQuestion(int id) throws SQLException, IOException {
        Question question = Question.byId(id);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(question.returnJSON().toString());
        return ok(actualObj);
    }

    public static Result returnPart(int partId) throws SQLException, IOException {
        AbstractGroup p = AbstractGroup.byId(partId);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(p.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    public static String getStoredName(Http.Request r, int expId) {
        Http.Cookie cookie = r.cookie("name_" + expId);
        if (cookie != null) {
            String name = Crypto.decryptAES(cookie.value());
            System.out.println("Cookie set for experiment " + expId + ". Stored name: " + name);
            return name;
        } else {
            return null;
        }
    }

    static int fallBackCounter = 0;

    public static Result returnPartAsJSON(int expId, String workerId) throws SQLException, IOException {
        LingoExpModel exp = LingoExpModel.byId(expId);

        Worker worker = Worker.getWorkerById(workerId);
        if (worker == null) {
            worker = Worker.createWorker(workerId);
        }

        String stored_workerId = getStoredName(request(), exp.getId());
        Worker stored_worker = null;
        if (stored_workerId != null) {
            stored_worker = Worker.getWorkerById(stored_workerId);
        }
        AbstractGroup group = null;

        // Worker didn't accept this hit yet
        if (stored_workerId == null || stored_worker == null) {
            Worker.Participation participation = worker.getParticipatesInExperiment(exp);

            if (participation == null) {
                for (AbstractGroup p : exp.getParts()) {
                    if (p.decreaseIfAvailable()) {
                        group = p;
                        break;
                    }
                }
                if (group == null) {
                    group = exp.getParts().get(fallBackCounter);
                    System.out.println("No Parts available. Fallback to Group Nr. " + group.getId());
                    fallBackCounter++;
                    if (fallBackCounter >= exp.getParts().size()) {
                        fallBackCounter = 0;
                    }
                }
                worker.addParticipatesInPart(group, null, null, "Prolific", null);
            } else {
                group = AbstractGroup.byId(participation.getPartID());
            }
        } else {
            if (!stored_workerId.equals(workerId)) {
                System.out.println(stored_workerId + " tries to open experiment " + expId + " again with new workerId " + workerId);
            }

            Worker.Participation participation = stored_worker.getParticipatesInExperiment(exp);

            if (participation == null) {
                throw new RuntimeException();
            } else {
                group = AbstractGroup.byId(participation.getPartID());

                if (!stored_workerId.equals(workerId)) {
                    Worker.updateParticipatesInPart(stored_workerId, group, workerId);
                }
            }
        }

        response().setCookie("name_" + expId, Crypto.encryptAES(workerId), 999999);

        if (group instanceof LinkingGroup) {
            PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT DISTINCT lhs_script, rhs_script FROM LinkingResult WHERE workerId = ?");
            statement.setString(1, workerId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int lhs_script = rs.getInt("lhs_script");
                int rhs_script = rs.getInt("rhs_script");

                for (Iterator<PartQuestion> combinationIterator = group.getQuestions().iterator(); combinationIterator.hasNext(); ) {
                    Combination c = (Combination) combinationIterator.next();
                    if (c.getLhs() == lhs_script && c.getRhs() == rhs_script) {
                        combinationIterator.remove();
                    }
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(group.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    /**
     * Submits a new DragNDrop experiment and saves it into the database.
     * It collects all data, which was submitted via POST.
     *
     * @return the index-page
     */
    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    public static Result submitNewExperiment() throws SQLException {
        JsonNode json = request().body().asJson();

        LingoExpModel experiment;
        String name = json.get("name").asText();
        String description = json.get("description").asText();
        String nameOnAmt = json.get("nameOnAmt").asText();
        String additionalExplanations = json.get("additionalExplanations").asText();
        String experimentType = json.get("type").asText();

        // new experiment
        if (json.get("id").asInt() == -1) {
            experiment = LingoExpModel.createLingoExpModel(name, description, additionalExplanations, nameOnAmt, experimentType);
        } else {
            experiment = LingoExpModel.byId(json.get("id").asInt());
            experiment.setName(name);
            experiment.setDescription(description);
            experiment.setNameOnAmt(nameOnAmt);
            experiment.setAdditionalExplanations(additionalExplanations);
            experiment.setExperimentType(experimentType);
        }

        experiment.update();

        // Create example question
        List<ExampleQuestion> exampleQuestions_tmp = new LinkedList<>();
        for (Iterator<JsonNode> exampleQuestions = json.get("exampleQuestions").iterator(); exampleQuestions.hasNext(); ) {
            JsonNode question = exampleQuestions.next();
            String type = question.get("type").asText();
            ExampleQuestion exampleQuestion = QuestionFactory.createExampleQuestion(type, experiment, question);
            exampleQuestions_tmp.add(exampleQuestion);
        }

        experiment.setExampleQuestions(exampleQuestions_tmp);


        // Create cheater detection questions
        JsonNode cheaterDetectionNode = json.get("cheaterDetectionQuestions");
        if (cheaterDetectionNode != null) {
            List<CheaterDetectionQuestion> cheaterDetectionQuestions_tmp = CheaterDetectionQuestion.createCheaterDetectionQuestions(experiment, json.get("cheaterDetectionQuestions"));
            experiment.setCheaterDetectionQuestions(cheaterDetectionQuestions_tmp);
        }

        // Create Parts
        List<AbstractGroup> groups = new LinkedList<>();
        for (Iterator<JsonNode> partIterator = json.get("parts").iterator(); partIterator.hasNext(); ) {
            JsonNode partNode = partIterator.next();
            AbstractGroup p = GroupFactory.createPart(partNode.get("type").asText(), experiment, partNode);
            if (p != null) {
                groups.add(p);
            } else {
                return internalServerError("Unknown Group type: " + partNode.get("type").asText());
            }
        }

        // Redirect
        return ok(index.render());
    }
}