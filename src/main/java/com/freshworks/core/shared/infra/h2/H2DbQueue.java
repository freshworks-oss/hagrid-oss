package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.mongodb.client.MongoCollection;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
@Setter
public class H2DbQueue implements InfraDbQueue {

    int publisherAttached = -100;
    int consumerAttached = -100;

    AtomicLong queueIndex = new AtomicLong(0);

    String dbString;
    String queueName;

    Timer timer;

    HikariDataSource hikariDataSource;

    volatile long popIndex ;

    private final ReentrantReadWriteLock.WriteLock queueAddLock = new ReentrantReadWriteLock().writeLock();
    private final ReentrantReadWriteLock.WriteLock queuePollLock = new ReentrantReadWriteLock().writeLock();

    ReentrantLock hasMoreDataLock = new ReentrantLock();
    final Condition hasNotMoreDataQueue = hasMoreDataLock.newCondition();

    protected H2DbQueue(HikariDataSource hikariDataSource, String namespace, String queueName)  throws Exception{

        this.hikariDataSource = hikariDataSource;
        String sanitizedNameSpace = sanitizeName(namespace);
        String sanitizedQueueName = sanitizeName(queueName);
        String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS " + sanitizedNameSpace;

        try(Connection connection = hikariDataSource.getConnection();){
            connection.createStatement().execute(createSchemaSql);
        }

        this.queueName = sanitizedNameSpace + "." + sanitizedQueueName;
        // SQL statement to create a table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " +  this.queueName  + "("
                + "queue_index BIGINT NOT NULL, "
                + "item VARCHAR(20000000), "
                + "PRIMARY KEY (queue_index))";


        try(Connection connection = this.hikariDataSource.getConnection()){
            connection.createStatement().execute(createTableSQL);
        }
    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{
        MeterRegistry meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        timer = meterRegistry.timer(queueName + ".execution.time");
    }

    @Override
    public void add(String s) throws Exception{

        publisherAttached = 0;
        s = s.replaceAll("\\.", "ENCODE_DOT");
        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            insert(currentIndex, s);
            this.queueIndex.incrementAndGet();
            hasMoreDataLock.lock();
            hasNotMoreDataQueue.signalAll();
            hasMoreDataLock.unlock();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    @Override
    public void add(List<String> s) throws Exception{

        publisherAttached = 0;
        if(s.isEmpty()){
            return;
        }

        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            for(int i=0; i<s.size(); i++){

                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
                currentIndex = currentIndex + 1;
            }

            this.queueIndex.addAndGet(s.size());
            hasMoreDataLock.lock();
            hasNotMoreDataQueue.signalAll();
            hasMoreDataLock.unlock();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    @Override
    public String poll() throws Exception{

        consumerAttached = 0;
        try{
            queuePollLock.lock();
            String s = find(popIndex);
            if(s != null){
                this.popIndex = this.popIndex + 1;
                s = s.replaceAll("ENCODE_DOT", "\\.");
                return s;
            }
            else{
                return null;
            }
        }

        finally {
            queuePollLock.unlock();
        }
    }

    @Override
    public List<String> poll(int n) throws Exception{

        consumerAttached = 0;

        try{
            queuePollLock.lock();
            ArrayList<String> returnList = new ArrayList<>();
            long index = this.popIndex;
            ArrayList<String> list = new ArrayList<>();
            for(int i=0; i< n; i++){

                String s = find(index);

                if(s != null){
                    list.add(s);
                }

                index = index + 1;
            }

            Iterator<String> it = list.iterator();

//        Here fetched document could be lesser than int n, hence we need to count the real fetch document and add it to pop index
            while (it.hasNext()){
                String s = it.next();
                s = s.replaceAll("ENCODE_DOT", "\\.");
                returnList.add(s);
                this.popIndex = this.popIndex + 1;
            }

            return returnList;
        }

        finally {
            queuePollLock.unlock();
        }
    }

    @Override
    public boolean hasMoreData() throws Exception{

        try{
            hasMoreDataLock.lock();
            // It means that child so far has consumed less data than parent has fetched already
            if(this.popIndex < this.queueIndex.get()){
                return true;
            }

            // It means, producer is still not de attached from the queue
            else if(publisherAttached != 1){
                hasNotMoreDataQueue.await();
                return true;
            }

            else {
                return false;
            }
        }

        finally {
            hasMoreDataLock.unlock();
        }

    }

    @Override
    public void attachPublisher() throws Exception{

    }

    @Override
    public void removePublisher() throws Exception{

        hasMoreDataLock.lock();
        publisherAttached = 1;
        hasNotMoreDataQueue.signalAll();
        hasMoreDataLock.unlock();
    }

    @Override
    public long size() throws Exception{
        return this.queueIndex.get();
    }

    @Override
    public Boolean isEmpty() throws Exception{
        if(this.popIndex >= this.queueIndex.get()){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public void delete() throws Exception{

        String dropTableSQL = "DROP TABLE IF EXISTS " + this.queueName;

        try (Connection connection = hikariDataSource.getConnection();
             Statement statement = connection.createStatement()) {

            queueAddLock.lock();
            // Execute the drop table statement
            statement.executeUpdate(dropTableSQL);

        }

        finally {
            queueAddLock.unlock();
        }
    }


    private void insert(long queueIndex, String item) throws Exception{

        String insertSql =  "Insert into " + this.queueName + " (queue_index, item) values (?, ?)";

        try(Connection connection = hikariDataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(insertSql);){

            // Set the values for the placeholders
            preparedStatement.setLong(1, queueIndex);
            preparedStatement.setString(2, item);

            preparedStatement.executeUpdate();
        }
    }

    private String  find(long queueIndex) throws Exception{

        String selectSQL = "SELECT item FROM " + this.queueName + " WHERE queue_index = ?";
        String itemValue = null;

        try(Connection connection = hikariDataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)){

            // Set the values for the placeholders
            preparedStatement.setLong(1, queueIndex);


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
