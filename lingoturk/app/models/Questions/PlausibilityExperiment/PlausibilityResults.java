package models.Questions.PlausibilityExperiment;

import play.db.ebean.Model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name="PlausibilityResults")
public class PlausibilityResults extends Model{

    @Id
    int id;

    @Basic
    String assignmentId;

    @Basic
    String hitId;

    @Basic
    String workerId;

    @Basic
    String origin;

    @Basic
    Date timestamp;

    @Basic
    int partId;

    @Basic
    int questionId;

    @Basic
    int answer;
}
