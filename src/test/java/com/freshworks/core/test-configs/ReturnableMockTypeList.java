package com.freshworks.core;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReturnableMockTypeList<E> {

    private final List<E> elements = new ArrayList<>();
    AtomicInteger readIndex = new AtomicInteger(0);
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void addNull(){

        try{
            lock.writeLock().lock();
            elements.add(null);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            lock.writeLock().unlock();
        }

    }

    public void add( E element ){
        try{
           lock.writeLock().lock();
            elements.add(element);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    public void add(List<E> elementList ){
        try{
            lock.writeLock().lock();
            for(E element : elementList){
                elements.add(element);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void add(E... elementList ){
        try{
            lock.writeLock().lock();
            for(E element : elementList){
                elements.add(element);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void clear(){
        elements.clear();
    }

    public E next(){

        try{
            lock.writeLock().lock();
            return read();
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private E read(){

        if(elements.isEmpty()){
            throw new NoSuchElementException();
        }

        if(readIndex.get() < elements.size()){
            E e = elements.get(this.readIndex.get());
            this.readIndex.incrementAndGet();
            return e;
        }

        this.readIndex.set(0);
        return elements.get(this.readIndex.get());
    }

    public int size(){
        return elements.size();
    }

    public Answer<E> answer(){

        ReturnableMockTypeList<E> list = this;
        return new Answer<E>() {

            @Override
            public E answer(InvocationOnMock invocationOnMock) throws Throwable {

                return list.next();
            }
        };
    }
}
