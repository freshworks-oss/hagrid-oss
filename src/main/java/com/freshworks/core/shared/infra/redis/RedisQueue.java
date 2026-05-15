package com.freshworks.core.shared.infra.redis;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
@Setter
public class RedisQueue implements InfraDbQueue {

    static HashMap<String, RedisQueue> singletonMap = new HashMap<>();

    RedisConnection redisConnection;

    String queue;

    private final ReentrantReadWriteLock queueAddLock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock queuePollLock = new ReentrantReadWriteLock();

    private RedisQueue(){
//        this.queue = mongoDb.getCollection(queueName);
//        this.queue.createIndex(Indexes.ascending("queue_index"));
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    public static RedisQueue getRedisQueue(RedisConnection redisConnection, String queueName) throws Exception{

        if(singletonMap.containsKey(queueName)){
            return singletonMap.get(queueName);
        }
        else{
            RedisQueue redisQueue = new RedisQueue();
            redisQueue.setQueue(queueName);
            redisQueue.setRedisConnection(redisConnection);
            singletonMap.put(queueName, redisQueue);
            return redisQueue;
        }

    }

    @Override
    public void add(String s) throws Exception{

        this.redisConnection.listCommands().lPush(this.queue.getBytes(), s.getBytes());

    }



    @Override
    public void add(List<String> s) throws Exception{

        List<Document> documentArrayList = new ArrayList<>();

        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        for (String ss:
            s ) {

            this.redisConnection.listCommands().lPush(this.queue.getBytes(), ss.getBytes());
        }
    }


    @Override
    public String poll() throws Exception{

        return this.redisConnection.listCommands().lPop(this.queue.getBytes()).toString();
    }

    @Override
    public List<String> poll(int n) throws Exception{

        ArrayList<String> returnList = new ArrayList<>();

        for(int i=0; i<n; i++){
            String s = this.redisConnection.listCommands().rPop(this.queue.getBytes()).toString();
            if(s == null){
                break;
            }

            returnList.add(s);
        }

        return returnList;
    }

    @Override
    public void removePublisher() throws Exception{
    }

    @Override
    public long size() throws Exception{
        return this.redisConnection.listCommands().lLen(this.queue.getBytes());
    }

    @Override
    public boolean hasMoreData() throws Exception {
        return false;
    }

    @Override
    public void attachPublisher() throws Exception{

    }

    @Override
    public Boolean isEmpty() throws Exception {
        if(this.redisConnection.listCommands().lLen(this.queue.getBytes()) > 0){

            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public void delete() throws Exception{
        this.redisConnection.listCommands().lTrim(this.queue.getBytes(), 100, 0);
    }
}
