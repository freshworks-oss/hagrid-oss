package com.freshworks.freshindex.index.typeindex;

import com.sun.source.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Scope(value = "prototype")
@Slf4j
public class DoubleIndex extends BaseIndex{

    TreeMap<Double, TreeMap<String, String>> index = new TreeMap<>();
    ReentrantReadWriteLock reLock = new ReentrantReadWriteLock();

    @Override
    public void insert(String value, String documentId) throws Exception{

        double key = Double.parseDouble(value);

        try{
            reLock.writeLock().lock();
            TreeMap<String, String> docMaps = index.get(key);;
            if (docMaps == null) {
                docMaps = new TreeMap<>();
                index.put(key, docMaps);
            }

            docMaps.computeIfAbsent(documentId, x -> "");

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
            Double key = Double.parseDouble(value);
            TreeMap<String, String> docIds = index.get(key);
            docIds.remove(documentId);
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
            Double key = Double.parseDouble(value);

            if(operator.equals("=")){
                return new ArrayList<>(index.get(key).keySet());
            }
            else if (operator.equals(">")){
                List<String> result = new ArrayList<>();

                SortedMap<Double, TreeMap<String, String>> sortedMap = index.tailMap(key);
                Collection<TreeMap<String, String>> values = sortedMap.values();

                for (TreeMap<String, String> docIdList: values) {

                    result.addAll(docIdList.keySet());
                }

                return result;
            }

            else if (operator.equals("<")){

                List<String> result = new ArrayList<>();

                SortedMap<Double, TreeMap<String, String>> sortedMap = index.headMap(key);
                Collection<TreeMap<String, String>> values = sortedMap.values();

                for (TreeMap<String, String> docIdList: values) {

                    result.addAll(docIdList.keySet());
                }

                return result;

            }
            else if(operator.equals("!=")){
                List<String> result = new ArrayList<>();
                for (Map.Entry<Double, TreeMap<String, String>> entry : index.entrySet()) {
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
    public List<Object> getValues() throws Exception{

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
