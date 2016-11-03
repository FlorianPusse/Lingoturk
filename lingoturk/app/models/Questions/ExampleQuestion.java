package models.Questions;

import models.LingoExpModel;
import controllers.DatabaseController;
import play.mvc.Result;
import play.db.ebean.Model;

import javax.json.JsonObject;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ExampleQuestion{

    static Model.Finder<Integer, ExampleQuestion> finder = new Model.Finder<>(Integer.class, ExampleQuestion.class);

    /**
     * returns the example question's id
     *
     * @return the id
     */
    public int getId();

    public JsonObject returnJSON() throws SQLException;

    public Result render();

    public static class EQ {
        public static ExampleQuestion byId(int id) {
            return finder.byId(id);
        }

        public static void addUsedInExperiments(LingoExpModel lingoExpModel, ExampleQuestion exampleQuestion) throws SQLException {
            PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                    "INSERT INTO LingoExpModels_contain_ExampleQuestions(LingoExpModelID,QuestionID) " +
                            "SELECT " + lingoExpModel.getId() + ", " + exampleQuestion.getId() +
                            " WHERE NOT EXISTS (" +
                            "SELECT * FROM LingoExpModels_contain_ExampleQuestions WHERE LingoExpModelID=" + lingoExpModel.getId() + " AND QuestionID=" + exampleQuestion.getId() +
                            ")");

            statement.execute();
        }
    }

}
