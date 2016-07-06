package models.Questions.LinkingV1Experiment;

import play.db.ebean.Model;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Pair extends Model{

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    @Id
    public int id;

    @Basic
    public int sentenceId;

    @Basic
    public int slot;

    @Basic
    public int source;

    @Basic
    public boolean none;

    public Pair(String sentenceId,String slot){
        this.sentenceId = Integer.parseInt(sentenceId.trim());
        this.slot = Integer.parseInt(slot.trim());
        this.none = false;
    }

    public Pair(String source, String sentenceId,String slot){
        this.sentenceId = Integer.parseInt(sentenceId.trim());
        this.slot = Integer.parseInt(slot.trim());
        this.source = Integer.parseInt(source.trim());
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