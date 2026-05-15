package com.freshworks.freshindex.index.typeindex;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Scope(value = "prototype")
@Slf4j
public class StringIndex extends BaseIndex {

    TreeMap<String, TreeMap<String, String>> index = new TreeMap<>();
    ReentrantReadWriteLock reLock = new ReentrantReadWriteLock();

    @Override
    public void insert(String value, String documentId) throws Exception{

        String key = value;

        try{

            reLock.writeLock().lock();
            TreeMap<String, String> docsMaps = index.computeIfAbsent(key, k -> new TreeMap<>());
            docsMaps.computeIfAbsent(documentId, x -> "");

        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            reLock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String value, String documentId) throws Exception{

        try{
            reLock.writeLock().lock();
            String key = value;
            TreeMap<String,String> docMaps = index.get(key);
            docMaps.remove(documentId);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {

            reLock.writeLock().unlock();
        }

    }

    @Override
    public List<String> get(String value, String operator) throws Exception{

        try{

            reLock.readLock().lock();
            String key = value;

            if(operator.equals("=")){
                return new ArrayList<>(index.get(key).keySet());
            }
            else if (operator.equals("!=")){
                List<String> result = new ArrayList<>();
                for (Map.Entry<String, TreeMap<String, String>> entry : index.entrySet()) {
                    if (entry.getKey() != key) {
                        result.addAll(entry.getValue().keySet());
                    }
                }

                return result;
            }
            else{
                throw  new Exception("Operation " + operator + " is not supported by FreshxPath");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        finally {

            reLock.readLock().unlock();
        }

        return null;
    }

    @Override
    public List<Object> getValues() throws Exception {

        try{
            reLock.readLock().lock();
            return new ArrayList<>(this.index.keySet());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
         reLock.readLock().unlock();
        }

        return null;
    }
}
