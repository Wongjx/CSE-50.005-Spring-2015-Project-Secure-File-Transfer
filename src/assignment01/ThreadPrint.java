package assignment01;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPrint {
	static ThreadGroup mainGroup;
	static ThreadGroup systemGroup;
	static ThreadGroup alphaGroup;
	static ThreadGroup betaGroup;
	static ThreadGroup thetaGroup;
	static ThreadGroup lambdaGroup;
	static ThreadGroup sigmaGroup;
	static ReentrantLock lock;

	public static void main(String[] args) {
		initTree();
		printInfo(systemGroup);
		

	}

	public static void initTree(){
		ArrayList<Thread> threads= new ArrayList<Thread>();
		Thread current=new Thread();

		ReentrantLock lock = new ReentrantLock();
		Condition wait =lock.newCondition();
		
		mainGroup = current.getThreadGroup();
		systemGroup = mainGroup.getParent();
		alphaGroup = new ThreadGroup(mainGroup,"alpha");
		Thread thread0= new Thread(alphaGroup,new waitThread(lock,wait),"0");
		threads.add(thread0);
		Thread thread1= new Thread(alphaGroup,new waitThread(lock,wait),"1");
		threads.add(thread1);
		Thread thread2= new Thread(alphaGroup,new waitThread(lock,wait),"2");
		threads.add(thread2);
		betaGroup = new ThreadGroup(mainGroup,"beta");
		Thread thread3= new Thread(betaGroup,new waitThread(lock,wait),"3");
		threads.add(thread3);
		thetaGroup = new ThreadGroup(alphaGroup,"theta");
		Thread thread4= new Thread(thetaGroup,new waitThread(lock,wait),"4");
		threads.add(thread4);
		Thread thread5= new Thread(thetaGroup,new waitThread(lock,wait),"5");
		threads.add(thread5);
		lambdaGroup = new ThreadGroup(alphaGroup,"lambda");
		Thread thread6= new Thread(lambdaGroup,new waitThread(lock,wait),"6");
		threads.add(thread6);
		sigmaGroup = new ThreadGroup(betaGroup,"sigma");
		Thread thread7= new Thread(sigmaGroup,new waitThread(lock,wait),"7");
		threads.add(thread7);
		for(Thread thread:threads){
			try {
				thread.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	
	public static void printInfo(ThreadGroup group){
		System.out.println("Group: "+group.getName()+" Priority: "+group.getMaxPriority());
		Thread[] threads =new Thread[group.activeCount()];
		ThreadGroup[] subGroups = new ThreadGroup[group.activeGroupCount()];
		int threadsNum =group.enumerate(threads, false);
		int groupNum=group.enumerate(subGroups,false);
		
		for(int i=0;i<threadsNum;i++){
			Thread thread=threads[i];
			System.out.printf("     %s:%d:%s:%s Priority:%d \n",thread.getName(),thread.getId(),thread.getState(),thread.isDaemon(),thread.getPriority());
		}
		for (int i=0;i<groupNum;i++){
			ThreadGroup groups=subGroups[i];
			printInfo(groups);
		}
	}
	
	public static String[][] getThreads(ThreadGroup group){
		int numOfThreads=group.activeCount();
		String[][] ret = new String[numOfThreads][6];
		
		Thread[] threads =new Thread[group.activeCount()];
		int threadsNum =group.enumerate(threads, true);
		
		for(int i=0;i<threadsNum;i++){
			Thread thread=threads[i];
			ret[i]=	new String[]{group.getName(),thread.getName(),Long.toString(thread.getId()),(thread.getState().toString()),Boolean.toString(thread.isDaemon()),Integer.toString(thread.getPriority())};
		}
		return ret;
	}
}

class waitThread implements Runnable {
	ReentrantLock lock;
	Condition wait;
	public waitThread(ReentrantLock lock,Condition wait){
		this.lock=lock;
		this.wait=wait;
	}
	@Override
	public void run(){
		lock.lock();
		try {
			wait.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}