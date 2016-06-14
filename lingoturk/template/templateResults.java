package models.Questions._TEMPLATE_Experiment;

import play.db.ebean.Model;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="_TEMPLATE_Results")
public class _TEMPLATE_Results extends Model{

    @Id
    int id;

    @Basic
    @Column(name = "assignmentId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    String assignmentId;

    @Basic
    @Column(name = "hitId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    String hitId;

    @Basic
    @Column(name = "workerId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    String workerId;

    @Basic
    @Column(name = "origin", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    String origin;

    @Basic
    @Column(name = "timestamp", columnDefinition = "timestamp default now()")
    Date timestamp;

    @Basic
    @Column(name = "partId", columnDefinition = "integer default -1")
    int partId;

    @Basic
    @Column(name = "questionId", columnDefinition = "integer default -1")
    int questionId;

    @Basic
    @Column(name = "answer", columnDefinition = "TEXT")
    String answer;

}
