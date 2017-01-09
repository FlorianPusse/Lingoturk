package controllers;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.LingoExpModel;

import org.apache.commons.io.FileUtils;

// Imports für Play
import play.mvc.*;

import java.io.*;
import java.sql.*;
import java.util.*;

public class Extract extends Controller {


    /**
     * Renders a page, containing a list of all experiments available on AMT
     *
     * @return the rendered page
     */
    @Security.Authenticated(Secured.class)
    public static Result extract() throws SQLException {
        List<PublishedExperiment> hits_sandbox = listPublishedExperiments("sandbox");
        List<PublishedExperiment> hits_mTurk = listPublishedExperiments("amt");

        return ok(views.html.ExtractResults.extract.render(hits_mTurk, hits_sandbox));
    }

    /**
     * Returns a .csv file, that contains all data available about an experiment
     *
     * @return the Result containing the .csv - file
     */
    @Security.Authenticated(Secured.class)
    public static Result result(int id) {
        LingoExpModel expModel = LingoExpModel.byId(id);
        if(expModel == null){
            return internalServerError("LingoExpModelId does not exist.");
        }

        String name = expModel.getExperimentType();
        name = name.substring(0, name.length() - "Experiment".length());
        return ok(views.html.ExtractResults.extractionInterface.render(id, name));
    }

    public static String getStoredQuery(String experimentType) throws IOException {
        File queryFile = new File("app/models/Questions/" + experimentType + "Experiment/resultQuery.sql");

        if (!queryFile.exists() || queryFile.isDirectory()) {
            return null;
        } else {
            return FileUtils.readFileToString(queryFile);
        }
    }

    public static void saveStoredQuery(String experimentType, String query) throws IOException {
        File queryFile = new File("app/models/Questions/" + experimentType + "Experiment/resultQuery.sql");
        FileWriter fw = new FileWriter(queryFile);
        fw.write(query);
        fw.close();
    }

    private static List<String> nodeToList(JsonNode node){
        List<String> result = new LinkedList<>();
        Iterator<JsonNode> nodeIterator = node.iterator();
        while(nodeIterator.hasNext()){
            JsonNode val = nodeIterator.next();
            result.add(val.asText());
        }
        return result;
    }

    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    @Security.Authenticated(Secured.class)
    public static Result loadResults(String d) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(d);
        int expId = json.get("expId").asInt();
        String experimentType = json.get("experimentType").asText();
        boolean useStored = json.get("useStored").asBoolean();
        List<String> usedFields = nodeToList(json.get("usedFields"));
        List<String> orderBy = nodeToList(json.get("orderBy"));
        List<String> allFields = nodeToList(json.get("allFields"));
        String query = null;

        PreparedStatement statement = null;
        File file = null;

        try {
            if (useStored) {
                query = getStoredQuery(experimentType);
            }else{
                query = "SELECT " + (!usedFields.isEmpty() ? String.join(", ", usedFields) : String.join(", ", allFields)) + " FROM (\n\t" + "SELECT * FROM " + experimentType + "Results\n\tLEFT OUTER JOIN Questions USING (QuestionId)\n\tLEFT OUTER JOIN Groups USING (PartId)\n) as tmp\nWHERE LingoExpModelId = " + expId;
                if(!orderBy.isEmpty()){
                    query += "\nORDER BY " + String.join(", ", orderBy);
                }
            }

            file = File.createTempFile("experiment_" + expId + "_results", ".csv");
            file.deleteOnExit();
            FileWriter fileWriter = new FileWriter(file);
            CSVWriter writer = new CSVWriter(fileWriter);

            statement = DatabaseController.getConnection().prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            writer.writeAll(resultSet, true);
            writer.close();

            if (!useStored) {
                saveStoredQuery(experimentType, query);
            }
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

        return ok(file);
    }

    public static List<HIT> getHITs(int publishID, RequesterService service) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT mTurkID FROM QuestionPublishedAs WHERE publishedId = ?");
        statement.setInt(1, publishID);
        ResultSet rs = statement.executeQuery();

        List<HIT> results = new LinkedList<>();
        while (rs.next()) {
            String hitID = rs.getString("mturkID");
            HIT hit = null;
            try {
                hit = service.getHIT(hitID);
            } catch (ServiceException e) {
                ok(views.html.errorpage.render("HIT does not exist or cannot establish connection\n" + e.getMessage(), "/"));
            }
            results.add(hit);
        }

        return results;
    }

    /**
     * Lists all HITs saved on AMT
     *
     * @return A list of all HITs saved on AMT or null if an error occurred
     */
    public static List<PublishedExperiment> listPublishedExperiments(String destination) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM LingoExpModelPublishedAs WHERE Destination = ?");
        statement.setString(1, destination);
        ResultSet rs = statement.executeQuery();

        List<PublishedExperiment> results = new LinkedList<>();
        while (rs.next()) {
            results.add(new PublishedExperiment(rs.getInt("publishID"), rs.getInt("lingoExpModelID"), rs.getTimestamp("timestamp"), rs.getLong("lifetime"), rs.getString("url")));
        }

        return results;
    }

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

}
