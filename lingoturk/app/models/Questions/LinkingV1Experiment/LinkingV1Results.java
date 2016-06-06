package models.Questions.LinkingV1Experiment;

import play.db.ebean.Model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name="LinkingV1Results")
public class LinkingV1Results extends Model{

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
    int lhs_script;

    @Basic
    int rhs_script;

    @Basic
    int lhs_item;

    @Basic
    int rhs_item;

    @Basic
    int before;

    @Basic
    int after;

    @Basic
    boolean noLinkingPossible;
}
