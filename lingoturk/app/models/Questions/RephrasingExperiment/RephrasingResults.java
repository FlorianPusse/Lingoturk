package models.Questions.RephrasingExperiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="RephrasingResults")
public class RephrasingResults extends Model{

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
    int choice1;

    @Basic
    int choice2;

    @Basic
    @Column(columnDefinition = "TEXT")
    String answer1;

    @Basic
    @Column(columnDefinition = "TEXT")
    String answer2;

    @Basic
    int readingTime1;

    @Basic
    int readingTime2;
}
