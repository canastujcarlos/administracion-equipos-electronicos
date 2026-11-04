/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.aliprinsa.administracion.gusto.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author admin.practicas
 */
public class DataBaseConnection {
   private static  Connection conn;
   
   public DataBaseConnection(){
       
   }
   
   public static Connection getConnection()throws SQLException{
       if (conn==null||conn.isClosed()){
           conn = DriverManager.getConnection(Credentials.URL_DB, Credentials.USER_DB, Credentials.PASS_DB);
       }
       return conn;
   }
}
