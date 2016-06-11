package models.Questions.SentenceCompletionExperiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="SentenceCompletionResults")
public class SentenceCompletionResults extends Model{

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
    @Column(columnDefinition = "TEXT", name = "answer")
    String answer;
}
