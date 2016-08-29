package models;

import play.db.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name="ParticipantStatistics")
public class ParticipantStatistics extends Model {

    @Id
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
