package assignment01;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ThreadGUI extends JPanel{
	
	Object[][] threadInfo;

	public ThreadGUI(Object[][] threadInfo){

        String[] columnTitles = {"Group", "Name", "Identifier", "State", "Daemon", "Priority"};
        
        JTable table = new JTable(threadInfo, columnTitles);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
 
//		Add the scroll pane to this panel.
        add(scrollPane);
	}
	
	public static void displayGUI(Object[][] threadInfo) {
        //Create and set up the frame.
        JFrame frame = new JFrame("ThreadInfoTable");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the GUI.
        ThreadGUI newThreadGUI = new ThreadGUI(threadInfo);
        newThreadGUI.setOpaque(true); //Make the GUI diplay all content within its bounds
        frame.setContentPane(newThreadGUI);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
