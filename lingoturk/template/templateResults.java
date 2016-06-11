package models.Questions._TEMPLATE_Experiment;

import play.db.ebean.Model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name="_TEMPLATE_Results")
public class _TEMPLATE_Results extends Model{

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

}
