/**
 * ViewerPanel.java
 * @author Brendan Horan
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;


public class ViewerPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;	// used for Eclipse
	
	private ViewerData data;
	private Font boldFont, layoutFont;
	
	// Experiment Panel Components
	private String currentExpPath;
	private JLabel pathValueLabel;
	private JButton quickLookButton;
	
	// Well Pane Components
	public static JTabbedPane tabbedPane;
	private JPanel wellBoxPanel, wellLayoutPanel;
	private JLabel[] colLabels1, colLabels2, rowLabels1, rowLabels2;
	private JCheckBox[][] wellBoxes;
	
	// Option Panel Components
	private JPanel optPanel;
	private JCheckBox[][] optionBoxes;
	
	// Time Panel Components
	private int timePanelRowsMax = 5;
	private JPanel timePanel, allTimePanel;
	private JButton allTimeButton;
	private JCheckBox[][] timeBoxes;
	private final String 	SELECT_ALL = "Select all time points",
							DESELECT_ALL = "Deselect all time points";
	
	// Run Panel Components
	private final String COPY = "Copy", CANCEL = "Cancel", OPEN = "Open";
	private JButton openButton, copyButton;
	private JCheckBox openStackBox;
	
	public ViewerPanel(ViewerData data) {
		super(new GridBagLayout());
		this.data = data;
		boldFont = new Font("Monospaced", Font.BOLD, 14);
		layoutFont = new Font("Monospaced", Font.PLAIN, 12);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets.left = 10;
		
		// Add Experiment Panel
		gbc.insets.right = 10;
		gbc.gridwidth = 3;
		gbc.gridx = 0; gbc.gridy = 0;
		this.add(createExpPanel(), gbc);
		
		// Add Well Pane
		gbc.insets.left = 0;
		gbc.insets.right = 0;
		gbc.weightx = 1.0;	gbc.weighty = 1.0;
		gbc.gridx = 0; gbc.gridy = 1;
		this.add(createWellPane(), gbc);
		
		// Add Option Panel
		gbc.insets.left = 25;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		gbc.gridx = 0; gbc.gridy = 2;
		this.add(createOptPanel(), gbc);
		
		// Add Time Panel
		gbc.insets.bottom = 8;
		gbc.gridx = 0; gbc.gridy = 3;
		this.add(createTimePanel(), gbc);
		
		// Add Run Panel
		gbc.insets.right = 5;
		gbc.anchor = GridBagConstraints.LAST_LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		gbc.gridx = 2; gbc.gridy = 3;
		this.add(createRunPanel(), gbc);
		
		updateExpPanel();
	}
	
	// Create Experiment Panel
	private JPanel createExpPanel() {
		currentExpPath = "";
		ButtonGroup schemeButtonGroup = new ButtonGroup();
		JRadioButton[] schemeButtons = new JRadioButton[data.schemeNames.length];
		
		ItemListener listener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED)
					changeScheme(((JRadioButton)e.getItem()).getText());
			}
		};
		
		JPanel schemePanel = new JPanel();
		for(int i = 0; i < schemeButtons.length; i++) {
			schemeButtons[i] = new JRadioButton(data.schemeNames[i]);
			schemeButtons[i].setFocusable(false);
			if(data.schemeNames[i].equals(data.startScheme))
				schemeButtons[i].setSelected(true);
			schemeButtons[i].addItemListener(listener);
			schemeButtonGroup.add(schemeButtons[i]);
			schemePanel.add(schemeButtons[i]);
		}
		
		JLabel expLabel = new JLabel("Experiment :");
		expLabel.setFont(boldFont);
		JLabel expNameLabel = new JLabel(data.expName);
		expNameLabel.setFont(boldFont);
		JLabel pathLabel = new JLabel("Path :");
		pathLabel.setHorizontalAlignment(JLabel.TRAILING);
		pathLabel.setFont(boldFont);
		pathValueLabel = new JLabel();
		pathValueLabel.setOpaque(true);
		quickLookButton = new JButton();
		quickLookButton.setFocusable(false);
		quickLookButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				data.quickLook();
			}
		});
		
		JPanel expPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.right = 10;
		
		gbc.gridx = 0; gbc.gridy = 0;
		expPanel.add(expLabel, gbc);
		
		gbc.gridx = 0; gbc.gridy = 1;
		expPanel.add(pathLabel, gbc);
		
		gbc.gridx = 1; gbc.gridy = 0;
		expPanel.add(expNameLabel, gbc);
		
		gbc.gridwidth = 2;
		gbc.gridx = 1; gbc.gridy = 1;
		expPanel.add(pathValueLabel, gbc);
		
		gbc.gridx = 2; gbc.gridy = 0;
		expPanel.add(schemePanel);
		
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1.0;
		gbc.gridwidth = 1;
		gbc.gridx = 3; gbc.gridy = 1;
		expPanel.add(quickLookButton, gbc);
		
		return expPanel;
	}
	
	// Update Experiment Panel
	private void updateExpPanel() {
		String quickLookString = data.quickLookString;
		quickLookButton.setText(quickLookString);
		if(quickLookString.equals(""))
			quickLookButton.setEnabled(false);
		else
			quickLookButton.setEnabled(true);
		
		if(!currentExpPath.equals(data.expPath)) {
			if(HCA_Viewer.pathExists(data.expPath)) {
				pathValueLabel.setFont(boldFont);
				pathValueLabel.setForeground(Color.BLACK);
				pathValueLabel.setText(data.expPath);
				openButton.setEnabled(true);
				copyButton.setEnabled(true);
				
				for(int i = 0; i < wellBoxes.length; i++) {
					for(int j = 0; j < wellBoxes[i].length; j++)
						wellBoxes[i][j].setEnabled(true);
				}
			}
			else {
				pathValueLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
				pathValueLabel.setForeground(Color.RED);
				pathValueLabel.setText("---->    PATH NOT FOUND: CONNECT AND RESTART    <----");
				openButton.setEnabled(false);
				copyButton.setEnabled(false);
				
				for(int i = 0; i < wellBoxes.length; i++) {
					for(int j = 0; j < wellBoxes[i].length; j++)
						wellBoxes[i][j].setEnabled(false);
				}
			}
			currentExpPath = new String(data.expPath);
		}
	}
	
	// Create Well Pane
	private JTabbedPane createWellPane() {
		String[] colHeaders = {	"01", "02", "03", "04", "05", "06", "07", "08",
								"09", "10", "11", "12", "13", "14", "15", "16",
								"17", "18", "19", "20", "21", "22", "23", "24"},
				rowHeaders = {	"A", "B", "C", "D", "E", "F", "G", "H",
								"I", "J", "K", "L", "M", "N", "O", "P"};
		
		colLabels1 = new JLabel[colHeaders.length];
		colLabels2 = new JLabel[colHeaders.length];
		rowLabels1 = new JLabel[rowHeaders.length];
		rowLabels2 = new JLabel[rowHeaders.length];
		
		for(int i = 0; i < colLabels1.length; i++) {
			colLabels1[i] = new JLabel(colHeaders[i]);
			colLabels1[i].setFont(boldFont);
			colLabels2[i] = new JLabel(colHeaders[i]);
			colLabels2[i].setFont(boldFont);
		}
		
		for(int i = 0; i < rowLabels1.length; i++) {
			rowLabels1[i] = new JLabel(rowHeaders[i]);
			rowLabels1[i].setFont(boldFont);
			rowLabels2[i] = new JLabel(rowHeaders[i]);
			rowLabels2[i].setFont(boldFont);
		}
		
		Font wellBoxFont = new Font("Monospaced", Font.PLAIN, 14);
		wellBoxes = new JCheckBox[rowHeaders.length][colHeaders.length];
		for(int i = 0; i < wellBoxes.length; i++) {
			for(int j = 0; j < wellBoxes[i].length; j++) {
				wellBoxes[i][j] = new JCheckBox(rowHeaders[i] + colHeaders[j]);
				wellBoxes[i][j].setFocusable(false);
				wellBoxes[i][j].setFont(wellBoxFont);
			}
		}
		
		Color background = new Color(202, 225, 255);
		wellBoxPanel = new JPanel(new GridBagLayout());
		wellBoxPanel.setBackground(background);
		wellLayoutPanel = new JPanel(new GridBagLayout());
		wellLayoutPanel.setBackground(background);
				
		tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.addTab("-      Wells      -", wellBoxPanel);
		tabbedPane.addTab("-     Layout      -", wellLayoutPanel);
		updateWellPane();
		
		return tabbedPane;
	}
	
	// Update Well Pane
	private void updateWellPane() {
		wellBoxPanel.removeAll();
		wellLayoutPanel.removeAll();
		
		JPanel[] boxPanels = new JPanel[data.blocks.length];
		JPanel[] layoutPanels = new JPanel[data.blocks.length];
		for(int i = 0; i < boxPanels.length; i++) {
			boxPanels[i] = new JPanel(new GridLayout(data.blocks[i].rows, data.blocks[i].cols, -8, -2));
			boxPanels[i].setBorder(BorderFactory.createLineBorder(Color.GRAY));
			boxPanels[i].setBackground(Color.WHITE);
			
			JCheckBox box;
			for(int j = 0; j < data.blocks[i].rows; j++) {
				for(int k = 0; k < data.blocks[i].cols; k++) {
					box = wellBoxes[data.blocks[i].y + j][data.blocks[i].x + k];
					box.setToolTipText(data.getToolTip(box.getText()));
					boxPanels[i].add(box);
				}
			}
			
			JTextArea ta = new JTextArea();
			ta.setFocusable(false);
			ta.setEditable(false);
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			ta.setFont(layoutFont);
			
			String[] lines = data.blocks[i].layoutText.split(",");
			for(int j = 0; j < lines.length; j++)
				ta.append(lines[j] + "\n");
			
			layoutPanels[i] = new JPanel(new BorderLayout());
			layoutPanels[i].setBorder(BorderFactory.createLineBorder(Color.GRAY));
			layoutPanels[i].setBackground(Color.WHITE);
			layoutPanels[i].add(ta, BorderLayout.CENTER);
			layoutPanels[i].setPreferredSize(boxPanels[i].getPreferredSize());
		}
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0; gbc.weighty = 0;
		gbc.gridx = 1; gbc.gridy = 0;
		for(int i = 0; i < colLabels1.length; i++) {
			wellBoxPanel.add(colLabels1[i], gbc);
			wellLayoutPanel.add(colLabels2[i], gbc);
			gbc.gridx++;
		}
		
		gbc.insets.left = 5; gbc.insets.right = 8;
		gbc.weightx = 0; gbc.weighty = 1.0;
		gbc.gridx = 0; gbc.gridy = 1;
		for(int i = 0; i < rowLabels1.length; i++) {
			wellBoxPanel.add(rowLabels1[i], gbc);
			wellLayoutPanel.add(rowLabels2[i], gbc);
			gbc.gridy++;
		}
		
		final int WELL_ORIGIN_X = 1, WELL_ORIGIN_Y = 1;
		gbc.insets.left = 0; gbc.insets.right = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0; gbc.weighty = 1.0;
		for(int i = 0; i < boxPanels.length; i++) {
			gbc.gridwidth = data.blocks[i].cols;
			gbc.gridheight = data.blocks[i].rows;
			gbc.gridx = WELL_ORIGIN_X + data.blocks[i].x;
			gbc.gridy = WELL_ORIGIN_Y + data.blocks[i].y;
			wellBoxPanel.add(boxPanels[i], gbc);
			wellLayoutPanel.add(layoutPanels[i], gbc);
		}
	}
	
	// Create Option Panel
	private JPanel createOptPanel() {
		optPanel = new JPanel(new BorderLayout());
		optPanel.setBorder(BorderFactory.createTitledBorder("Select option(s)"));
		updateOptPanel();
		
		return optPanel;
	}
	
	// Update Option Panel
	private void updateOptPanel() {
		optPanel.removeAll();
		
		optionBoxes = new JCheckBox[data.optionGrid.labels.length][];
		JLabel[] optionLabels = new JLabel[data.optionGrid.labels.length];
		JPanel[] optionPanels = new JPanel[data.optionGrid.labels.length];
		JPanel optionPanelContainer = new JPanel(new GridLayout(optionPanels.length, 0, 0, -10));
		
		for(int i = 0; i < data.optionGrid.labels.length; i++) {
			optionLabels[i] = new JLabel(data.optionGrid.labels[i] + " :  ");
			optionBoxes[i] = new JCheckBox[data.optionGrid.itemNames[i].length];
			optionPanels[i] = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 5));
			optionPanels[i].add(optionLabels[i]);
			for(int j = 0; j < data.optionGrid.itemNames[i].length; j++) {
				optionBoxes[i][j] = new JCheckBox(data.optionGrid.itemNames[i][j]);
				optionBoxes[i][j].setFocusable(false);
				optionPanels[i].add(optionBoxes[i][j]);
			}
			if(data.optionGrid.itemNames[i].length == 1)
				optionBoxes[i][0].setSelected(true);
			optionPanelContainer.add(optionPanels[i]);
		}
		optPanel.add(optionPanelContainer, BorderLayout.LINE_START);
	}
	
	// Create Time Panel
	private JPanel createTimePanel() {
		timePanel = new JPanel(new BorderLayout());
		timePanel.setBorder(BorderFactory.createTitledBorder("Select time point(s)"));
		allTimeButton = new JButton(SELECT_ALL);
		allTimeButton.setFocusable(false);
		allTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(allTimeButton.getText().equals(SELECT_ALL)) {
					toggleAllTime(true);
					allTimeButton.setText(DESELECT_ALL);
				}
				else {
					toggleAllTime(false);
					allTimeButton.setText(SELECT_ALL);
				}
			}
		});
		
		allTimePanel = new JPanel();
		allTimePanel.add(allTimeButton);
		updateTimePanel();
		
		return timePanel;
	}
	
	// Update Time Panel
	private void updateTimePanel() {
		
		timeBoxes = new JCheckBox[data.timeGrid.itemNames.length][];
		JPanel[] timeBoxPanels = new JPanel[data.timeGrid.itemNames.length];
		
		if (data.timeGrid.itemNames.length > timePanelRowsMax){
			timePanelRowsMax = data.timeGrid.itemNames.length;
		}	
		JPanel timePanelContainer = new JPanel(new GridLayout(timePanelRowsMax, 0, 0, -10));
		
		for(int i = 0; i < data.timeGrid.itemNames.length; i++) {
			timeBoxes[i] = new JCheckBox[data.timeGrid.itemNames[i].length];
			timeBoxPanels[i] = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 5));
			for(int j = 0; j < data.timeGrid.itemNames[i].length; j++) {
				String boxName = data.timeGrid.itemNames[i][j];
				if(boxName.length() == 1)	// zero-padding
					boxName = "0" + boxName;
				timeBoxes[i][j] = new JCheckBox(boxName);
				timeBoxes[i][j].setFocusable(false);
				timeBoxPanels[i].add(timeBoxes[i][j]);
			}
			timePanelContainer.add(timeBoxPanels[i]);
		}
		
		if((data.timeGrid.itemNames.length == 1) && (data.timeGrid.itemNames[0].length == 1))
			timeBoxes[0][0].setSelected(true);
		
		for(int i = data.timeGrid.itemNames.length; i < timePanelRowsMax; i++) {
			timePanelContainer.add(new JLabel(" "));
		}
		
		allTimeButton.setText(SELECT_ALL);
		timePanel.removeAll();
		timePanel.add(timePanelContainer, BorderLayout.LINE_START);
		timePanel.add(allTimePanel, BorderLayout.PAGE_START);
	}
	
	// Create Run Panel
	private JPanel createRunPanel() {
		JPanel runPanel = new JPanel(new BorderLayout(0, 10));
		
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				if(command.equals(OPEN))
					process(data.OPEN);
				else if(command.equals(COPY))
					process(data.COPY);
				else if(command.equals(CANCEL))
					HCA_Viewer.exit();
			}
		};
		
		openStackBox = new JCheckBox("Open as stack");
		openStackBox.setFocusable(false);
		openStackBox.setHorizontalAlignment(JCheckBox.TRAILING);
		
		openButton = new JButton(OPEN);
		openButton.setFocusable(false);
		openButton.addActionListener(listener);
		
		copyButton = new JButton(COPY);
		copyButton.setFocusable(false);
		copyButton.addActionListener(listener);
		
		JButton cancelButton = new JButton(CANCEL);
		cancelButton.setFocusable(false);
		cancelButton.addActionListener(listener);
		
		runPanel.add(openStackBox, BorderLayout.PAGE_START);
		runPanel.add(copyButton, BorderLayout.CENTER);
		runPanel.add(cancelButton, BorderLayout.LINE_START);
		runPanel.add(openButton, BorderLayout.LINE_END);
		
		return runPanel;
	}
	
	// Change scheme
	private void changeScheme(String scheme) {
		int update = data.changeScheme(scheme);
		
		// Bit mask operations
		if((update & data.UPDATE_EXP) == data.UPDATE_EXP)
			updateExpPanel();
		
		if((update & data.UPDATE_WELL) == data.UPDATE_WELL)
			updateWellPane();
		
		if((update & data.UPDATE_OPT) == data.UPDATE_OPT)
			updateOptPanel();
		
		if((update & data.UPDATE_TIME) == data.UPDATE_TIME)
			updateTimePanel();
		
		HCA_Viewer.redraw();
	}
	
	// Select all time boxes (true)
	// Deselect all time boxes (false)
	private void toggleAllTime(boolean OnOff) {
		for(int i = 0; i < timeBoxes.length; i++) {
			for(int j = 0; j < timeBoxes[i].length; j++)
				timeBoxes[i][j].setSelected(OnOff);
		}
	}
	
	private void process(int operation) {
		HCA_Viewer.printToLog("Collect user selections...");
		// Check wells
		for(int i = 0; i < data.blocks.length; i++) {
			for(int j = 0; j < data.blocks[i].rows; j++) {
				for(int k = 0; k < data.blocks[i].cols; k++)
					if(wellBoxes[data.blocks[i].y + j][data.blocks[i].x + k].isSelected()) {
						data.blocks[i].wellChecked[j][k] = true;
						data.blocks[i].wellNames[j][k] = wellBoxes[data.blocks[i].y + j][data.blocks[i].x + k].getText();
					}
					else
						data.blocks[i].wellChecked[j][k] = false;
			}
		}
		
		// Check options
		for(int i = 0; i < data.optionGrid.itemNames.length; i++) {
			for(int j = 0; j < data.optionGrid.itemNames[i].length; j++) {
				if(optionBoxes[i][j].isSelected())
					data.optionGrid.itemChecked[i][j] = true;
				else
					data.optionGrid.itemChecked[i][j] = false;
			}
		}
		
		// Check times
		for(int i = 0; i < data.timeGrid.itemNames.length; i++) {
			for(int j = 0; j < data.timeGrid.itemNames[i].length; j++) {
				if(timeBoxes[i][j].isSelected())
					data.timeGrid.itemChecked[i][j] = true;
				else
					data.timeGrid.itemChecked[i][j] = false;
			}
		}
		
		data.setOpenAsStack(openStackBox.isSelected());
		HCA_Viewer.printToLog("Selections collected");
		
		int result = data.validateSelection();
		if(result == data.ERROR) {
			HCA_Viewer.showMessage("Error", data.message, data.ERROR);
			return;
		}
		
		if(result == data.WARNING) {
			if(!HCA_Viewer.showMessage("Warning", data.message, data.WARNING))
				return;
		}
		
		data.process(operation);
	}
}