package eu.bibl.cfide.ui.editor;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;

public class EditorTabbedPane extends JTabbedPane {
	
	private static final long serialVersionUID = 9106124854514247948L;
	
	protected Map<String, EditorTextTab> tabs;
	
	public EditorTabbedPane() {
		tabs = new HashMap<String, EditorTextTab>();
		setFocusable(false);
	}
	
	public EditorTextTab getTextTab(String className) {
		return tabs.get(className);
	}
	
	public EditorTextTab createTextTab(String className) {
		EditorTextTab textTab = tabs.get(className);
		if (textTab == null) {
			tabs.put(className, textTab = new EditorTextTab(this, className));
			addTab(className, textTab);
			textTab.setupFinal();
		}
		return textTab;
	}
	
	@Override
	public void remove(Component c) {
		super.remove(c);
	}
	
	@Override
	public void addTab(String title, Component c) {
		super.addTab(title, c);
		revalidate();
	}
}