package models.Questions.PictureNamingExperiment;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="PictureNamingResults")
public class PictureNamingResults extends Model{

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
    int chunkId;

    @Basic
    int pictureId;

    @Basic
    @Column(columnDefinition = "TEXT")
    String answer;
}
