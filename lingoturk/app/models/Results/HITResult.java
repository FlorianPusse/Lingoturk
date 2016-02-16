package models.Results;

import java.util.List;

/**
 * Created by anon on 2/28/15.
 */
public class HITResult {

        private String hitID;
        private int questionID;
        private String connector;
        private String sentence1;
        private String sentence2;
        private String relation;
        private List<AssignmentResult> assignmentResults;

        public HITResult(String hitID,int questionID,String relation,String connector,String sentence1,String sentence2, List<AssignmentResult> assignmentResults) {
            this.hitID = hitID;
            this.questionID = questionID;
            this.relation = relation;
            this.connector = connector;
            this.sentence1 = '"' + sentence1.replaceAll("\"","\"\"") + '"';
            this.sentence2 = '"' + sentence2.replaceAll("\"","\"\"") + '"';
            this.assignmentResults = assignmentResults;
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            for(AssignmentResult assignmentResult : assignmentResults){
                sb.append(hitID + "\t" + questionID  + "\t" + this.relation+ "\t" + this.connector + "\t" + assignmentResult.toString());
            }
            return sb.toString();
        }

        public String getHitID() {
            return hitID;
        }

        public String getSentence1() {
            return sentence1;
        }

        public String getSentence2() {
            return sentence2;
        }

        public List<AssignmentResult> getAssignmentResults() {
            return assignmentResults;
        }

        public int getQuestionID() {
            return questionID;
        }
}
