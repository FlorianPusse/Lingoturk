package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import play.data.FormFactory;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.DatabaseService;
import services.ExperimentWatchService;
import services.LingoturkConfig;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

import static play.libs.Json.stringify;

/***
 * Controller handling handling requests related to experiment types.
 * These requests include creation, modification, or deletion.
 */
public class ExperimentTypeController extends Controller {

    private final DatabaseService databaseService;
    private final LingoturkConfig lingoturkConfig;
    private final ExperimentWatchService experimentWatchService;

    @Inject
    public ExperimentTypeController(final FormFactory formFactory, DatabaseService databaseService, LingoturkConfig lingoturkConfig, ExperimentWatchService experimentWatchService) {
        this.databaseService = databaseService;
        this.lingoturkConfig = lingoturkConfig;
        this.experimentWatchService = experimentWatchService;
    }

    /**
     * Lists all directories of a given experiment type, represented by {@experimentName}
     *
     * @param experimentName The name of the experiment type
     * @return The list of all directories for the given experiment type
     */
    private List<File> getDirectories(String experimentName) {
        List<File> directories = new LinkedList<>();

        String suffix = experimentName != null ? experimentName : "";

        directories.add(new File(lingoturkConfig.getPathPrefix() + "app/models/Groups/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "app/views/ExperimentCreation/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "app/views/ExperimentRendering/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "public/javascripts/ExperimentCreation/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "public/javascripts/ExperimentRendering/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "public/stylesheets/ExperimentRendering/" + suffix));
        directories.add(new File(lingoturkConfig.getPathPrefix() + "public/images/Experiments/" + suffix));

        return directories;
    }

    /**
     * Renders the experiment type creation interface.
     *
     * @return The rendered experiment type creation interface
     */
    @SubjectPresent
    public Result experimentCreationInterface() {
        return ok(views.html.ExperimentTypeManagement.experimentCreationInterface.render());
    }

    /**
     * Deleted the experiment type represented by the name {@code experimentName}
     *
     * @param experimentName The name of the experiment that should be delted
     * @return {@code ok} if no error occurs. An {@code internalServerError} otherwise
     */
    @SubjectPresent
    public Result deleteExperimentType(String experimentName) {
        databaseService.backupDatabase();
        experimentWatchService.removeExperimentType(experimentName);

        for (File d : getDirectories(experimentName)) {
            if (d.exists() && d.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(d);
                    for (LingoExpModel exp : LingoExpModel.getAllExperiments()) {
                        if (exp.getExperimentType().equals(experimentName)) {
                            exp.delete();
                        }
                    }
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                    return internalServerError(e.getMessage());
                }
            }
        }

        return redirect("/");
    }

    /**
     * Exports an experiment type. All files will be packaged into a zip file and returned.
     *
     * @return {@code ok} if no error occurs. An {@code internalServerError} otherwise
     */
    @SubjectPresent
    public Result exportExperimentType(String experimentName) {
        ZipFile zipFile;
        File f;

        try {
            f = File.createTempFile(experimentName, ".zip");
            f.delete();

            zipFile = new ZipFile(f);
            f.deleteOnExit();
        } catch (ZipException | IOException e) {
            e.printStackTrace();
            return internalServerError("Error: " + e.getMessage());
        }

        for (File d : getDirectories(experimentName)) {
            if (d.exists() && d.isDirectory()) {
                try {
                    ZipParameters parameters = new ZipParameters();
                    // Remove prefix, replace separator by __ and remove the experiment name
                    String path = StringUtils.replaceOnce(d.getPath().replace(lingoturkConfig.getPathPrefix(), "").replace(File.separator, "__"), "__" + experimentName, "");
                    parameters.setRootFolderInZip(path);
                    zipFile.addFolder(d, parameters);
                } catch (ZipException e) {
                    e.printStackTrace();
                    return internalServerError("Error: " + e.getMessage());
                }
            }
        }

        response().setHeader("Content-Disposition", "attachment; filename=" + experimentName + ".zip");
        return ok(zipFile.getFile());
    }

    /**
     * Imports an experiment type from a submitted .zip file
     *
     * @return {@code ok} if no error occurs. An {@code internalServerError} otherwise
     */
    @SubjectPresent
    public Result importExperimentType() {
        databaseService.backupDatabase();

        Http.MultipartFormData<File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<File> experimentData = body.getFile("experimentData");
        if (experimentData != null) {
            String fileName = experimentData.getFilename();
            File file = experimentData.getFile();

            try {
                ZipFile zipFile = new ZipFile(file);
                if (!zipFile.isValidZipFile()) {
                    return internalServerError("Your ZIP file is not valid: " + fileName);
                }
                for (FileHeader fh : (List<FileHeader>) zipFile.getFileHeaders()) {
                    if (!fh.isDirectory()) {
                        String directoryName = fh.getFileName();

                        // if that is the case, something is seriously wrong here
                        if (!(directoryName.startsWith("app__") || directoryName.startsWith("public__"))) {
                            continue;
                        }

                        directoryName = directoryName.replace("__", "/");
                        fh.setFileName(directoryName.substring(directoryName.lastIndexOf("/") + 1));
                        zipFile.extractFile(fh, lingoturkConfig.getPathPrefix() + directoryName.substring(0, directoryName.lastIndexOf("/")));
                    }
                }
            } catch (ZipException e) {
                e.printStackTrace();
                return internalServerError("Error reading your ZIP file: " + fileName);
            }

            experimentWatchService.addExperimentType(fileName.replace(".zip", ""));

            return redirect("/");
        }

        return internalServerError("File not found.");
    }

    /**
     * Returns the experiment change interface for a given experiment type represented
     * by {@code experimentName}
     *
     * @param experimentName The experiment type that should be updated
     * @return The experiment change interface
     */
    public Result changeExperimentFields(String experimentName) {
        return ok(views.html.ExperimentTypeManagement.changeExperimentFields.render(experimentName));
    }

    /**
     * Stores new fields for a given experiment type {@code type}
     *
     * @return Returns ok if no error occurs. An internalServerError otherwise
     */
    @BodyParser.Of(value = BodyParser.Json.class)
    @SubjectPresent
    public Result submitNewFields() {
        databaseService.backupDatabase();

        JsonNode json = request().body().asJson();
        String experimentType = json.get("type").asText();

        File fieldsFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + experimentType + "Experiment/fields.json");
        List<String[]> fields = new LinkedList<>();

        for (Iterator<JsonNode> fieldIterator = json.get("types").get(experimentType).get("fields").iterator(); fieldIterator.hasNext(); ) {
            JsonNode fieldNode = fieldIterator.next();
            String fieldName = fieldNode.get("name").asText();
            String fieldType = fieldNode.get("type").asText();
            fields.add(new String[]{fieldName, fieldType});
        }

        try {
            setFields(fieldsFile, fields);
        } catch (IOException e) {
            return internalServerError(e.getMessage());
        }

        return ok();
    }

