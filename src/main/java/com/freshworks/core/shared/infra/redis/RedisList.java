package com.freshworks.core.shared.infra.redis;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@Slf4j
@Getter
@Setter

public class RedisList implements InfraDbList {

    static HashMap<String, RedisList> singletonMap = new HashMap<>();

    RedisConnection redisConnection;
    String list;

    private RedisList(){

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    public static RedisList getRedisList(RedisConnection redisConnection, String listName) throws Exception{
        // This should return singleton for single listname

        if(singletonMap.containsKey(listName)){
            return singletonMap.get(listName);
        }
        else{
            RedisList redisQueue = new RedisList();
            redisQueue.setList(listName);
            redisQueue.setRedisConnection(redisConnection);
            singletonMap.put(listName, redisQueue);
            return redisQueue;
        }
    }

    @Override
    public void add(String s) throws Exception{

        this.redisConnection.listCommands().lPush(this.list.getBytes(), s.getBytes());
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

            this.redisConnection.listCommands().lPush(this.list.getBytes(), ss.getBytes());
        }
    }

    @Override
    public Long addAndGetIndex(String s) throws Exception {
        return null;
    }

    @Override
    public List<Long> addAndGetIndexBulk(List<String> sList) throws Exception {
        return List.of();
    }

    @Override
    public String get(int index) throws Exception {

        return this.redisConnection.listCommands().lIndex(this.list.getBytes(), index).toString();
    }

    public InfraDbList getPublisherList() throws Exception{

        return RedisList.getRedisList(this.redisConnection, "publisher_list");
    }



    @Override
    public List<String> get(int start, int n) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();

        for(int i=start; i<n; i++){
            String s = this.redisConnection.listCommands().lIndex(this.list.getBytes(), i).toString();
            if(s == null){
                break;
            }

            returnList.add(s);
        }

        return returnList;
    }

    @Override
    public List<String> get(List<Long> documentIdList) throws Exception {
        return List.of();
    }

    @Override
    public void deRegisterPublisher() throws Exception{

    }

    @Override
    public long size() throws Exception {
        return this.redisConnection.listCommands().lLen(this.list.getBytes());
    }

    @Override
    public Boolean isEndOfListReached(int index) throws Exception{
        if(index < this.redisConnection.listCommands().lLen(this.list.getBytes())){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public void delete() throws Exception{
        this.redisConnection.listCommands().lTrim(this.list.getBytes(), 100, 0);
    }
}
