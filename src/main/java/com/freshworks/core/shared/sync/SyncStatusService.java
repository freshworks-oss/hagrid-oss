package com.freshworks.core.shared.sync;

import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Scope(value="prototype")
@Getter
public final class SyncStatusService {

    /**
     * sync = -100, -1 , 0 , 1 means not yet started, failed, in progress and success respectively.
     */


    @Setter(AccessLevel.PRIVATE)
    volatile  int traverser_status = -100;

    @Setter(AccessLevel.PRIVATE)
    volatile  int processor_status = -100;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private ReentrantReadWriteLock.WriteLock writeLock = new ReentrantReadWriteLock().writeLock();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Condition waitUntilSyncIsComplete = writeLock.newCondition();

    private Condition waitUntilTraverserIsComplete = writeLock.newCondition();

    private Condition waitUntilProcessorIsComplete = writeLock.newCondition();


    SyncServiceContainer syncServiceContainer;

    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;
    NamespaceService namespaceService;

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
        this.analyticsFactory = this.syncServiceContainer.getBean(AnalyticsFactory.class);
        this.namespaceService = this.syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = this.analyticsFactory.getAnalyticsService(this.namespaceService.getNamespace());
    }

    public void setTraverserInProgress(){
        try{
            writeLock.lock();
            setTraverser_status(0);
            waitUntilSyncIsComplete.signalAll();
            waitUntilTraverserIsComplete.signalAll();
        }

        finally {
            writeLock.unlock();
        }

    }

    public void setTraverserInFailed(){

        try{
            writeLock.lock();
            setTraverser_status(-1);
            waitUntilSyncIsComplete.signalAll();
            waitUntilTraverserIsComplete.signalAll();
        }
        finally {
            writeLock.unlock();
        }
    }

    public void setTraverserInSuccessful(){

        try{
            writeLock.lock();
            setTraverser_status(1);
            waitUntilSyncIsComplete.signalAll();
            waitUntilTraverserIsComplete.signalAll();
        }

        finally {
            writeLock.unlock();
        }
    }

    public void setProcessorInProgress(){

        try{
            writeLock.lock();
            setProcessor_status(0);
            waitUntilSyncIsComplete.signalAll();
            waitUntilProcessorIsComplete.signalAll();
        }

        finally {
            writeLock.unlock();
        }
    }

    public void setProcessorInFailed(){

        try{
            writeLock.lock();
            setProcessor_status(-1);
            waitUntilSyncIsComplete.signalAll();
            waitUntilProcessorIsComplete.signalAll();
        }

        finally {
            writeLock.unlock();
        }
    }

    public void setProcessorInSuccessful(){

        try{
            writeLock.lock();
            setProcessor_status(1);
            waitUntilSyncIsComplete.signalAll();
            waitUntilProcessorIsComplete.signalAll();
        }

        finally {
            writeLock.unlock();
        }
    }

    private void setProcessor_status(int status){

        this.processor_status = status;
    }

    private void setTraverser_status(int status){

        this.traverser_status = status;
    }

    public SyncStatusService(){
    }


    public synchronized int getSyncStatus() {

        // Here sequence of if and else matters.

        // If any module in progress then total sync is in progress
        if(this.getTraverser_status() == 0 || this.getProcessor_status() == 0 ){
            return 0;
        }

        // If execution comes to this point then both processor and traverser must be completed.
        // Then check if any of them failed then it is total failed
        else if (this.getTraverser_status() == -1 || this.getProcessor_status() == -1){
            return -1 ;
        }

        // If both are successful then total syn is successful
        else if(this.getTraverser_status() == 1 && this.getProcessor_status() == 1){
            return 1;
        }

        // If any of them has not started then sync has partially stated
        else if(this.getTraverser_status() == -100 || this.getProcessor_status() == -100){
            return -100;
        }

        else{
            throw new IllegalStateException("Sync status can not be figured out as traverser status is " + this.getTraverser_status()  + " and processor_status is " + this.getProcessor_status());
        }
    }


    public void waitUntilSyncIsInProgress(){

        try{
            writeLock.lock();
            while(this.getSyncStatus() == 0 || this.getSyncStatus() == -100){

                waitUntilSyncIsComplete.await();
                this.analyticsService.infoLogEvent("SYNC_STATUS","total_status", this.getSyncStatus(), "traverser_status", this.getTraverser_status(), "processor_status", this.getProcessor_status());
            }
        }

        catch (Exception e){
            throw new RuntimeException("Error in sync status service", e);
        }

        finally {

            writeLock.unlock();
        }

    }

    public void waitUntilTraverserIsInProgress(){

        try{
            writeLock.lock();

            while(this.getTraverser_status() == 0 || this.getTraverser_status() == -100){

                waitUntilTraverserIsComplete.await();
            }
        }

        catch (Exception e){
            throw new RuntimeException("Error in sync status service", e);
        }

        finally {

            writeLock.unlock();
        }
    }

    public void waitUntilProcessorIsInProgress(){

        try{
            writeLock.lock();

            while(this.getProcessor_status() == 0 || this.getProcessor_status() == -100){

                waitUntilProcessorIsComplete.await();
            }
        }
        catch (Exception e){
            throw new RuntimeException("Error in sync status service", e);
        }

        finally {

            writeLock.unlock();
        }
    }
}
