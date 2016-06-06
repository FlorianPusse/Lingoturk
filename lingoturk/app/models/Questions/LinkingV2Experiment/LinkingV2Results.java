package models.Questions.LinkingV2Experiment;

import play.db.ebean.Model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name="LinkingV2Results")
public class LinkingV2Results extends Model{

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
    int workingTimes;

    @Basic
    int lhs_script;

    @Basic
    int rhs_script;

    @Basic
    int lhs_slot;

    @Basic
    int rhs_slot;

    @Basic
    int result;
}
