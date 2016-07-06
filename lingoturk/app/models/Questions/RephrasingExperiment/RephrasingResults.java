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
    @Column(name = "timestamp", columnDefinition = "timestamp default now()")
    Date timestamp;

    @Basic
    @Column(name = "partId")
    int partId;

    @Basic
    @Column(name = "questionId")
    int questionId;

    @Basic
    @Column(name = "choice1")
    boolean choice1;

    @Basic
    @Column(name = "choice2")
    boolean choice2;

    @Basic
    @Column(columnDefinition = "TEXT", name = "answer1")
    String answer1;

    @Basic
    @Column(columnDefinition = "TEXT", name = "answer2")
    String answer2;

    @Basic
    @Column(name = "readingTime1")
    int readingTime1;

    @Basic
    @Column(name = "readingTime2")
    int readingTime2;

    @Basic
    @Column(name = "age")
    int age;

    @Basic
    @Column(name = "startedLearning")
    int startedLearning;
}
