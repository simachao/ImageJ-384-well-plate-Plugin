/**
 * HCA_Viewer.java
 * @author Brendan Horan
 */

import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.IJ;
import ij.plugin.frame.PlugInFrame;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;


public class HCA_Viewer extends PlugInFrame implements ActionListener, ItemListener {
	
	private static PlugInFrame thisViewer;
	private static boolean IJLOG = false;
	private String dataFilePath;
	private File dataFile, optFile;
	private Document doc, newDoc;
	private Data data;
	private GenericDialog expDialog;
	private String chosenExp;
	private Panel dialogPanel;
	private JPanel optPanel;
	private JCheckBox logBox, playStackBox, bcAdjustBox, remBox;
	private boolean experimentsChanged, options;
	
	private boolean usedChangeExperiments;
	
	// options.txt
	private final String optFilePath = "/Applications/ImageJ/plugins/HCA/options.txt",
			TRUE = "TRUE", FALSE = "FALSE", X_ML = "XML", LOG = "LOG", PLAY = "PLAY", SHOW = "SHOW";
	private BufferedReader input;
	private BufferedWriter output;
	private ArrayList<String> inputLines;
	
	public HCA_Viewer() {
		super("HCA Viewer");
		thisViewer = this;
		experimentsChanged = false;
		options = false;
		usedChangeExperiments = false;
		
		dialogPanel = new Panel(new BorderLayout(40, 10));
		optPanel = new JPanel();
		optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.PAGE_AXIS));
		logBox = new JCheckBox("Enable Log", false);
		playStackBox = new JCheckBox("Stack - play automatically", true);
		bcAdjustBox = new JCheckBox("Stack - show brightness/contrast", true);
		remBox = new JCheckBox("Remember these options");
		optPanel.add(logBox);
		optPanel.add(playStackBox);
		optPanel.add(bcAdjustBox);
		optPanel.add(new JLabel("   _"));
		optPanel.add(remBox);
		optPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		
		JButton button = new JButton("Change experiments");
		button.addActionListener(this);
		JCheckBox optBox = new JCheckBox("Options", options);
		optBox.addItemListener(this);
		
		dialogPanel.add(button, BorderLayout.LINE_END);
		dialogPanel.add(optBox, BorderLayout.LINE_START);
		
		dataFilePath = XML.DEFAULT_DIR + XML.DEFAULT_NAME;
		
		inputLines = new ArrayList<String>(8);
		optFile = new File(optFilePath);
		if(optFile.exists()) {
			try {
				input = new BufferedReader(new FileReader(optFile));
				String line = input.readLine();
				while(line != null) {
					inputLines.add(line);
					if(line.startsWith(X_ML)) {
						String[] split = line.split("=");
						if(split.length == 2)
							dataFilePath = split[1];
					}
					else if(line.startsWith(LOG)) {
						String[] split = line.split("=");
						if((split.length == 2) && split[1].equals(TRUE))
							logBox.setSelected(true);
					}
					else if(line.startsWith(PLAY)) {
						String[] split = line.split("=");
						if((split.length == 2) && split[1].equals(FALSE))
							playStackBox.setSelected(false);
					}
					else if(line.startsWith(SHOW)) {
						String[] split = line.split("=");
						if((split.length == 2) && split[1].equals(FALSE))
							bcAdjustBox.setSelected(false);
					}
					line = input.readLine();
				}
				input.close();
			} catch(Exception e) {}
		}
	}
	
	// Primary method called by ImageJ
	public void run(String arg) {
		dataFile = new File(dataFilePath);
		doc = loadData();
		if(doc == null)
			return;
		
		data = null;
		newDoc = null;
		while(data == null) {
			data = new Data(doc);
			chosenExp = showExpDialog();
			if((chosenExp == null) && (!experimentsChanged))
				return;
		}
		
		data.autoPlay = (playStackBox.isSelected() ? true : false);
		data.showBC = (bcAdjustBox.isSelected() ? true : false);
		
		if(remBox.isSelected()) {
			try {
				if(!optFile.exists()) {
					output = new BufferedWriter(new FileWriter(optFile, false));
					output.write("# LOG, PLAY and SHOW must appear below");
					output.newLine();
					output.write(LOG + "=" + (logBox.isSelected() ? TRUE : FALSE));
					output.newLine();
					output.write(PLAY + "=" + (playStackBox.isSelected() ? TRUE : FALSE));
					output.newLine();
					output.write(SHOW + "=" + (bcAdjustBox.isSelected() ? TRUE : FALSE));
				}
				else {
					output = new BufferedWriter(new FileWriter(optFile, false));
					for(int i = 0; i < inputLines.size(); i++) {
						String line = inputLines.get(i);
						if(line.startsWith(LOG))
							output.write(LOG + "=" + (logBox.isSelected() ? TRUE : FALSE));
						else if(line.startsWith(PLAY))
							output.write(PLAY + "=" + (playStackBox.isSelected() ? TRUE : FALSE));
						else if(line.startsWith(SHOW))
							output.write(SHOW + "=" + (bcAdjustBox.isSelected() ? TRUE : FALSE));
						else
							output.write(line);
						output.newLine();
					}
				}
				output.close();
			} catch (Exception e) { }
		}
		
		if(data.init(chosenExp) == data.ERROR) {
			showMessage("Error", data.message, data.ERROR);
			return;
		}
		
		// Add primary viewer panel
		this.add(new ViewerPanel(data));
		this.pack();
		this.setSize(1274, 806);
		this.setLocationRelativeTo(null);
		ViewerPanel.tabbedPane.requestFocusInWindow();
		this.setVisible(true);
	}
	
	// Send string to log
	public static void printToLog(String message) {
		if(IJLOG) IJ.log(message);
	}
	
	// Refresh display
	public static void redraw() {
		thisViewer.validate();
	}
	
	// Exit this plugin
	public static void exit() {
		thisViewer.close();
	}
	
	// Check if path is valid
	public static boolean pathExists(String path) {
		if ((new File(path)).exists())
			return true;
		return false;
	}
	
	// Display error message
	public static boolean showMessage(String title, String message, int messageType) {
		boolean isError = (messageType == ViewerData.ERROR), status = false;
		GenericDialog messageDialog = new GenericDialog(title);
		JTextArea ta = new JTextArea(message, 10, 30);
		ta.setFocusable(false);
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		Panel messagePanel = new Panel(new BorderLayout());
		messagePanel.add(new JScrollPane(ta), BorderLayout.CENTER);
		messageDialog.addPanel(messagePanel, GridBagConstraints.CENTER, new Insets(5,0,0,0));
		messageDialog.setOKLabel("Continue");
		
		if(isError) {
			messageDialog.setOKLabel("Cancel");
			messageDialog.hideCancelButton();
		}
		
		messageDialog.showDialog();
		if(isError)
			status = false;
		else if(messageDialog.wasOKed())
			status = true;
		
		WindowManager.toFront(thisViewer);
		return status;
	}
	
	// Change Experiments button
	public void actionPerformed(ActionEvent e) {
		OpenDialog openDialog = new OpenDialog("Open file", XML.DEFAULT_DIR, XML.DEFAULT_NAME);
		String fileName = openDialog.getFileName();
		if(fileName == null)
			return;
		dataFile = new File(openDialog.getDirectory() + openDialog.getFileName());
		newDoc = loadData();
		Button[] buttons = expDialog.getButtons();
		expDialog.actionPerformed(new ActionEvent(buttons[1], 0, "Cancel"));
		
		usedChangeExperiments = true;
	}
	
	// Options check box
	public void itemStateChanged(ItemEvent e) {
		if(!options) {
			dialogPanel.add(optPanel, BorderLayout.PAGE_END);
			options = true;
		}
		else {
			dialogPanel.remove(optPanel);
			options = false;
		}
		expDialog.validate();
		expDialog.pack();
	}
	
	// Attempt to load XML file into Document
	// @return Document from file
	private Document loadData() {
		Document doc;		
		if(!dataFile.exists()) {
			if(!showMessage("Warning", "-  XML data file not found at\n" + dataFile.getPath()
					+ "\n-  Press 'Continue' to open file", data.WARNING))
				return null;
			
			//OpenDialog openDialog = new OpenDialog("Open file", "/", "");
			OpenDialog openDialog = new OpenDialog("Open file", XML.DEFAULT_DIR, XML.DEFAULT_NAME);
			String fileName = openDialog.getFileName();
			if(fileName == null)
				return null;
			printToLog("dir: " + openDialog.getDirectory() + " file: " + openDialog.getFileName());
			dataFile = new File(openDialog.getDirectory() + openDialog.getFileName());
		}
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(dataFile);
		} catch(Exception e) {
			showMessage("Error", "-  Data file cannot be parsed", data.ERROR);
			return null;
		}
		return doc;
	}
	
	// Ask user to choose experiment
	// @return Experiment name chosen
	private String showExpDialog() {
		experimentsChanged = false;
		String[] allExpNames = data.getAllExpNames();
		expDialog = new GenericDialog("Choose an experiment");
		expDialog.addChoice("Select experiment:", allExpNames, allExpNames[allExpNames.length-1]);
		
		expDialog.addPanel(dialogPanel, GridBagConstraints.CENTER, new Insets(5,0,0,0));
		expDialog.showDialog();
		
		if (expDialog.wasCanceled()) return null;
		
		if(usedChangeExperiments){
			usedChangeExperiments = false;
			
			if(newDoc != null) {
				doc = newDoc;
				newDoc = null;
				experimentsChanged = true;
			}
			else
				experimentsChanged = false;
			
			data = null;
			return null;
		}
			
		String chosenExp = expDialog.getNextChoice();
		IJLOG = logBox.isSelected();
		
		if(IJLOG) {
			Calendar cal = Calendar.getInstance();
			String am_pm = (cal.get(Calendar.AM_PM) == 0) ? "AM" : "PM";
			int hr = cal.get(Calendar.HOUR),
				min = cal.get(Calendar.MINUTE),
				sec = cal.get(Calendar.SECOND),
				yr = cal.get(Calendar.YEAR),
				mon = cal.get(Calendar.MONTH),
				day = cal.get(Calendar.DATE);
			
			IJ.log("**************************************\nStart time: " + hr + ":" + min + ":" + sec
					+ " " + am_pm +", " + yr + "/"+ mon + "/"+ day);
			IJ.log("experiment -- " + chosenExp);
			IJ.log("");
		}
		return chosenExp;
	}
}