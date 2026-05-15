package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbList;
import com.mongodb.client.MongoCollection;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Getter
@Setter

public class H2DbList implements InfraDbList {


    String dbString;
    String listName;

    HikariDataSource hikariDataSource;

    AtomicLong listIndex = new AtomicLong(0);


    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected H2DbList(HikariDataSource hikariDataSource, String namespace, String listName) throws Exception {

        this.hikariDataSource = hikariDataSource;
        String sanitizedNameSpace = sanitizeName(namespace);
        String sanitizedListName = sanitizeName(listName);
        String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS "  + sanitizedNameSpace;

        try(Connection connection = hikariDataSource.getConnection()){
            connection.createStatement().execute(createSchemaSql);
        }


        this.listName = sanitizedNameSpace + "." + sanitizedListName;
        // SQL statement to create a table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " +  this.listName  + "("
                + "list_index BIGINT NOT NULL PRIMARY KEY, "
                + "item VARCHAR(20000000), "
                + "PRIMARY KEY (list_index))";

        try(Connection connection = hikariDataSource.getConnection()){
            connection.createStatement().execute(createTableSQL);
        }
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }


    @Override
    public void add(String s) throws Exception{

        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{

            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            insert(currentIndex, s);
            listIndex.incrementAndGet();
        }

        finally {
            listAddLock.unlock();
        }
    }

    public Long addAndGetIndex(String s) throws Exception{

        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            insert(currentIndex, s);
            this.listIndex.incrementAndGet();
            return currentIndex;
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public List<Long> addAndGetIndexBulk(List<String> s) throws Exception{

        List<Long> documentIds = new ArrayList<>();
        if(s.isEmpty()){
            return  documentIds;
        }

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");

                insert(currentIndex, ss);
                documentIds.add(currentIndex);
                currentIndex = currentIndex + 1;
            }

            this.listIndex.addAndGet(documentIds.size());
            return documentIds;
        }

        finally {

            listAddLock.unlock();
        }
    }

    @Override
    public void add(List<String> s) throws Exception{

        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
                currentIndex = currentIndex + 1;
            }

            this.listIndex.addAndGet(s.size());
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public String get(int index) throws Exception {

        String s = find(index);
        if(s != null){
            return s.replaceAll("ENCODE_DOT", "\\.");
        }
        else{
            return null;
        }
    }


    @Override
    public List<String> get(int start, int n) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();
        long index = start;
        ArrayList<String> list = new ArrayList<>();
        for(int i=0; i< n; i++){

            String s = find(index);
            if(s != null){
                list.add(s);
            }

            index = index + 1;
        }

        Iterator<String> it = list.iterator();

        while (it.hasNext()){
            String s = it.next();
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }


    public List<String> get(List<Long> documentIdList) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();
        Iterator<Long> it = documentIdList.iterator();


        while (it.hasNext()){
            long id = it.next();
            String s = find(id);
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }

    @Override
    public void deRegisterPublisher() throws Exception{

    }

    @Override
    public long size() {
        return this.listIndex.get();
    }

    @Override
    public Boolean isEndOfListReached(int index) throws Exception{
        if(index < this.listIndex.get()){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public void delete() throws Exception{

        String dropTableSQL = "DROP TABLE IF EXISTS " + this.listName;

        try (Connection connection = hikariDataSource.getConnection();
             Statement statement = connection.createStatement()) {

            listAddLock.lock();
            // Execute the drop table statement
            statement.executeUpdate(dropTableSQL);
        }

        finally {
            listAddLock.unlock();
        }
    }


    private void insert(long listIndex, String item) throws Exception{

        String insertSql =  "Insert into " + this.listName + " (list_index, item) values (?, ?)";

        try(Connection connection = hikariDataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(insertSql);){

            // Set the values for the placeholders
            preparedStatement.setLong(1, listIndex);
            preparedStatement.setString(2, item);

            // Execute the insert statement
            int rowsAffected = preparedStatement.executeUpdate();
        }
    }

    private String  find(long listIndex) throws Exception{

        String selectSQL = "SELECT item FROM " + this.listName + " WHERE list_index = ?";
        String itemValue = null;

        try(Connection connection = hikariDataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)){

            // Set the values for the placeholders
            preparedStatement.setLong(1, listIndex);


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
