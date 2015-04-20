package assignment01;

public class test {
	
	public static void main(String[] args){
		ThreadPrint.initTree();
		String[][] threads = ThreadPrint.getThreads(((new Thread()).getThreadGroup()).getParent());
		
		for (int i=0;i<threads.length;i++){
			for(int j=0;j<=5;j++){
				System.out.println(threads[i][j]);
			}
		}
		ThreadGUI.displayGUI(threads);
		
	}

}
