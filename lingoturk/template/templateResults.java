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

    
}
