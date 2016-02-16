package models.Questions;

import models.Repository;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.mvc.Result;

import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Inheritance
@DiscriminatorColumn(length=30)
@Table(name="Questions")
@MappedSuperclass
public abstract class Question extends Model {

    @Id
    @Column(name="QuestionID")
    protected int id;

    @Basic
    @Constraints.Required
    @Column(name="LingoExpModelId")
    protected int experimentID;

    @Column(name="Availability", columnDefinition = "integer default 1")
    protected int availability = 1;

    private static Finder<Integer,Question> finder = new Finder<>(Integer.class,Question.class);

    public static Question byId(int id) {
       return finder.byId(id);
    }

    public synchronized Question getIfAvailable() throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT * FROM Questions WHERE QuestionID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        if (rs.next()) {
            this.availability = rs.getInt("Availability");
        }

        if (availability > 0){
            this.availability-- ;
            statement = Repository.getConnection().prepareStatement("UPDATE Questions SET Availability = ? WHERE QuestionID = ?");
            statement.setInt(1,this.availability);
            statement.setInt(2,this.getId());
            statement.execute();
            return this;
        }

        return null;
    }

    public int getExperimentID() {
        return experimentID;
    }

    public abstract void writeResults(JsonNode resultNode) throws SQLException;

    public int getId() {
        return id;
    }

    public abstract JsonObject returnJSON() throws SQLException;

    public abstract Result render(String assignmentId, String hitId, String workerId, String turkSubmitTo, String additionalExplanations);
}
