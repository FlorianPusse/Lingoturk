package models.Results;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by anon on 2/28/15.
 */
public class AssignmentResult {

        public String getWorkerID() {
            return workerID;
        }

        public String getCategory() {
            return category;
        }

        public String[] getManualAnswer() {
            return manualAnswer;
        }

        public String[] getNotRelevant() {
            return notRelevant;
        }

        public String[] getValidConnectives() {
            return validConnectives;
        }

        public long getWorkingTime() {
            return workingTime;
        }

        public String[] getCantDecide() {
            return cantDecide;
        }

        public String assignmentID;
        public String workerID;
        public String category;
        public String[] manualAnswer;
        public String[] notRelevant;
        public String[] validConnectives;
        public String[] cantDecide;
        public long workingTime;

        public AssignmentResult(String assignmentID, String workerID, String category, String[] manualAnswer, String[] notRelevant, String[] validConnectives, String[] cantDecide, long workingTime){
            this.assignmentID = assignmentID;
            this.workerID = workerID;
            this.category = category;
            this.manualAnswer = manualAnswer;
            this.notRelevant = notRelevant;
            this.validConnectives = validConnectives;
            this.cantDecide = cantDecide;
            this.workingTime = workingTime;
        }

        @Override
        public String toString(){
            return  assignmentID + "\t" +
                    workerID + "\t" +
                    workingTime + "\t" +
                    category + "\t" +
                    StringUtils.join(manualAnswer, ":") + "\t" +
                    StringUtils.join(notRelevant,":") + "\t" +
                    StringUtils.join(validConnectives,":") + "\t" +
                    StringUtils.join(cantDecide,":") + "\n";
        }
}
