package models;

import io.ebean.Model;

import javax.persistence.*;

/**
 * Represents statistics that might be collected about a worker.
 * (Statistics collection is turned off by default)
 */
@Entity
@Table(name = "ParticipantStatistics")
public class ParticipantStatistics extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participantstatistics_seq")
    int id;

    @Basic
    @Column(name = "workerId")
    String workerId;

    @Basic
    @Column(name = "origin")
    String origin;

    @Basic
    @Column(name = "expId")
    String expId;

    @Basic
    @Column(name = "statistics", columnDefinition = "TEXT")
    String statistics;
}
