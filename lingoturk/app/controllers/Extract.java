package controllers;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import models.LingoExpModel;

import org.apache.commons.io.FileUtils;


// Imports für Play
import play.mvc.*;

import java.io.*;
import java.sql.*;
import java.util.*;

public class Extract extends Controller{


    /**
     * Renders a page, containing a list of all experiments available on AMT
     * @return the rendered page
     */
    @Security.Authenticated(Secured.class)
    public static Result extract() throws SQLException {
        List<PublishedExperiment> hits_sandbox = listPublishedExperiments("sandbox");
        List<PublishedExperiment> hits_mTurk = listPublishedExperiments("amt");

        return ok(views.html.ExtractResults.extract.render(hits_mTurk,hits_sandbox));
    }

    /**
     * Returns a .csv file, that contains all data available about an experiment
     * @return the Result containing the .csv - file
     */
    @Security.Authenticated(Secured.class)
    public static Result result(int id) {
        LingoExpModel expModel = LingoExpModel.byId(id);
        String experimentType = expModel.getExperimentType();

        PreparedStatement statement = null;
        try {
            File file = File.createTempFile("experiment_" + id + "_results", ".csv");
            FileWriter fileWriter = new FileWriter(file);
            CSVWriter writer = new CSVWriter(fileWriter);

            File queryFile = new File("app/models/Questions/" + experimentType + "/resultQuery.sql");

            if(!queryFile.exists() || queryFile.isDirectory()){
                return internalServerError("Query file does not exist for experiment type: " + experimentType);
            }else{
                String queryString = FileUtils.readFileToString(queryFile);
                statement = DatabaseController.getConnection().prepareStatement(queryString);
                ResultSet resultSet = statement.executeQuery();
                writer.writeAll(resultSet,true);
                statement.close();
            }

            fileWriter.close();
            writer.close();
            return ok(file);
        } catch (IOException exception) {
            return internalServerError("Can't create temporary file.");
        } catch (SQLException e) {
            return internalServerError("SQL query throws exception: \n" + e.getMessage());
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                        // TODO: LOG SOMEWHERE
                        e.printStackTrace();
                }
            }
        }
    }

    public static List<HIT> getHITs(int publishID, RequesterService service) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT mTurkID FROM QuestionPublishedAs WHERE publishedId = ?");
        statement.setInt(1, publishID);
        ResultSet rs = statement.executeQuery();

        List<HIT> results = new LinkedList<>();
        while(rs.next()){
            String hitID = rs.getString("mturkID");
            HIT hit = null;
            try{
                hit = service.getHIT(hitID);
            }catch(ServiceException e){
                ok(views.html.errorpage.render("HIT does not exist or cannot establish connection\n" + e.getMessage(), "/"));
            }
            results.add(hit);
        }

        return results;
    }

    /**
     * Lists all HITs saved on AMT
     * @return A list of all HITs saved on AMT or null if an error occurred
     */
    public static List<PublishedExperiment> listPublishedExperiments(String destination) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM LingoExpModelPublishedAs WHERE Destination = ?");
        statement.setString(1,destination);
        ResultSet rs = statement.executeQuery();

        List<PublishedExperiment> results = new LinkedList<>();
        while(rs.next()){
            results.add(new PublishedExperiment(rs.getInt("publishID"),rs.getInt("lingoExpModelID"),rs.getTimestamp("timestamp"),rs.getLong("lifetime"),rs.getString("url")));
        }

        return results;
    }

    public static class PublishedExperiment {
        private int publishID;
        private LingoExpModel lingoExpModel;
        private Timestamp timestamp;
        private Timestamp lifetime;
        private String url;

        PublishedExperiment(int publishID, int lingoExpModelID,Timestamp timestamp, long lifetime,String url) throws SQLException {
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
