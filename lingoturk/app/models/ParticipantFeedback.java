package models;

import play.db.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name="ParticipantFeedback")
public class ParticipantFeedback extends Model {

    @Id
    int id;

    @Basic
    @Column(name = "workerId")
    String workerId;

    @Basic
    @Column(name = "expId")
    String expId;

    @Basic
    @Column(name = "feedback")
    String feedback;

}
