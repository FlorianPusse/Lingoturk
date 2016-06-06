package models.Questions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Repository;
import org.apache.commons.beanutils.BeanUtils;
import play.db.ebean.Model;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.*;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;


@Entity
@Inheritance
@DiscriminatorValue("PartQuestion")
public abstract class PartQuestion extends PublishableQuestion {

    @JsonIgnore
    public void addUsedInPart(AbstractGroup p) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO Parts_contain_Questions(PartID, QuestionID) " +
                        "SELECT " + p.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM Parts_contain_Questions WHERE PartID=" + p.getId() + " AND QuestionID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public AbstractGroup getPartUsedIn() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT PartID FROM Parts_contain_Questions WHERE QuestionID=" + getId());

        AbstractGroup group = null;
        if (rs.next()) {
            int partID = rs.getInt("PartID");
            group = AbstractGroup.byId(partID);
        }

        return group;
    }

    private static class JsonRestriction extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember m) {
            return m.getDeclaringClass() == Model.class || super.hasIgnoreMarker(m);
        }
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JsonRestriction());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        try {
            return Json.createReader(new StringReader(mapper.writeValueAsString(this))).readObject();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        experimentID = experiment.getId();
        for (Iterator<Map.Entry<String, JsonNode>> fieldIterator = questionNode.fields(); fieldIterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = fieldIterator.next();

            if (entry.getKey().equals("type")) {
                continue;
            }

            try {
                Field field = getClass().getField(entry.getKey());
                if (field.getType().equals(Integer.TYPE) || field.getType().equals(Integer.class)) {
                    field.setInt(this, entry.getValue().asInt());
                } else if (field.getType().equals(String.class)) {
                    BeanUtils.setProperty(this, entry.getKey(), entry.getValue().asText());
                } else if (field.getType().equals(Boolean.TYPE) || field.getType().equals(Boolean.class)) {
                    field.setBoolean(this, entry.getValue().asBoolean());
                } else {
                    throw new RuntimeException("Unknown field type");
                }
                field.get(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("Unknown field name: " + entry.getKey());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
