package models;

import io.ebean.Model;

import javax.persistence.*;

/**
 * Represents feedback that might be collected about an experiment after
 * a worker has completed it. (Feedback collection is turned off by default)
 */
@Entity
@Table(name = "ParticipantFeedback")
public class ParticipantFeedback extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participantfeedback_seq")
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
