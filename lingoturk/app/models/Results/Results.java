package models.Results;

import io.ebean.Model;
import io.ebean.annotation.DbJson;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "Results")
public class Results extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "results_seq")
    int id;

    @Basic
    @Column(name = "experimentType", columnDefinition = "varchar(100)", nullable = false)
    public String experimentType;

    @Basic
    @Column(name = "assignmentId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    public String assignmentId;

    @Basic
    @Column(name = "hitId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    public String hitId;

    @Basic
    @Column(name = "workerId", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    public String workerId;

    @Basic
    @Column(name = "origin", columnDefinition = "varchar(255) default 'NOT AVAILABLE'")
    public String origin;

    @Basic
    @Column(name = "timestamp", columnDefinition = "timestamp default now()")
    public Date timestamp;

    @Basic
    @Column(name = "partId", columnDefinition = "integer default -1")
    public Integer partId;

    @Basic
    @Column(name = "questionId", columnDefinition = "integer default -1")
    public Integer questionId;

    @DbJson
    public Map<String, Object> answer;
}
