package models.Groups._TEMPLATE_Experiment;


import com.amazonaws.mturk.service.axis.RequesterService;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.SQLException;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("_TEMPLATE_Experiment._TEMPLATE_Group")
public class _TEMPLATE_Group extends AbstractGroup {

    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {
        return null;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return null;
    }

}