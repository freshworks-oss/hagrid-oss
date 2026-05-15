package com.freshworks.core.shared.infra.redis;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
@Setter

public class RedisKeyValue implements InfraDbKeyValue {

    static HashMap<String, RedisKeyValue> singletonMap = new HashMap<>();

    RedisConnection redisConnection;

    String keyValue;



    private RedisKeyValue(){
//        this.keyValue = mongoDb.getCollection(keyValueName);
//        keyValue.createIndex(Indexes.ascending("key"));
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {

    }
    public static RedisKeyValue getRedisKeyValue(RedisConnection redisConnection, String keyValueName){

        if(singletonMap.containsKey(keyValueName)){
            return singletonMap.get(keyValueName);
        }
        else{
            RedisKeyValue redisKeyValue = new RedisKeyValue();
            redisKeyValue.setKeyValue(keyValueName);
            redisKeyValue.setRedisConnection(redisConnection);
            singletonMap.put(keyValueName, redisKeyValue);
            return redisKeyValue;
        }
    }


    @Override
    public void put(String key, String value) {

        String namespace = this.keyValue + "/" + key;
        this.redisConnection.set(namespace.getBytes(), value.getBytes());
    }

    @Override
    public String get(String key){

        String namespace = this.keyValue + "/" + key;
        return this.redisConnection.get(namespace.getBytes()).toString();
    }

    @Override
    public void putList(String key, List<String> value) {

        String namespace = this.keyValue + "/" + key;

        for (String s: value) {
         this.redisConnection.lPush(namespace.getBytes(), s.getBytes());
        }
    }

    @Override
    public void putList(String key, String value){

        String namespace = this.keyValue + "/" + key;
        this.redisConnection.lPush(namespace.getBytes(), value.getBytes());

    }

    @Override
    public List<String> getList(String key) {

        String namespace = this.keyValue + "/" + key;
        ArrayList<String> returnResult = new ArrayList<>();
        List<byte[]> list = this.redisConnection.lRange(namespace.getBytes(), 0 , - 1);

        for (byte[] b: list) {
            returnResult.add(b.toString());
        }
        return returnResult;
    }

    @Override
    public void delete(){
        Set<byte[]> keys = this.redisConnection.keys(this.keyValue.getBytes());
        for (byte[] key : keys) {
            this.redisConnection.del(key);
        }
    }
}
