package models.Questions.LinkingExperimentV1;

import play.db.ebean.Model;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Pair extends Model{

    @Id
    int id;

    @Basic
    int sentenceId;

    @Basic
    int slot;

    @Basic
    boolean none;

    public Pair(String sentenceId,String slot){
        this.sentenceId = Integer.parseInt(sentenceId.trim());
        this.slot = Integer.parseInt(slot.trim());
        this.none = false;
    }

    public Pair(boolean none){
        this.none = none;
    }

    public JsonObject toJson(){
        return Json.createObjectBuilder().add("sentenceId",sentenceId).add("slot",slot).build();
    }

    public String toString(){
        return "[" + sentenceId + ", " + slot + "]";
    }

    public int getSentenceId() {
        return sentenceId;
    }

    public int getSlot() {
        return slot;
    }
}