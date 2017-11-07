package controllers;

import akka.util.ByteString;
import be.objectify.deadbolt.java.actions.SubjectPresent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import models.LingoExpModel;
import org.apache.commons.io.FileUtils;
import play.http.HttpEntity;
import play.mvc.Controller;
import play.mvc.ResponseHeader;
import play.mvc.Result;
import services.DatabaseService;
import services.LingoturkConfig;

import javax.inject.Inject;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/***
 * Controller handling handling requests related to the extraction of experiment results
 */
public class ExtractionController extends Controller {

    private final DatabaseService databaseService;
    private final LingoturkConfig lingoturkConfig;

    @Inject
    public ExtractionController(DatabaseService databaseService, LingoturkConfig lingoturkConfig) {
        this.databaseService = databaseService;
        this.lingoturkConfig = lingoturkConfig;
    }

    /**
     * A Java wrapper for the published experiment table in the database.
     */
    public static class PublishedExperiment {
        private int publishID;
        private LingoExpModel lingoExpModel;
        private Timestamp timestamp;
        private Timestamp lifetime;
        private String url;

        PublishedExperiment(int publishID, int lingoExpModelID, Timestamp timestamp, long lifetime, String url) throws SQLException {
            this.publishID = publishID;
            this.lingoExpModel = LingoExpModel.byId(lingoExpModelID);
            this.timestamp = timestamp;
            this.lifetime = new Timestamp(timestamp.getTime() + lifetime);
            this.url = url;
        }

        public int getPublishID() {
            return publishID;
        }

        public LingoExpModel getLingoExpModel() {
            return lingoExpModel;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public Timestamp getLifetime() {
            return lifetime;
        }

        public String getURL() {
            return url;
        }
    }

    /**
     * Lists all HITs saved on AMT
     *
     * @return A list of all HITs saved on AMT or null if an error occurred
     */
    public List<PublishedExperiment> listPublishedExperiments(String destination) throws SQLException {
        PreparedStatement statement = databaseService.getConnection().prepareStatement("SELECT * FROM LingoExpModelPublishedAs WHERE Destination = ?");
        statement.setString(1, destination);
        ResultSet rs = statement.executeQuery();

        List<PublishedExperiment> results = new LinkedList<>();
        while (rs.next()) {
            results.add(new PublishedExperiment(rs.getInt("publishID"), rs.getInt("lingoExpModelID"), rs.getTimestamp("timestamp"), rs.getLong("lifetime"), rs.getString("url")));
        }

        return results;
    }

    /**
     * Renders a page, containing a list of all experiments available on AMT
     *
     * @return the rendered page
     */
    @SubjectPresent
    public Result extract() throws SQLException {
        List<PublishedExperiment> hits_sandbox = listPublishedExperiments("sandbox");
        List<PublishedExperiment> hits_mTurk = listPublishedExperiments("amt");

        return ok(views.html.ExtractResults.extract.render(hits_mTurk, hits_sandbox));
    }

    /**
     * Renders the result extraction interface
     *
     * @return the Result extraction interface
     */
    @SubjectPresent
    public Result result(int id) {
        LingoExpModel expModel = LingoExpModel.byId(id);
        if (expModel == null) {
            return internalServerError("LingoExpModelId does not exist.");
        }

        String name = expModel.getExperimentType();
        name = name.substring(0, name.length() - "Experiment".length());
        return ok(views.html.ExtractResults.extractionInterface.render(id, name));
    }

    /**
     * Retrieves the last executed query for the given experiment type.
     *
     * @param experimentType The name of the experiment type
     * @return The last executed query for this experiment type
     * @throws IOException If the file could not be read
     */
    private String getStoredQuery(String experimentType) throws IOException {
        File queryFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + experimentType + "Experiment/resultQuery.sql");

        if (!queryFile.exists() || queryFile.isDirectory()) {
            return null;
        } else {
            return FileUtils.readFileToString(queryFile, "UTF-8");
        }
    }

    /**
     * Stores the last executed query for the given experiment type.
     *
     * @param experimentType The name of the experiment type
     * @param query          The last executed query for this experiment type
     * @throws IOException If the file could not be stored
     */
    private void saveStoredQuery(String experimentType, String query) throws IOException {
        File queryFile = new File(lingoturkConfig.getPathPrefix() + "app/models/Questions/" + experimentType + "Experiment/resultQuery.sql");
        FileWriter fw = new FileWriter(queryFile);
        fw.write(query);
        fw.close();
    }

    /**
     * Converts a JsonNode (representing an JsonArray) to a List representation
     *
     * @param node The node representing an JsonArray
     * @return The list representation of the node
     */
    private static List<String> nodeToList(JsonNode node) {
        List<String> result = new LinkedList<>();
        Iterator<JsonNode> nodeIterator = node.iterator();
        while (nodeIterator.hasNext()) {
            JsonNode val = nodeIterator.next();
            result.add(val.asText());
        }
        return result;
    }

    /**
     * Returns a .csv file, that contains all results stored for an experiment instance {@code id}.
     *
     * @return the Result containing the .csv - file
     */
    @SubjectPresent
    public Result loadResults(String d) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(d);
        int expId = json.get("expId").asInt();
        String experimentType = json.get("experimentType").asText();
        boolean useStored = json.get("useStored").asBoolean();
        List<String> usedFields = nodeToList(json.get("usedFields"));
        List<String> orderBy = nodeToList(json.get("orderBy"));
        List<String> allFields = nodeToList(json.get("allFields"));
        String query;

        if (usedFields.isEmpty()) {
            usedFields = allFields;
        }

        for (int i = 0; i < usedFields.size(); ++i) {
            String field = usedFields.get(i);
            if (field.startsWith(experimentType + "_")) {
                String fieldName = field.split("_")[1];
                usedFields.set(i, "(data->>'" + fieldName + "\') as " + fieldName);
            }
            if (field.equals("answer")) {
                usedFields.set(i, "answer::TEXT");
            }
        }

        PreparedStatement statement = null;

        try {
            if (useStored) {
                query = getStoredQuery(experimentType);
            } else {
                query = "SELECT " + String.join(", ", usedFields) + " FROM (\n\t" + "(SELECT * FROM Results WHERE experimentType='" + experimentType + "Experiment') as tmp1\n\tLEFT OUTER JOIN Questions USING (QuestionId)\n\tLEFT OUTER JOIN Groups USING (PartId)\n) as tmp\nWHERE LingoExpModelId = " + expId;
                if (!orderBy.isEmpty()) {
                    query += "\nORDER BY " + String.join(", ", orderBy);
                }
            }

            Writer fileWriter = new StringWriter();
            CSVWriter writer = new CSVWriter(fileWriter);

            statement = databaseService.getConnection().prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            writer.writeAll(resultSet, true);
            writer.close();
            fileWriter.flush();

            if (!useStored) {
                saveStoredQuery(experimentType, query);
            }

            HashMap<String, String> map = new HashMap<>();
            map.put("Content-Disposition", "attachment; filename=\"" + "experiment_" + expId + "_results.csv" + "\"");

            return new Result(
                    new ResponseHeader(200, map),
                    new HttpEntity.Strict(ByteString.fromString(fileWriter.toString()), Optional.of("text/plain"))
            );
        } catch (IOException exception) {
            exception.printStackTrace();
            return internalServerError("Can't create temporary file.");
        } catch (SQLException e) {
            return internalServerError("SQL query throws exception: \n" + e.getMessage());
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
