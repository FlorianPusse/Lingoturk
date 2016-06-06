package models.Questions.NewDiscourseConnectivesExperiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="NewDiscourseConnectivesResults")
public class NewDiscourseConnectivesResults extends Model{

    @Id
    int id;

    @Basic
    @Column(name = "assignmentId")
    String assignmentId;

    @Basic
    @Column(name = "hitId")
    String hitId;

    @Basic
    @Column(name = "workerId")
    String workerId;

    @Basic
    @Column(name = "origin")
    String origin;

    @Basic
    @Column(name = "timestamp")
    Date timestamp;

    @Basic
    @Column(name = "partId")
    int partId;

    @Basic
    @Column(name = "questionId")
    int questionId;

    @Basic
    @Column(name = "connective1")
    String connective1;

    @Basic
    @Column(name = "connective2")
    String connective2;

    @Basic
    @Column(name = "manualAnswer1")
    String manualAnswer1;

    @Basic
    @Column(name = "manualAnswer2")
    String manualAnswer2;

}
