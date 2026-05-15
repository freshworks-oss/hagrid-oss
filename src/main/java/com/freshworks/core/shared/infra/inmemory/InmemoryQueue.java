package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Getter
@Setter
public class InmemoryQueue implements InfraDbQueue {

    int publisherAttached = -100;
    int consumerAttached = -100;

    List<String> queue;

    String queueName;

    AtomicLong queueIndex = new AtomicLong(0);

    volatile long popIndex ;

    private final ReentrantReadWriteLock.WriteLock queueAddLock = new ReentrantReadWriteLock().writeLock();
    private final ReentrantReadWriteLock queuePollLock = new ReentrantReadWriteLock();

    ReentrantLock hasMoreDataLock = new ReentrantLock();
    final Condition hasNotMoreDataQueue = hasMoreDataLock.newCondition();

    protected InmemoryQueue(){

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void add(String s) throws Exception{

        publisherAttached = 0;
        try{
            queueAddLock.lock();
            this.queue.add(s);
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
        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        try{

            queueAddLock.lock();
            for(int i=0; i<s.size(); i++){

                this.queue.add(s.get(i));
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

        try{
            consumerAttached = 0;
            queuePollLock.writeLock().lock();
            if(this.popIndex >= this.queue.size()){
                return null;
            }

            String s = this.queue.get((int)popIndex);
            if(s != null){
                this.popIndex = this.popIndex + 1;
                return s;
            }
            else{
                return null;
            }
        }

        finally {
            queuePollLock.writeLock().unlock();
        }
    }

    @Override
    public List<String> poll(int n) throws Exception {
        try{
            consumerAttached = 0;
            queuePollLock.writeLock().lock();
//            System.out.println("POLLING");
            ArrayList<String> returnList = new ArrayList<>();

            for(int i=0; i<n; i++){
                if(this.popIndex >= this.queue.size()){
                    break;
                }
                String s = this.queue.get((int)popIndex);
                this.popIndex = this.popIndex + 1;
                returnList.add(s);
            }

            return returnList;
        }
        finally {
            queuePollLock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasMoreData() throws Exception {

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
        }

        finally {
            hasMoreDataLock.unlock();
        }

        return false;
    }

    @Override
    public void attachPublisher() throws Exception{

    }

    @Override
    public void removePublisher() throws Exception {

        hasMoreDataLock.lock();
        publisherAttached = 1;
        hasNotMoreDataQueue.signalAll();
        hasMoreDataLock.unlock();
    }

    @Override
    public long size() throws Exception {
        return this.queue.size();
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
    public void delete() throws Exception {
        queue.clear();
    }
}
