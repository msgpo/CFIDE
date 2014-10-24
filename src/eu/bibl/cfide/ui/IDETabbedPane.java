package eu.bibl.cfide.ui;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import eu.bibl.cfide.project.CFIDEProject;
import eu.bibl.cfide.project.ProjectUtils;

public class IDETabbedPane extends JTabbedPane {
	
	private static final long serialVersionUID = -8407666288357935339L;
	
	public IDETabbedPane() {
		setFocusable(false);
		addTab("Welcome", new JPanel());
	}
	
	public void openJar(String location) {
		File loc = new File(location);
		if (!loc.exists()) {
			JOptionPane.showMessageDialog(null, "File doesn't exist.", "Invalid input file.", JOptionPane.ERROR_MESSAGE);
			return;
		}
		CFIDEProject proj = ProjectUtils.newProject(location);
		String tabName = loc.getName().substring(0, loc.getName().length() - 4);// remove .jar from the end of the name
		
		ProjectPanel panel = new ProjectPanel(this, tabName, proj);
		addTab(tabName, panel);
		panel.setupFinal();
		setSelectedComponent(panel);
	}
	
	public void openProj(String location) {
		File loc = new File(location);
		if (!loc.exists()) {
			JOptionPane.showMessageDialog(null, "File doesn't exist.", "Invalid input file.", JOptionPane.ERROR_MESSAGE);
			return;
		}
		CFIDEProject proj = ProjectUtils.fromFile(loc);
		String tabName = loc.getName().substring(0, loc.getName().length() - 6);// remove .cfide from the end of the name
		
		ProjectPanel panel = new ProjectPanel(this, tabName, proj);
		addTab(tabName, panel);
		panel.setupFinal();
		setSelectedComponent(panel);
	}
}