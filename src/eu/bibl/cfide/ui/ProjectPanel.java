package eu.bibl.cfide.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.reflect.Constructor;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;

import eu.bibl.banalysis.asm.ClassNode;
import eu.bibl.bio.JarInfo;
import eu.bibl.bio.jfile.in.JarDownloader;
import eu.bibl.cfide.engine.compiler.BasicSourceCompiler;
import eu.bibl.cfide.engine.compiler.CFIDECompiler;
import eu.bibl.cfide.engine.decompiler.ClassNodeDecompilationUnit;
import eu.bibl.cfide.engine.decompiler.DecompilationUnit;
import eu.bibl.cfide.project.CFIDEProject;
import eu.bibl.cfide.project.ProjectUtils;
import eu.bibl.cfide.ui.editor.EditorTabbedPane;
import eu.bibl.cfide.ui.editor.EditorTextTab;
import eu.bibl.cfide.ui.tree.ClassViewerTree;

public class ProjectPanel extends JPanel implements MouseListener, ActionListener {
	
	private static final long serialVersionUID = 7392644318314592144L;
	
	protected JTabbedPane tabbedPane;
	protected String tabName;
	protected CFIDEProject project;
	protected JSplitPane splitPane;
	protected JarDownloader dl;
	protected JScrollPane scrollPane;
	protected JTree tree;
	protected EditorTabbedPane etp;
	protected BasicSourceCompiler<ClassNode[]> compiler;
	
	public ProjectPanel(JTabbedPane tabbedPane, String tabName, CFIDEProject project) {
		super(new BorderLayout());
		this.tabbedPane = tabbedPane;
		this.tabName = tabName;
		this.project = project;
		init();
	}
	
	private void init() {
		File jarFile = new File(project.<String> getProperty(CFIDEProject.JAR_LOCATION_KEY));
		dl = new JarDownloader(new JarInfo(jarFile));
		if (!dl.parse())
			return;
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		etp = new EditorTabbedPane(project);
		compiler = getCompilerImpl();
		tree = new ClassViewerTree(project, etp, jarFile.getName(), dl.getContents(), this, getDecompilerImpl(), compiler);
		splitPane.setResizeWeight(0.115D);
		scrollPane = new JScrollPane(tree);
		splitPane.add(scrollPane);
		splitPane.add(etp);
		add(splitPane);
		createPopupMenu();// needs to be called first
		createTabPanel();
	}
	
	@SuppressWarnings("unchecked")
	protected DecompilationUnit<ClassNode> getDecompilerImpl() {
		DecompilationUnit<ClassNode> decompilerImpl = null;
		String className = null;
		try {
			className = project.getProperty(CFIDEProject.DECOMPILER_CLASS, ClassNodeDecompilationUnit.class.getCanonicalName());
			Class<?> c = Class.forName(className);
			for (Constructor<?> constructor : c.getDeclaredConstructors()) {
				if (constructor.toString().equals("(eu.bibl.cfide.project.CFIDEProject,eu.bibl.banalysis.storage.classes.ClassContainer)")) { // because the compiler constructor needs to take a project instance to init the builder and parse
					decompilerImpl = (DecompilationUnit<ClassNode>) constructor.newInstance(project, dl.getContents());
				}
			}
			if (decompilerImpl == null) {
				decompilerImpl = new ClassNodeDecompilationUnit(project, dl.getContents());
			}
		} catch (Exception e) {
			System.out.println("Error loading custom compiler: " + className);
			e.printStackTrace();
			project.putProperty(CFIDEProject.DECOMPILER_CLASS, ClassNodeDecompilationUnit.class.getCanonicalName());
			decompilerImpl = new ClassNodeDecompilationUnit(project, dl.getContents());
		}
		return decompilerImpl;
	}
	
	@SuppressWarnings("unchecked")
	public BasicSourceCompiler<ClassNode[]> getCompilerImpl() {
		BasicSourceCompiler<ClassNode[]> compilerImpl = null;
		String className = null;
		try {
			className = project.getProperty(CFIDEProject.COMPILER_CLASS, CFIDECompiler.class.getCanonicalName());
			Class<?> c = Class.forName(className);
			for (Constructor<?> constructor : c.getDeclaredConstructors()) {
				if (constructor.toString().endsWith("CFIDEProject)")) { // because the compiler constructor needs to take a project instance to init the builder and parse
					compilerImpl = (BasicSourceCompiler<ClassNode[]>) constructor.newInstance(project);
				}
			}
			if (compilerImpl == null) {
				compilerImpl = new CFIDECompiler(project);
			}
		} catch (Exception e) {
			System.out.println("Error loading custom compiler: " + className);
			e.printStackTrace();
			project.putProperty(CFIDEProject.COMPILER_CLASS, CFIDECompiler.class.getCanonicalName());
			compilerImpl = new CFIDECompiler(project);
		}
		return compilerImpl;
	}
	
	public String getText(String className) {
		EditorTextTab ett = etp.getTextTab(className);
		return ett.getTextArea().getText();
	}
	
	protected JPopupMenu popupMenu;
	
	protected void createPopupMenu() {
		// save menu popup
		popupMenu = new JPopupMenu();
		JMenuItem saveMenuItem = new JMenuItem("Save");
		saveMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("CFIDE projects", "cfide");
				chooser.setFileFilter(filter);
				int returnValue = chooser.showSaveDialog(ProjectPanel.this);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					if (file.exists()) {
						int yesNoValue = JOptionPane.showOptionDialog(ProjectPanel.this, "Do you want to overwrite the file?", "File already exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (yesNoValue == JOptionPane.NO_OPTION)// return value for no button press: 1
							return;
					}
					// if code reaches here, user pressed yes button
					ProjectUtils.save(project, file);
				}
			}
		});
		
		popupMenu.add(saveMenuItem);
	}
	
	protected int index;
	protected JPanel tabNamePanel;
	protected JLabel tabNameLabel;
	protected JButton tabCloseButton;
	
	protected void createTabPanel() {
		tabNamePanel = new JPanel(new BorderLayout(5, 2));
		tabNamePanel.setOpaque(false);
		tabNamePanel.setFocusable(false);
		
		tabNameLabel = new JLabel(tabName);
		tabNameLabel.addMouseListener(this);
		
		tabCloseButton = new JButton(UISettings.CLOSE_BUTTON_ICON);
		tabCloseButton.setFocusable(false);
		tabCloseButton.addActionListener(this);
		tabCloseButton.setSize(UISettings.CLOSE_BUTTON_SIZE);
		tabCloseButton.setPreferredSize(UISettings.CLOSE_BUTTON_SIZE);
		
		tabNamePanel.setComponentPopupMenu(popupMenu);
		tabNamePanel.add(tabNameLabel, BorderLayout.WEST);
		tabNamePanel.add(tabCloseButton);
	}
	
	public void setupFinal() { // called from IDETabbedPane.openJar and openProj
		index = tabbedPane.indexOfTab(tabName);
		tabbedPane.setTabComponentAt(index, tabNamePanel);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		tabbedPane.setSelectedComponent(ProjectPanel.this);
		if (e.getButton() != MouseEvent.BUTTON1) {
			if (popupMenu.isShowing())
				popupMenu.setVisible(false);
			popupMenu.show(tabNamePanel, e.getX(), e.getY());
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		tabbedPane.remove(ProjectPanel.this);
	}
}