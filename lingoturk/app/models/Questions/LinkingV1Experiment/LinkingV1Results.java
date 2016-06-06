package models.Questions.LinkingV1Experiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="LinkingV1Results")
public class LinkingV1Results extends Model{

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
    @Column(name = "lhs_script")
    int lhs_script;

    @Basic
    @Column(name = "rhs_script")
    int rhs_script;

    @Basic
    @Column(name = "lhs_item")
    int lhs_item;

    @Basic
    @Column(name = "rhs_item")
    int rhs_item;

    @Basic
    @Column(name = "before")
    int before;

    @Basic
    @Column(name = "after")
    int after;

    @Basic
    @Column(name = "noLinkingPossible")
    boolean noLinkingPossible;
}
