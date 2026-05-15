package com.freshworks.core.shared.infra.h2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
@Setter

public class H2DbKeyValue implements InfraDbKeyValue {


    String keyValueName;
    HikariDataSource hikariDataSource;
    ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock.WriteLock keyAddLock = new ReentrantReadWriteLock().writeLock();

    protected H2DbKeyValue(HikariDataSource hikariDataSource, String namespace, String keyValueName)  throws Exception{

        this.hikariDataSource = hikariDataSource;
        String sanitizedNameSpace = sanitizeName(namespace);
        String sanitizedKeyValueName = sanitizeName(keyValueName);
        String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS " + sanitizedNameSpace ;

        try(Connection connection = hikariDataSource.getConnection()){
            connection.createStatement().execute(createSchemaSql);
        }

        this.keyValueName = sanitizedNameSpace + "." + sanitizedKeyValueName;

        // SQL statement to create a table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " +  this.keyValueName  + "("
                + "search_key BIGINT NOT NULL PRIMARY KEY, "
                + "item VARCHAR(20000000)) ";

        try(Connection connection = hikariDataSource.getConnection();){
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
        }

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void put(String key, String value) throws Exception{

        try{
            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            insert(key, value);
        }

        finally {

            keyAddLock.unlock();
        }
    }

    @Override
    public String get(String key) throws Exception{

        key = key.replaceAll("\\.", "ENCODE_DOT");
        return find(key);
    }

    @Override
    public void putList(String key, List<String> value) throws Exception{

        try{
            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");

            String s = find(key);
            if(s != null){

                ArrayNode jsonNode = (ArrayNode) objectMapper.readTree(s);

                for(int i = 0; i < value.size(); i++){
                    String v = value.get(i).replaceAll("\\.", "ENCODE_DOT");
                    JsonNode x = objectMapper.readTree(v);
                    jsonNode.add(x);
                }

                insert(key, objectMapper.writeValueAsString(jsonNode));
            }
            else{
                ArrayNode arrayNode = objectMapper.createArrayNode();

                for(int i = 0; i < value.size(); i++){

                    String v = value.get(i).replaceAll("\\.", "ENCODE_DOT");
                    JsonNode x = objectMapper.readTree(v);
                    arrayNode.add(x);
                }

                insert(key, objectMapper.writeValueAsString(arrayNode));
            }
        }

        finally {

            keyAddLock.unlock();
        }
    }

    @Override
    public void putList(String key, String value) throws Exception{

        try{

            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            String s = find(key);
            if(s != null){

                ArrayNode jsonNode = (ArrayNode) objectMapper.readTree(s);
                JsonNode x = objectMapper.readTree(value);
                jsonNode.add(x);
                insert(key, objectMapper.writeValueAsString(jsonNode));
            }
            else{
                ArrayNode arrayNode = objectMapper.createArrayNode();
                JsonNode x = objectMapper.readTree(value);
                arrayNode.add(x);
                insert(key, objectMapper.writeValueAsString(arrayNode));
            }
        }
        finally {
            keyAddLock.unlock();
        }
    }

    @Override
    public List<String> getList(String key) throws Exception{
        key = key.replaceAll("\\.", "ENCODE_DOT");
        ArrayList<String> returnResult = new ArrayList<>();
        String s = find(key);
        JsonNode j = objectMapper.readTree(s);

        for(int i = 0; i < j.size(); i++){
            String ss = j.get(i).asText();
            returnResult.add(ss.replaceAll("ENCODE_DOT", "\\."));
        }
        return returnResult;
    }

    @Override
    public void delete() throws Exception{

        String dropTableSQL = "DROP TABLE IF EXISTS " + this.keyValueName;

        try (Connection connection = hikariDataSource.getConnection();
             Statement statement = connection.createStatement()) {

            keyAddLock.lock();
            // Execute the drop table statement
            statement.executeUpdate(dropTableSQL);

        }
        finally {
            keyAddLock.unlock();
        }
    }


    private void insert(String key, String value) throws Exception{

        String insertSql =  "Insert into " + this.keyValueName + " (search_key, item) values (?, ?)";

        try(Connection connection = hikariDataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(insertSql);){

            // Set the values for the placeholders
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, value);

            // Execute the insert statement
            int rowsAffected = preparedStatement.executeUpdate();
        }

    }

    private String  find(String key) throws Exception{

        String selectSQL = "SELECT item FROM " + this.keyValueName + " WHERE search_key = ?";
        String itemValue = null;

        try(Connection connection = hikariDataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)){

            // Set the values for the placeholders
            preparedStatement.setString(1, key);


            // Execute the select statement
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    // Retrieve the item value
                    itemValue = resultSet.getString("item");
                    return itemValue;
                }
            }
        }
        return itemValue;
    }

    private String sanitizeName(String name){

        return "h2_" + name.toLowerCase().replaceAll("\\.", "_").replaceAll("-", "_").replaceAll(":", "_");
    }
}
