package models.Questions.LinkingV2Experiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="LinkingV2Results")
public class LinkingV2Results extends Model{

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
    @Column(name = "workingTimes")
    int workingTimes;

    @Basic
    @Column(name = "lhs_script")
    int lhs_script;

    @Basic
    @Column(name = "rhs_script")
    int rhs_script;

    @Basic
    @Column(name = "lhs_slot")
    int lhs_slot;

    @Basic
    @Column(name = "rhs_slot")
    int rhs_slot;

    @Basic
    @Column(name = "result")
    String result;
}
