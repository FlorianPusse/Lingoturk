package models;

import play.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Repository {

    private static Connection connection = DB.getConnection();

    public static Connection getConnection(){
        try {
            if(connection.isClosed()){
                connection = DB.getConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void saveErrorMessage(String message){
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO ErrorMessages(message) VALUES (?)");
            statement.setString(1,message);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
