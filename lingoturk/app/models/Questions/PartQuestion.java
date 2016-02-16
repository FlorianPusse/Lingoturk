package models.Questions;

import com.fasterxml.jackson.databind.JsonNode;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Repository;

import javax.persistence.Basic;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


@Entity
@Inheritance
@DiscriminatorValue("PartQuestion")
public abstract class PartQuestion extends PublishableQuestion {

    @Basic
    protected String innerID;

    public void addUsedInPart(AbstractGroup p) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO Parts_contain_Questions(PartID, QuestionID) " +
                        "SELECT " + p.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM Parts_contain_Questions WHERE PartID=" + p.getId() + " AND QuestionID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public AbstractGroup getPartUsedIn() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT PartID FROM Parts_contain_Questions WHERE QuestionID=" + getId());

        AbstractGroup group = null;
        if(rs.next()){
            int partID = rs.getInt("PartID");
            group = AbstractGroup.byId(partID);
        }

        return group;
    }

    public abstract void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException;
}
