package io.pretty.collection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyArrayBlockingQueue<E> {
    private final ReentrantLock lock;
    private final Condition empty;
    private final Condition full;
    private final Object[] items;
    private int count;
    private int putIndex;
    private int takeIndex;

    public MyArrayBlockingQueue(int capacity, boolean fair) {
        this.lock = new ReentrantLock(fair);
        this.empty = lock.newCondition();
        this.full = lock.newCondition();
        this.items = new Object[capacity];
    }

    public boolean put(E e){
        if (e == null){
            throw new NullPointerException();
        }
        try {
            lock.lock();
            if (count == items.length){
                full.await();
            }
            items[putIndex] = e;
            putIndex = nextIndex(putIndex);
            count++;
            empty.signal();
            return true;
        }catch (InterruptedException e1){
            e1.printStackTrace();
        }finally {
            lock.unlock();
        }
        return false;
    }

    public E take(){
        try {
            lock.lock();
            if (count == 0){
                empty.await();
            }
            E e = (E) items[takeIndex];
            items[takeIndex] = null;
            takeIndex = nextIndex(takeIndex);
            count--;
            full.signal();
            return e;
        }catch (InterruptedException e1){
            e1.printStackTrace();
        }finally {
            lock.unlock();
        }
        return null;
    }

    private int nextIndex(int i){
        return ++i == items.length ? 0 : i;
    }

    public static void main(String[] args) {
        MyArrayBlockingQueue queue = new MyArrayBlockingQueue(20,true);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        pool.execute(new Test("put",queue));
        pool.execute(new Test("take",queue));
        pool.execute(new Test("take",queue));
        pool.shutdown();
    }


}

class Test implements Runnable{

    private String mode;
    private MyArrayBlockingQueue queue;
    public Test(String mode,MyArrayBlockingQueue queue) {
        this.mode = mode;
        this.queue = queue;
    }

    @Override
    public void run() {
        if (mode.equalsIgnoreCase("put")){
            for (int i = 0;i<200;i++){
                queue.put(i);
                System.out.println("put : "+i);
                if (i % 50 == 0 ){
                    try {
                        System.out.println(Thread.currentThread().getName() +" sleep for a while ");
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }else if (mode.equalsIgnoreCase("take")){
            while (true){

                Object take = queue.take();
                System.out.println(Thread.currentThread().getName()+" take : "+take);
            }
        }
    }
}