

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;

import java.util.*;


import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Елена
 */
@SuppressWarnings("unchecked")
public class MainClass {

    public static JSONObject search(String file) throws Exception{
        String[] criterias = {"lastName","productName","minExpenses","badCustomers"};   
        
        Connection connection = null;
        Statement statement = null; 
        JSONParser parser = new JSONParser();

        try {
            Map<Object,Object> response = new HashMap<Object,Object>();
            response.put("type", "search");
            Object obj = parser.parse(new FileReader(file));
            JSONObject jsonObject = (JSONObject) obj;
            
            connection = getdbConnection();
            statement = connection.createStatement();
            int type;
            
            JSONArray krit = (JSONArray) jsonObject.get("criterias");
            JSONArray results = new JSONArray(); 
            
            for (int i=0; i<krit.size();i++){
                JSONObject criteria = (JSONObject) krit.get(i);
                if (criteria.get(criterias[0])!=null) {
                    type = 0 ;
                }else if (criteria.get(criterias[1])!=null) {
                    type = 1;
                }else if (criteria.get(criterias[2])!=null) {
                    type = 2;
                }else if (criteria.get(criterias[3])!=null) {
                    type = 3;
                }
                else throw new Exception("Неверный формат критерия");

                JSONObject result = getRequest(statement,type,criteria);
                results.add((Object)result);
            }
            response.put("results", results);
            JSONObject ob = new JSONObject(response);
            return ob;
            
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("parse exception");
        }
        finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return null;
    }
    public static Connection getdbConnection(){
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your JDBC Driver?");
	return null;
}
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/shop","postgres", "1234");
        }
        catch (SQLException e){
            System.out.println("no connection to database");
        }
        return connection;
    }
    public static String getQuery(int type, JSONObject ob){
        String query = null;
        switch (type) {
            case 0: {
                query = "select firstName,lastName from Customers where lastName='"+ob.get("lastName")+"'";
                break;
            }
            case 1: {
                query = "select firstName,lastName from (" +
                "select count(*) cnt,firstname,lastname from products, purchases, customers " +
                "where (products.name = '"+ob.get("productName")+"' and products.id_product = purchases.id_product " +
                "and purchases.id_customer = customers.id_customer )" +
                "group by customers.id_customer) as uu where uu.cnt>"+ob.get("minTimes");
                break;
            }
            case 2: {
                query = "select  firstName, lastName from (" +
                "select sum(price) as summa, purchases.id_customer   from purchases, products where (" +
                "purchases.id_product = products.id_product ) group by purchases.id_customer) as uu , customers " +
                "where (uu.id_customer = customers.id_customer and uu.summa between " +
                ob.get("minExpenses")+" and "+ob.get("maxExpenses")+")";
                break;
            }
            case 3: {
                query = "select firstName, lastName from (" +
                "select count(purchases.id_customer) as cnt, customers.id_customer " +
                "from  customers left join purchases on " +
                "(customers.id_customer = purchases.id_customer) " +
                "group by customers.id_customer order by cnt limit "+ob.get("badCustomers")+") as uu, customers " +
                "where uu.id_customer = customers.id_customer";
                break;
            }
        }
        return query;
    }
    public static JSONArray getResultFromDB(ResultSet rs) throws SQLException{
        JSONArray ar = new JSONArray();
        while (rs.next()) {
            String last = rs.getString("lastName");
            String first = rs.getString("firstName");
            Map<Object,Object> object = new LinkedHashMap<Object,Object>();
            object.put("lastName", last);
            object.put("firstName", first);
            JSONObject ob = new JSONObject(object);
            ar.add((Object)ob);
        }
        return ar;
    } 
    public static JSONObject getRequest(Statement statement, int type, JSONObject criteria) throws SQLException{
        String query = getQuery(type,criteria);//генерируем текст запроса на основе входного критерия
        ResultSet rs = statement.executeQuery(query);//получаем таблицу с ответом
        JSONArray results = getResultFromDB(rs);
        
        Map<Object,Object> object = new LinkedHashMap<Object,Object>();
        object.put("criteria", criteria);
        object.put("results", results);
        JSONObject ob = new JSONObject(object);
        return ob;
    }
    
    public static int countOfDay(String start, String end){
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        int weekdays = 0;
        while(startDate.isBefore(endDate.plusDays(1))){
            if (DayOfWeek.SATURDAY.equals(startDate.getDayOfWeek())
                 || DayOfWeek.SUNDAY.equals(startDate.getDayOfWeek()) 
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-01"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-02"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-03"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-04"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-05"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-06"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-07"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"01-08"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"02-23"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"03-08"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"05-01"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"05-09"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"06-12"))
                    || startDate.equals(LocalDate.parse(startDate.getYear()+"-"+"11-04")))
            {}
            else  weekdays++;
            startDate = startDate.plusDays(1);
        } 
        return weekdays;
    }
    public static JSONObject stat(String file) throws SQLException{
        Connection connection = null;
        Statement statement = null; 
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(file));
            JSONObject jsonObject = (JSONObject) obj;
            String start = (String) jsonObject.get("startDate");
            String end = (String) jsonObject.get("endDate");
            
            connection = getdbConnection();
            statement = connection.createStatement();
            
            String holidays = "('01-01'), ('01-02'),"
                    + "('01-03'), ('01-04'),"
                    + "('01-05'), ('01-06'),"
                    + "('01-07'), ('01-08'),"
                    + "('02-23'), ('03-08'),"
                    + "('05-01'), ('05-09'),"
                    + "('06-12'), ('11-04')"; // условные праздники
            String request_customers = "with holidays as (values "+holidays+") " +
                "select customers.id_customer, summa ,customers.lastName,customers.firstName from customers, " +
                "(select sum(price) as summa, purchases.id_customer from purchases, products " +
                "where (purchases.id_product = products.id_product and " +
                "(date_of between DATE '"+start+"' and DATE '"+end+"' " +
                "and extract (isodow from date_of) <6 " +
                "and to_char(purchases.date_of,'MM-DD') not in (select * from holidays))) " +
                "group by purchases.id_customer) as uu " +
                "where (customers.id_customer=uu.id_customer) " +
                "order by coalesce(summa,0) desc ";
            ResultSet rs = statement.executeQuery(request_customers);
            JSONArray results = new JSONArray();
            int summa = 0; 
            int rows = 0;
            while (rs.next()){
                String id = rs.getString("id_customer");
                int summ = rs.getInt("summa");
                String request_products = "with holidays as (values "+holidays+")" +
                    "select  products.name,(count(purchases.id_product)*price) as prod_sum from purchases, products " +
                    "where (products.id_product = purchases.id_product and purchases.id_customer = "+id+" and " +
                    "(date_of between DATE '"+start+"' and DATE '"+end+"'  " +
                    "and extract (isodow from date_of) <6  " +
                    "and to_char(purchases.date_of,'MM-DD') not in (select * from holidays))) " +
                    "group by products.id_product " +
                    "order by prod_sum desc";
                statement = connection.createStatement();
                ResultSet rs2 = statement.executeQuery(request_products);
                JSONArray purchases = new JSONArray();
                while (rs2.next()){
                    Map<Object,Object> object = new LinkedHashMap<Object,Object>();
                    object.put("name", rs2.getString("name"));
                    object.put("expenses", rs2.getInt("prod_sum"));
                    JSONObject ob = new JSONObject(object);
                    purchases.add((Object)ob);
                }
                Map<Object,Object> object = new LinkedHashMap<Object,Object>();
                object.put("name", rs.getString("lastName") + " " + rs.getString("firstName"));
                object.put("purchases", purchases);
                object.put("totalExpenses", summ);
                JSONObject ob = new JSONObject(object);
                results.add((Object)ob);
                summa = summa + summ;
                rows++;
            }
            Map<Object,Object> result = new LinkedHashMap<Object,Object>();
            result.put("type", "stat");
            result.put("totalDays", countOfDay(start, end));
            result.put("customers", results);
            result.put("totalExpenses", summa);
            String s =String.format("%.2f",(float)summa/rows).replace(',', '.');
            result.put("avgExpenses", s);
            JSONObject ob = new JSONObject(result); 
            return ob;
            
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("parse exception");
        }
        finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return null;
    }
    public static void writeToFile(JSONObject ob, String file_name) throws IOException{
        try (FileWriter file = new FileWriter(file_name)) {
            file.write(ob.toJSONString());
            file.flush();
        }
    }
    public static void main(String[] args) throws Exception{
        String[] arg = {"search","input.json","output.json"}; 
        JSONObject ob = new JSONObject();
        try {
            if (args.length != 3) {
                throw new Exception("Incorrect data entry");
            }
            switch (args[0]){
                case ("search"): ob = search(args[1]); break;
                case ("stat"): ob = stat(args[1]); break;
                default: throw new Exception("Неверный ввод критерия");
            }
            writeToFile(ob,args[2]);
            
        }catch (Exception e) {
            Map<Object,Object> result = new LinkedHashMap<Object,Object>();
            result.put("type", "error");
            result.put("message", e.getMessage());
            ob = new JSONObject(result);
            writeToFile(ob, args[2]);
        }
    }
}