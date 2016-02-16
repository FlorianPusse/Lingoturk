package models;

import com.fasterxml.jackson.databind.JsonNode;
import models.Questions.DiscourseConnectivesExperiment.CheaterDetectionQuestion;
import models.Questions.DiscourseConnectivesExperiment.DiscourseConnectivesQuestion;
import models.Questions.ExampleQuestion;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name="Words")
public class Word extends Model{

    @Column(name = "WordID")
    @Id
    int id;

    @Column(name = "Word")
    @Constraints.Required
    String word;

    private static Model.Finder<Integer,Word> finder = new Model.Finder<Integer,Word>(Integer.class,Word.class);

    protected Word(String word){
        this.word = word;
    }

    public void addQuestionUsedIn(DiscourseConnectivesQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO PregivenAnswersQuestions_haveChoosable_Words(QuestionID,WordID) " +
                        "SELECT " + q.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM PregivenAnswersQuestions_haveChoosable_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Integer> getQuestionsUsedIn() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM PregivenAnswersQuestions_haveChoosable_Words WHERE WordID=" + this.getId());

        List<Integer> result = new LinkedList<Integer>();
        while(rs.next()){
            result.add(rs.getInt("QuestionID"));
        }

        return result;
    }

    public void addExampleQuestionUsedIn(ExampleQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO ExampleQuestions_haveRightAnswer_Words(QuestionID,WordID) " +
                "SELECT " + q.getId() + ", " + this.getId() +
                " WHERE NOT EXISTS (" +
                "SELECT * FROM ExampleQuestions_haveRightAnswer_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                ")");

        statement.execute();
    }

    public void addExampleQuestionPossibleIn(ExampleQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO ExampleQuestions_havePossible_Words(QuestionID,WordID) " +
                        "SELECT " + q.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM ExampleQuestions_havePossible_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Integer> getExampleQuestionsUsedIn() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM ExampleQuestions_haveRightAnswer_Words WHERE WordID=" + this.getId());

        List<Integer> result = new LinkedList<Integer>();
        while(rs.next()){
            result.add(rs.getInt("QuestionID"));
        }

        return result;
    }

    public void addContainedAsProposedConnective(CheaterDetectionQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO CheaterDetectionQuestions_mustHave_Words(QuestionID,WordID) " +
                        "SELECT " + q.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM CheaterDetectionQuestions_mustHave_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Integer> getContainedAsProposedConnective() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustHave_Words WHERE WordID=" + this.getId());

        List<Integer> result = new LinkedList<Integer>();
        while(rs.next()){
            result.add(rs.getInt("QuestionID"));
        }

        return result;
    }

    public void addContainedAsMustNotHaveConnective(CheaterDetectionQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO CheaterDetectionQuestions_mustNotHave_Words(QuestionID,WordID) " +
                        "SELECT " + q.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM CheaterDetectionQuestions_mustNotHave_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Integer> getContainedAsMustNotHaveConnective() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustNotHave_Words WHERE WordID=" + this.getId());

        List<Integer> result = new LinkedList<Integer>();
        while(rs.next()){
            result.add(rs.getInt("QuestionID"));
        }

        return result;
    }

    public void addContainedAsMayHaveConnective(CheaterDetectionQuestion q) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO CheaterDetectionQuestions_mayHave_Words(QuestionID,WordID) " +
                        "SELECT " + q.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM CheaterDetectionQuestions_mayHave_Words WHERE QuestionID=" + q.getId() + " AND WordID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Integer> getContainedAsMayHaveConnective() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mayHave_Words WHERE WordID=" + this.getId());

        List<Integer> result = new LinkedList<Integer>();
        while(rs.next()){
            result.add(rs.getInt("QuestionID"));
        }

        return result;
    }

    public static Word createWord(String word) throws SQLException {
        Word result = new Word(word);
        result.save();

        return result;
    }

    public static List<Word> createWords(JsonNode node) throws SQLException {
        List<Word> connectives = new LinkedList<Word>();
        for(Iterator<JsonNode> connectives_tmp = node.iterator(); connectives_tmp.hasNext();){
            String connective = connectives_tmp.next().asText();
            connectives.add(createWord(connective));
        }
        return connectives;
    }

    public static Word getWordbyId(int id) throws SQLException {
        return finder.byId(id);
    }

    public static List<Word> getWordsByID(List<Integer> words) throws SQLException {
        List<Word> result = new LinkedList<Word>();
        for(Integer word : words){
            Word w = getWordbyId(word);
            if(w != null){
                result.add(w);
            }
        }
        return result;
    }

    public static List<String> getWordsByIDAsStrings(List<Integer> words) throws SQLException {
        List<String> result = new LinkedList<String>();
        for(Integer word : words){
            Word w = getWordbyId(word);
            if(w != null){
                result.add(w.getWord());
            }
        }
        return result;
    }

    public static List<String> getallWordsAsStrings() throws SQLException {
        List<String> result = new LinkedList<String>();

        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT * FROM WORDS");
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()){
            result.add(resultSet.getString("Word"));
        }
        return result;
    }

    @Override
    public String toString(){
        return "Id: " + id + "\nWord: " + word;
    }

    public int getId(){
        return id;
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Word)) return false;

        Word word1 = (Word) o;

        if (id != word1.id) return false;
        if (!word.equals(word1.word)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + word.hashCode();
        return result;
    }
}
