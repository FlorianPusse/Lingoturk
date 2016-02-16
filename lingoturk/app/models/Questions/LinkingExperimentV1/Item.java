package models.Questions.LinkingExperimentV1;


import play.db.ebean.Model;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name="LinkingItem")
public class Item extends Model {

    @Id
    int id;

    @Basic
    String h;

    @Basic
    String head;

    @Basic
    String original;

    @Basic
    int slot;

    @Basic
    String text;

    @Column(name="script_questionId")
    @ManyToOne
    Script script;

    @OneToMany(cascade = CascadeType.ALL)
    List<Pair> pairs = new LinkedList<>();

    @OneToMany(cascade = CascadeType.ALL)
    List<PTCP> ptcps = new LinkedList<>();

    private static Finder<Integer,Item> finder = new Finder<>(Integer.class,Item.class);

    public Item(String h, String pair, String text, String slot, String original, String head) {
        this.h = h;

        if(!pair.equals("")){
            String[] pairs = pair.split(";");
            for (String p : pairs){
                if(p.equals("none")){
                    this.pairs.add(new Pair(true));
                }else{
                    this.pairs.add(new Pair(p.split(",")[0],p.split(",")[1]));
                }
            }
        }

        this.text = text;
        this.slot = Integer.parseInt(slot);
        this.original = original;
        this.head = head;
    }

    @Override
    public String toString(){
        return "item : "
                + "\n\th : " + h
                + "\n\thead : " + head
                + "\n\toriginal : " + original
                + "\n\tslot : " + slot
                + "\n\ttext : " + text
                + "\n\tpairs : " + pairs
                + "\n\tptcps: " + ptcps;
    }

    public void addPTCP(PTCP ptcp){
        ptcps.add(ptcp);
    }

    public JsonObject returnJSON() throws SQLException {
        JsonArrayBuilder pairBuilder = Json.createArrayBuilder();
        for(Pair p : pairs){
            pairBuilder.add(p.toJson());
        }

        JsonArrayBuilder ptcpBuilder = Json.createArrayBuilder();
        for(PTCP ptcp: ptcps){
            ptcpBuilder.add(ptcp.toJson());
        }

        return Json.createObjectBuilder().add("isActive",h.isEmpty() ? false : true)
                .add("h",h)
                .add("id",id)
                .add("head",head)
                .add("original",original)
                .add("slot",slot)
                .add("text",text)
                .add("pairs",pairBuilder.build())
                .add("ptcps",ptcpBuilder.build())
                .build();
    }

    public List<Pair> getPairs(){
        return pairs;
    }

    public String getH(){
        return h;
    }

    public int getSlot(){
        return slot;
    }

    public static class ItemSlotComparator implements Comparator<Item>{
        @Override
        public int compare(Item o1, Item o2) {
            return Integer.compare(o1.slot,o2.slot);
        }
    }

    static Item byId(int id){
        return finder.byId(id);
    }

}
