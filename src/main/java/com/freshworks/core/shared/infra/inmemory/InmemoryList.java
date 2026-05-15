package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Getter
@Setter
public class InmemoryList implements InfraDbList {


    List<String> list;

    String listName;

    AtomicLong listIndex = new AtomicLong(0);

    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected InmemoryList(){

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void add(String s) throws Exception{

        try{
            listAddLock.lock();
            this.list.add(s);
            this.listIndex.incrementAndGet();
            }

        finally {
            listAddLock.unlock();
        }
    }


    public Long addAndGetIndex(String s) throws Exception{

        try{

            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            this.list.add(s);
            this.listIndex.incrementAndGet();
            return currentIndex;
        }

        finally {
            listAddLock.unlock();
        }

    }

    @Override
    public List<Long> addAndGetIndexBulk(List<String> sList) throws Exception{

        try{

            listAddLock.lock();
            List<Long> list = new ArrayList<>();
            long currentIndex = this.listIndex.get();

            for(String s : sList){
                this.list.add(s);
                list.add(currentIndex);
                currentIndex = currentIndex + 1;
            }

            this.listIndex.addAndGet(sList.size());

            return list;
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

            for(int i=0; i<s.size(); i++){
                this.list.add(s.get(i));
            }
            this.listIndex.addAndGet(s.size());
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public String get(int index) throws Exception {
        return this.list.get(index);
    }

    @Override
    public List<String> get(int start, int n) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();

        for(int i=start; i < start + n; i++){

            if(i >= this.list.size()){
                break;
            }
            else{
                returnList.add(this.list.get(i));
            }
        }

        return returnList;
    }

    @Override
    public List<String> get(List<Long> documentIdList) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();

        for(long documentId : documentIdList){

            returnList.add(this.list.get((int) documentId));
        }

        return returnList;

    }

    @Override
    public void deRegisterPublisher() throws Exception{

    }

    @Override
    public long size() throws Exception {
        return this.list.size();
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
    public void delete() throws Exception {
        this.list.clear();
    }
}