    /**
     * Stores new fields experiment fields to a file. If the file does not exist yet,
     * it will be created automatically
     *
     * @param file   The file that the fields should be stored to
     * @param fields The new experiment fields
     * @throws IOException If the JSOn representation cannot be converted
     */
    static void setFields(File file, List<String[]> fields) throws IOException {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (String[] field : fields) {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("type", field[1]);
            objectBuilder.add("simpleTypeName", field[1]);
            objectBuilder.add("name", field[0]);

            arrayBuilder.add(objectBuilder.build());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(arrayBuilder.build().toString());
        FileUtils.writeStringToFile(file, stringify(actualObj), "UTF-8");
    }

    /**
     * Looks up and returns the details/fields of an experiment type represented by the name {@code experimentName}
     *
     * @param experimentName The name of the experiment type to look up to
     * @return The details of the experiment type
     * @throws ClassNotFoundException If one of the underlying classes of the experiment type does not exist
     * @throws IOException            If the object cannot be converted to JSON
     */
    @SubjectPresent
    public Result getExperimentDetails(String experimentName) throws ClassNotFoundException, IOException {
        Properties experimentProperties = new Properties();
        experimentProperties.load(new FileReader("app/models/Questions/" + experimentName + "Experiment/experiment.properties"));

        String fields = FileUtils.readFileToString(new File("app/models/Questions/" + experimentName + "Experiment/fields.json"), "UTF-8");
        String groupName = experimentProperties.getProperty("groupType");

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        Set<String> observedTypes = new HashSet<>();
        addFieldType(groupName, objectBuilder, observedTypes);
        addFieldType("models.Results.Results", objectBuilder, observedTypes);

        objectBuilder.add(experimentName, Json.createObjectBuilder()
                .add("fields", Json.createReader(new StringReader(fields)).readArray())
                .add("isQuestionType", true)
                .add("isGroupType", false)
                .build());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(objectBuilder.build().toString());
        return ok(stringify(actualObj));
    }

    /**
     * Adds the class represented by {@code className} to the {@code objectBuilder}, if it has not been
     * observed yet.
     *
     * @param className     The name of the class to check
     * @param objectBuilder The JSON objectBuilder that the class will be added to
     * @param observedTypes The list of currently observed types
     * @throws ClassNotFoundException If the class represented by {@code className} does not exist
     */
    private static void addFieldType(String className, JsonObjectBuilder objectBuilder, Set<String> observedTypes) throws ClassNotFoundException {
        JsonArrayBuilder fieldBuilder = Json.createArrayBuilder();
        Class c = Class.forName(className);
        boolean isGroupType = AbstractGroup.class.isAssignableFrom(c);

        if (observedTypes.contains(c.getSimpleName())) {
            return;
        }

        for (Field s : getFields(c)) {
            JsonObjectBuilder questionBuilder = Json.createObjectBuilder();
            if (s.getGenericType().getTypeName().startsWith("java.util.List")) {
                if (s.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) s.getGenericType();
                    String typeName = pType.getActualTypeArguments()[0].getTypeName();
                    if (!observedTypes.contains(typeName)) {
                        observedTypes.add(typeName);
                        addFieldType(pType.getActualTypeArguments()[0].getTypeName(), objectBuilder, observedTypes);
                    }
                }
            }
            fieldBuilder.add(questionBuilder.add("type", s.getGenericType().toString().replace("class ", ""))
                    .add("name", (s.getName()))
                    .add("simpleTypeName", s.getType().getSimpleName())
                    .build());
        }
        objectBuilder.add(c.getSimpleName(), Json.createObjectBuilder()
                .add("fields", fieldBuilder.build())
                .add("isQuestionType", false)
                .add("isGroupType", isGroupType)
                .add("path", c.getName())
                .build());
    }

    /**
     * Looks up and returns the fields of a Class {@code c}, that are not part declared by Ebean or the Lingoturk
     * default variables
     *
     * @param c The class whose fields should be looked up.
     * @return The list of a all fields that fulfill the requirements
     */
    private static List<Field> getFields(Class c) {
        String[] unusedFields = new String[]{"experimentType", "dtype", "finder", "_idGetSet", "random", "questions", "id", "availability", "experimentID", "disabled", "subList", "maxWorkingTime", "maxParticipants", "databaseService"};
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

    /**
     * Checks if an experiment type name {@code name} already exists in the given directory {@code directory}
     *
     * @param directory The directory that will be looked through
     * @param name      The new experiment type name to test
     * @return Returns true iff the type name already exists.
     */
    private boolean typeNameExists(File directory, String name) {
        if (!directory.exists()) {
            throw new IllegalStateException("Directory \"" + directory.getPath() + "\" does not exist.");
        }

        for (File f : directory.listFiles()) {
            if (f.isDirectory() && f.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a new experiment type name is available. This is the case iff no directory with that
     * name exists in any of the app,css or javascript directories.
     *
     * @param name The new experiment type name to check
     * @return true, if the name is available. false otherwise
     */
    private boolean isTypeNameAvailable(String name) {
        for (File f : getDirectories(null)) {
            if (typeNameExists(f, name)) {
                return false;
            }
        }

        return true;
    }

    /**
     * The white-list of file extensions that represents files we are interested in. Any other file
     * types will be ignored by the copy function.
     */
    private static List<String> modifiableExtensions = Arrays.asList("java", "txt", "html", "config", "js", "conf", "properties", "sql");

    /**
     * Copies an existing experiment file from an old location {@code sourcepath} to a new one {@code destination}.
     * The name of the old type {@code oldName} will be replaced by the new type name {@code newName}.
     * IOException will be propagated from the FileUtils functions.
     *
     * @param sourcepath  The path to the old file
     * @param destination The path where the file should be copied to
     * @param oldName     The name of the old experiment type
     * @param newName     The name of the new experiment type
     * @throws IOException propagated form the internal FileUtils functions
     */
    private void copyExperimentFile(String sourcepath, String destination, String oldName, String newName) throws IOException {
        File sourceFile = new File(lingoturkConfig.getPathPrefix() + sourcepath);
        File destinationFile = new File(lingoturkConfig.getPathPrefix() + destination);

        if (modifiableExtensions.contains(FilenameUtils.getExtension(sourcepath))) {
            String fileContent = FileUtils.readFileToString(sourceFile, "UTF-8");
            fileContent = fileContent.replaceAll(oldName, newName);
            fileContent = fileContent.replace(oldName.toLowerCase(), newName.toLowerCase());
            fileContent = fileContent.replace("_TEMPLATEUNDERSCORE_", newName.toLowerCase());
            FileUtils.writeStringToFile(destinationFile, fileContent, StandardCharsets.UTF_8);
        } else {
            FileUtils.copyFile(sourceFile, destinationFile);
        }
    }

    /**
     * Copies an existing experiment directory recursively from an old location {@code sourcepath} to a new
     * one {@code destination}. The name of the old type {@code oldName} will be replaced by the new type
     * name {@code newName}. IOException will be propagated from the FileUtils functions. If the target
     * directory does not exist yet, it will be created automatically.
     *
     * @param sourcepath  The path to the old directory
     * @param destination The path where the directory should be copied to
     * @param oldName     The name of the old experiment type
     * @param newName     The name of the new experiment type
     * @throws IOException propagated form the internal FileUtils functions
     */
    private boolean copyExperimentDirectory(String sourcepath, String destination, String oldName, String newName) throws IOException {
        if (sourcepath.charAt(sourcepath.length() - 1) != '/') {
            sourcepath = sourcepath + '/';
        }

        if (destination.charAt(destination.length() - 1) != '/') {
            destination = destination + '/';
        }

        File sourceFile = new File(lingoturkConfig.getPathPrefix() + sourcepath);
        if (!sourceFile.exists()) {
            return false;
        }

        File destinationFile = new File(lingoturkConfig.getPathPrefix() + destination);
        if (!destinationFile.exists()) {
            destinationFile.mkdirs();
        }

        File[] sourceFiles = sourceFile.listFiles();
        if (sourceFiles == null) {
            return false;
        }

        for (File f : sourceFiles) {
            if (f.isDirectory()) {
                copyExperimentDirectory(sourcepath + f.getName(), destination + f.getName(), oldName, newName);
            } else {
                copyExperimentFile(sourcepath + f.getName(), destination + f.getName().replaceAll(oldName, newName), oldName, newName);
            }
        }

        return true;
    }

    /**
     * Creates a completely new experiment type with name {@code name} with fields {@code questionFields},
     * listType {@code listType} and the groupName {@code reusedGroupName}. If that is null a new group
     * will be created for this experiment type.
     *
     * @param name            The name of the new experiment type
     * @param questionFields  The fields of the new experiment type
     * @param listType        The listType of the new experimentType. If it is null, "MULTIPLE LISTS" will be used
     * @param reusedGroupName The name of the group to use. If it is null, a new one will be created
     */
    private void createExperimentType(String name, List<String[]> questionFields, String listType, String reusedGroupName) {
        try {
            File f = new File(lingoturkConfig.getPathPrefix() + "public/images/Experiments/" + name + "Experiment");
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    throw new IOException("Could not create directory");
                }
            }

            copyExperimentFile("template/templateExperimentRendering.html",
                    "app/views/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.html", "_TEMPLATE_", name);

            if (reusedGroupName == null) {
                copyExperimentFile("template/templateGroup.java",
                        "app/models/Groups/" + name + "Experiment/" + name + "Group.java", "_TEMPLATE_", name);
            }

            File fieldsFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + name + "Experiment/fields.json");
            setFields(fieldsFile, questionFields);

            copyExperimentFile("template/templateResultQuery.sql",
                    "app/models/Questions/" + name + "Experiment/resultQuery.sql", "_TEMPLATE_", name);

            copyExperimentFile("template/templateExperimentRendering.js",
                    "public/javascripts/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.js", "_TEMPLATE_", name);

            copyExperimentFile("template/templateExperimentRendering.css",
                    "public/stylesheets/ExperimentRendering/" + name + "Experiment/" + name + "Experiment_render.css", "_TEMPLATE_", name);

            File propertiesFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + name + "Experiment/experiment.properties");
            BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile));
            writer.write("questionType = models.Questions." + name + "Experiment." + name + "Question\n");
            if (reusedGroupName != null) {
                writer.write("groupType = models.Groups." + reusedGroupName + "\n");
            } else {
                writer.write("groupType = models.Groups." + name + "Experiment." + name + "Group");
            }
            if (listType != null) {
                writer.write("\nlistType = " + listType);
            } else {
                writer.write("\nlistType = MULTIPLE LISTS");
            }
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Error occured while copying experiment files: " + e.getMessage());
        }
    }

    /**
     * Copy an old experiment type {@code sourceExperiment} and give it the new name {@code targetExperiment}
     *
     * @param sourceExperiment The name of the old experiment type
     * @param targetExperiment The name of the new experiment type
     */
    private void copyExperimentType(String sourceExperiment, String targetExperiment) {
        try {
            String sourceExperimentName = sourceExperiment + "Experiment/";
            String targetExperimentName = targetExperiment + "Experiment/";

            // This directory may exist but it doesn't have to, copy only if it does
            File groupFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Groups/" + sourceExperimentName);
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
            copyExperimentDirectory("public/images/Experiments/" + sourceExperimentName, "public/images/Experiments/" + targetExperimentName, sourceExperiment, targetExperiment);
        } catch (IOException e) {
            throw new IllegalStateException("Error occured while copying experiment files: " + e.getMessage());
        }
    }

    /**
     * The function that will be invoked by the user if a new experiment type should be created.
     * The {@code request} contains all information such as name, type, or fields.
     *
     * @return Returns {@code ok} if no problem occurs. An {@code internalServerError} otherwise.
     */
    @SubjectPresent
    @BodyParser.Of(BodyParser.Json.class)
    public Result createNewExperimentType() {
        databaseService.backupDatabase();

        JsonNode json = request().body().asJson();

        String newTypeName = json.get("newTypeName").asText().trim();

        if (newTypeName.isEmpty()) {
            return internalServerError("Input name is empty!");
        }

        if (!StringUtils.isAlphanumeric(newTypeName)) {
            return internalServerError("Name is not alphanumeric!");
        }

        if (newTypeName.equals("template")) {
            return internalServerError("Name \"template\" is reserved!");
        }

        JsonNode sourceTypeListNode = json.get("sourceListType");
        String sourceListType = sourceTypeListNode != null ? sourceTypeListNode.asText() : null;

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
        for (Iterator<JsonNode> questionFieldIterator = json.get("questionFields").iterator(); questionFieldIterator.hasNext(); ) {
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
                createExperimentType(newTypeName, questionFields, sourceListType, null);
                break;
            case "REUSE":
                String sourceGroup = json.get("sourceGroupName").asText();
                try {
                    createExperimentType(newTypeName, questionFields, sourceListType, sourceGroup);
                } catch (IllegalStateException e) {
                    return internalServerError(e.getMessage());
                }
                break;
            default:
                return internalServerError("Unknown experiment type.");
        }

        // Register new experiment type
        experimentWatchService.addExperimentType(newTypeName + "Experiment");

        return ok();
    }

}