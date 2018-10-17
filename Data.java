/**
 * Data.java
 * @author Brendan Horan
 * @author Chao Sima
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.process.ImageStatistics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//Needed for overlays
import java.awt.Font;
import java.awt.Color;
import ij.gui.Overlay;
import ij.gui.TextRoi;



public class Data extends ViewerData {

	private Document doc;
	private Element	expNode, expDefaultsNode;
	private Element expDefaultsSchemeNode;
	
	private NodeList schemeList;
	private Element schemeNode, schemeDefaultsNode;
	private Element toolTipNode;
	
	private String quickLookPath;
	private static final int TIME_POINT_LINE_LIMIT = 20;
	
	private SelectionProcessor processor;
	
	// Variables needed by SelectionProcessor
	private String[] folderNames, folderTifPrefixes, excluded;
	private String[] selectedWells, selectedSites, selectedChannels;
	private String namingConvention, dummyTifName;
	private int[] timePointsEachFolder, selectedTimePoints;

	public Data(Document doc) {
		super();
		this.doc = doc;
		processor = new SelectionProcessor();
	}
	
	// Needed by HCA_Viewer opening dialog
	public String[] getAllExpNames() {
		return getNodeAttributes(getChildren(doc, XML.TAG_EXP), XML.ATT_NAME);
	}
	
	public int init(String chosenExp) {
		super.expName = chosenExp;
		HCA_Viewer.printToLog("Initializing data...");
		try {
			expDefaultsNode = (Element)getChildren(doc, XML.TAG_EXP_DEFAULTS).item(0);
			expDefaultsSchemeNode = (Element)getChildren(expDefaultsNode, XML.TAG_SCHEME).item(0);
			processScheme(expDefaultsSchemeNode);
			
			expNode = (Element)getNodeFromAtt(getChildren(doc, XML.TAG_EXP), XML.ATT_NAME, expName);
			schemeList = getChildren(expNode, XML.TAG_SCHEME);
			String[] names = getNodeAttributes(schemeList, XML.ATT_NAME);
			super.schemeNames = new String[names.length - 1];
			int k = 0;
			for(int i = 0; i < names.length; i++) {
				if(names[i].equals(XML.ATT_VAL_DEFAULT))
					continue;
				else
					super.schemeNames[k++] = names[i];
			}
			
			super.startScheme = getNodeAtt(expNode, XML.ATT_START_SCHEME);
			schemeDefaultsNode = (Element)getNodeFromAtt(schemeList, XML.ATT_NAME, XML.ATT_VAL_DEFAULT);
			processScheme(schemeDefaultsNode);
			
			schemeNode = (Element)getNodeFromAtt(schemeList, XML.ATT_NAME, super.startScheme);
			processScheme(schemeNode);
		} catch(Exception e) { super.message = "XML document error"; return super.ERROR; }
		
		if(!validateProcessing())
			return super.ERROR;
		HCA_Viewer.printToLog("Data initialized");
		
		return super.VALID;
	}
	
	// Executed by quickLookButton in ViewerPanel
	public void quickLook() {
		try {
			Runtime.getRuntime().exec(new String[] {"qlmanage", "-p", quickLookPath});
		} catch (IOException ioe) { }
	}
	
	public String getToolTip(String itemName) {		
		if(toolTipNode == null)
			return itemName;
		
		//StringBuilder toolTip = new StringBuilder("<html>[" + itemName + "]:");
		StringBuilder toolTip = new StringBuilder("<html>" + itemName + ":");
		String[] lines = getNodeAtt(toolTipNode, itemName).split(",");
		for(int i = 0; i < lines.length; i++)
			toolTip.append("<br>" + lines[i]);
		toolTip.append("</html>");
		
		return toolTip.toString();
	}
	
	// Change scheme, return bit mask indicating where GUI update is required
	public int changeScheme(String scheme) {
		HCA_Viewer.printToLog("Change scheme [ " + scheme + " ]");
		schemeNode = (Element)getNodeFromAtt(schemeList, XML.ATT_NAME, scheme);
		return processScheme(schemeNode);
	}
	
	public int validateSelection() {
		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < super.blocks.length; i++) {
			for(int j = 0; j < super.blocks[i].wellChecked.length; j++) {
				for(int k = 0; k < super.blocks[i].wellChecked[j].length; k++) {
					if(super.blocks[i].wellChecked[j][k])
						list.add(super.blocks[i].wellNames[j][k]);
				}
			}
		}
		selectedWells = new String[list.size()];
		selectedWells = list.toArray(selectedWells);
		
		list = new ArrayList<String>();
		for(int i = 0; i < super.optionGrid.itemNumbers[0].length; i++) {
			if(super.optionGrid.itemChecked[0][i])
				list.add(super.optionGrid.itemNumbers[0][i]);
		}
		selectedChannels = new String[list.size()];
		selectedChannels = list.toArray(selectedChannels);
		
		list = new ArrayList<String>();
		for(int i = 0; i < super.optionGrid.itemNumbers[1].length; i++) {
			if(super.optionGrid.itemChecked[1][i])
				list.add(super.optionGrid.itemNumbers[1][i]);
		}
		selectedSites = new String[list.size()];
		selectedSites = list.toArray(selectedSites);
		
		list = new ArrayList<String>();
		for(int i = 0; i < super.timeGrid.itemNumbers.length; i++) {
			for(int j = 0; j < super.timeGrid.itemNumbers[i].length; j++) {
				if(super.timeGrid.itemChecked[i][j])
					list.add(super.timeGrid.itemNumbers[i][j]);
			}
		}
		
		selectedTimePoints = new int[list.size()];
		for(int i = 0; i < selectedTimePoints.length; i++)
			selectedTimePoints[i] = Integer.parseInt(list.get(i));
		
		return processor.translateTifNames();
	}
	
	public void process(int operation) {
		processor.actionsImageJ(operation);
	}
	
	private int processScheme(Element schemeNode) {
		int update = 0;
		String[] blank = {""};
		String[][] optItemNames = super.optionGrid.itemNames;
		String[][] optItemNumbers = super.optionGrid.itemNumbers;
		
		String[] timePoints = blank;
		String timePointStyle = blank[0];
		
		NodeList children = schemeNode.getChildNodes();
		Node child;
		for(int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				String childName = child.getNodeName();
				
				// <root>
				if(childName.equals(XML.TAG_ROOT)) {
					super.expPath = getNodeAtt(child, XML.ATT_PATH);
					update |= super.UPDATE_EXP;
					continue;
				}
				
				// <wells>
				if(childName.equals(XML.TAG_WELLS)) {
					NodeList blockList = getChildren(child, XML.TAG_BLOCK);
					super.blocks = createBlocks(getNodeAttributes(blockList, XML.ATT_GRID),
							getNodeAttributes(blockList, XML.ATT_LAYOUT_TEXT));
					update |= super.UPDATE_WELL;
					continue;
				}
				
				// <toolTip>
				if(childName.equals(XML.TAG_TOOL_TIP)) {
					this.toolTipNode = (Element)child;
					update |= super.UPDATE_WELL;
					continue;
				}
				
				// <channels>
				if(childName.equals(XML.TAG_CHANNELS)) {
					optItemNames[0] = getNodeAtt(child, XML.ATT_NAME).split(",");
					String[][] split = splitNumbers(getNodeAtt(child, XML.ATT_NUMBER), "");
					optItemNumbers[0] = split[1];
					update |= super.UPDATE_OPT;
					continue;
				}
				
				// <sites>
				if(childName.equals(XML.TAG_SITES)) {
					String[][] split = splitNumbers(getNodeAtt(child, XML.ATT_NUMBER), "Site ");
					optItemNames[1] = split[0];
					optItemNumbers[1] = split[1];
					update |= super.UPDATE_OPT;
					continue;
				}
				
				// <tifNamingConvention>
				if(childName.equals(XML.TAG_TIF_NAMING_CONVENTION)) {
					this.namingConvention = getNodeAtt(child, XML.ATT_NAMING_CONVENTION);
					continue;
				}
				
				// <tif>
				if(childName.equals(XML.TAG_TIF)) {
					this.folderNames = getNodeAtt(child, XML.ATT_FOLDERS).split(",");
					this.folderTifPrefixes = getNodeAtt(child, XML.ATT_TIF_PREFIX).split(",");
					String[] tp = getNodeAtt(child, XML.ATT_TP).split(",");
					this.timePointsEachFolder = new int[tp.length];
					try {
						for(int j = 0; j < tp.length; j++)
							this.timePointsEachFolder[j] = Integer.parseInt(tp[j]);
					} catch(Exception e) { this.timePointsEachFolder = null; }
					continue;
				}
				
				// <timePoints>
				if(childName.equals(XML.TAG_TIME_POINTS)) {
					timePointStyle = getNodeAtt(child, XML.ATT_STYLE);
					String[][] split = splitNumbers(getNodeAtt(child, XML.ATT_TP), "");
					timePoints = split[1];
					continue;
				}
				
				// <exclusions>
				if(childName.equals(XML.TAG_EXCLUSIONS)) {
					this.excluded = getNodeAtt(child, XML.ATT_EXCLUDED).split(",");
					if(this.excluded[0].equals(XML.ATT_VAL_NONE))
						this.excluded = new String[0];
					continue;
				}
				
				// <dummyTif>
				if(childName.equals(XML.TAG_DUMMY_TIF)) {
					this.dummyTifName = getNodeAtt(child, XML.ATT_PATH);
					continue;
				}
				
				// <quickLook>
				if(childName.equals(XML.TAG_QUICK_LOOK)) {
					super.quickLookString = getNodeAtt(child, XML.ATT_NAME);
					this.quickLookPath = getNodeAtt(child, XML.ATT_PATH);
					update |= super.UPDATE_EXP;
					continue;
				}
			}
		}
		
		super.optionGrid = new ItemGrid(new String[] {"Channel(s)", "Site(s)"},
				optItemNames, optItemNumbers);
		
		if((timePointStyle != blank[0]) && (this.timePointsEachFolder != null) &&
				(timePoints != blank)) {
			
			int totalTimePoints = 0;
			for(int j = 0; j < this.timePointsEachFolder.length; j++)
				totalTimePoints += this.timePointsEachFolder[j];
			
			if(timePoints.length > totalTimePoints) {
				super.timeGrid = null;
				return update;
			}
			
			if(timePoints.length < totalTimePoints)
				timePointStyle = XML.ATT_VAL_WRAP;
			
			String[][]items = null;
			try {
				if(timePointStyle.equals(XML.ATT_VAL_BY_FOLDER)) {
					items = new String[this.timePointsEachFolder.length][];
					int l = 0;
					for(int j = 0; j < items.length; j++) {
						items[j] = new String[this.timePointsEachFolder[j]];
						for(int k = 0; k < items[j].length; k++)
							items[j][k] = timePoints[l++];
					}
				}
				
				else if(timePointStyle.equals(XML.ATT_VAL_WRAP)) {
					int lines = (timePoints.length / TIME_POINT_LINE_LIMIT);
					int rem = (timePoints.length % TIME_POINT_LINE_LIMIT);
					if(rem == 0)
						items = new String[lines][];
					else
						items = new String[lines + 1][];
					
					int j = 0, k = 0, l = 0;
					for(j = 0; j < lines; j++) {
						items[j] = new String[TIME_POINT_LINE_LIMIT];
						for(k = 0; k < items[j].length; k++)
							items[j][k] = timePoints[l++];
					}
					if(rem != 0) {
						items[j] = new String[rem];
						for(k = 0; k < rem; k++)
							items[j][k] = timePoints[l++];
					}
				}
				
				if(items != null) {
					super.timeGrid = new ItemGrid(new String[] {"", ""}, items, items);
					update |= super.UPDATE_TIME;
				} else
					super.timeGrid = null;
			} catch(Exception e) { super.timeGrid = null; }
		} else
			super.timeGrid = null;
		
		return update;
	}
	
	private Block[] createBlocks(String[] grid, String[] layoutText) {
		int x = 0, y = 0, rows = 0, cols = 0;
		Block[] newBlocks = new Block[grid.length];
		try {
			for(int i = 0; i < grid.length; i++) {
				String[] split = grid[i].toUpperCase().split(":");
				x = Integer.parseInt(split[0].substring(1)) - 1;
				y = (split[0].charAt(0) - 'A');
				rows = (split[1].charAt(0) - 'A') - y + 1;
				cols = Integer.parseInt(split[1].substring(1)) - x;
				newBlocks[i] = new Block(x, y, rows, cols, layoutText[i]);
			}
		} catch(Exception e) { return null; }
		
		// Check for overlap
		int[][] wells = new int[16][24];
		for(int i = 0; i < wells.length; i++) {
			for(int j = 0; j < wells[i].length; j++)
				wells[i][j] = 0;
		}
		
		for(int i = 0; i < newBlocks.length; i++) {
			for(int j = 0; j < newBlocks[i].rows; j++) {
				for(int k = 0; k < newBlocks[i].cols; k++)
					wells[newBlocks[i].y + j][newBlocks[i].x + k] += 1;
			}
		}
		
		for(int i = 0; i < wells.length; i++) {
			for(int j = 0; j < wells[i].length; j++) {
				if(wells[i][j] > 1)
					return null;
			}
		}
		
		return newBlocks;
	}
	
	private String[][] splitNumbers(String str, String optionName) {
		StringBuilder sb1 = new StringBuilder(56);
		StringBuilder sb2 = new StringBuilder(24);
		String[] commaSplit = str.split(",");
		try {
			for(int i = 0; i < commaSplit.length; i++) {
				String[] colonSplit = commaSplit[i].split(":");
				if(colonSplit.length == 1) {
					sb1.append(optionName + colonSplit[0] + ":");
					sb2.append("" + colonSplit[0] + ":");
				}
				else if(colonSplit.length == 2) {
					int start = Integer.parseInt(colonSplit[0]);
					int end = Integer.parseInt(colonSplit[1]);
					for(int j = start; j <= end; j++) {
						sb1.append(optionName + j + ":");
						sb2.append("" + j + ":");
					}
				}
			}
		} catch(Exception e) { return new String[][] {{""}, {""}}; }
		
		return new String[][] {sb1.toString().split(":"), sb2.toString().split(":")};
	}
	
	private boolean validateProcessing() {
		boolean status = true;
		StringBuilder sb = new StringBuilder("Xml Error\n");
		if(super.blocks == null) {
			sb.append("Incorrect format: block information\n");
			status = false;
		}
		if(super.timeGrid == null) {
			sb.append("Incorrect format: time point information\n");
			status = false;
		}
		
		if(status == false)
			super.message = sb.toString();
		
		return status;
	}
	
	private String getNodeAtt(Node node, String att) {
		String attValue = "";
		NamedNodeMap attMap = node.getAttributes();
		if(attMap != null) {
			Node item = attMap.getNamedItem(att);
			if(item != null)
				attValue = item.getNodeValue();
		}
		return attValue;
	}
	
	// Method to extract attribute values given a list and given attribute
	private String[] getNodeAttributes(NodeList list, String att) {
		String[] atts = new String[list.getLength()];
		for(int i = 0; i < atts.length; i++)
			atts[i] = getNodeAtt(list.item(i), att);
		return atts;
	}
	
	// Method to retrieve node with given attribute value
	private Node getNodeFromAtt(NodeList list, String att, String attValue) {
		Node listItem, attNode;
		int listLength = list.getLength();
		for(int i = 0; i < listLength; i++) {
			listItem = list.item(i);
			if(listItem.hasAttributes()) {
				attNode = listItem.getAttributes().getNamedItem(att);
				if(attNode != null && attNode.getNodeValue().equals(attValue))
					return listItem;
			}
		}
		return null;
	}
	
	// Method to get list of children with given XML tag
	private NodeList getChildren(Node root, String tag) {
		short type = root.getNodeType();
		if(type == Node.ELEMENT_NODE)
			return ((Element)root).getElementsByTagName(tag);
		if(type == Node.DOCUMENT_NODE)
			return ((Document)root).getElementsByTagName(tag);
		return root.getChildNodes();
	}
	
	
	public class SelectionProcessor{
		/*
		
		Methods provided:
		* public static int translateTifNames(): either return VALID without setting message, or WARNING/ERROR after setting message
		* public static void actionsImageJ(int operation): actions: open, open as stack, or copy
		
		
		Fields taken from enclosing class as inputs:
		
	    * String expPath
	    * String[] folderNames
	    * String[] folderTifPrefixes
	    * int[] timePointsEachFolder
	    * String namingConvention
	    * String[] selectedWells
	    * String[] selectedSites
	    * String[] selectedChannels
	    * int[] selectedTimePoints
	    * String dummyTifName
	    * String[] excluded
		* VALID
		* WARNING
		* ERROR 
		* OPEN
		* COPY
		* openAsStack
		
		Methods taken from enclosing class:
		* HCA_Viewer.printToLog()
		
		Fields taken from enclosing class as outputs:
		* message
		
		
		*/

	    public static final int maxNumOfTifsBeforeWarning = 100;
	    public static final String newlineIndicator = "\n";
		private static final String pathSep = "/";

		private int numSelectedWells,numSelectedSites,numSelectedChannels,numSelectedTPs;
		private int numExclPatterns;
		private int totalNumTifs;
		private String[] selectedTifNamesWithFullPath,selectedTifNamesNew;
		

		private Overlay overlay;
		private Font cellMarkerFont;
	
		
		StringBuilder returnMsgWarning;
		StringBuilder returnMsgERROR;
		
		public SelectionProcessor() {
			overlay = new Overlay();
			cellMarkerFont = new Font("Arial", Font.PLAIN, 15);
		}
		
		
		public int translateTifNames(){
			
			returnMsgWarning = new StringBuilder("Do you still want to proceed with these WARNINGS?");
			returnMsgWarning.append(newlineIndicator);
			returnMsgWarning.append(newlineIndicator);
			returnMsgERROR = new StringBuilder("ERROR");
			returnMsgERROR.append(newlineIndicator);
			returnMsgERROR.append(newlineIndicator);
			
			numSelectedWells = selectedWells.length;
			numSelectedSites = selectedSites.length;
			numSelectedChannels = selectedChannels.length;
			numSelectedTPs = selectedTimePoints.length;
			numExclPatterns = excluded.length;
			
			totalNumTifs = numSelectedWells*numSelectedSites*numSelectedChannels*numSelectedTPs;
			
						
			int validationStatus = selectionValidation();
			
			if (validationStatus == VALID) {
				selectedTifNamesWithFullPath = new String[totalNumTifs];
				selectedTifNamesNew = new String[totalNumTifs];			
				
				//cumulative time points
				int numFolders = folderNames.length;
				int [] cumsumTP = new int[numFolders];
				cumsumTP[0] = 0;
				for (int i=1; i<numFolders; i++) {
					cumsumTP[i] = cumsumTP[i-1]+timePointsEachFolder[i-1];
				}
				
				int tifCounter = 0;
				for (int i=0; i<numSelectedTPs; i++) {

					int t = selectedTimePoints[i];
					int seqTPfolder = numFolders;

					int tp=0;
					String folder = "",prefix = "";
					while (seqTPfolder >0){
						if (t>cumsumTP[seqTPfolder-1]){
							folder = folderNames[seqTPfolder-1];
							tp = t-cumsumTP[seqTPfolder-1];
							prefix = folderTifPrefixes[seqTPfolder-1];
							break;
						}
						seqTPfolder--;
					}


					for (int j=0; j<numSelectedWells; j++) {
						for (int s=0; s<numSelectedSites; s++) {
							for (int c=0; c<numSelectedChannels; c++) {
								
								if (namingConvention.equals(XML.ATT_VAL_RFTPWSC)){
									String tifname_wsc = selectedWells[j]+"_s"+selectedSites[s]+"_w"+selectedChannels[c];
									selectedTifNamesWithFullPath[tifCounter] = expPath+folder+pathSep+"TimePoint_"+tp+pathSep+prefix+tifname_wsc+".TIF";
									selectedTifNamesNew[tifCounter] = tifname_wsc+"_t"+String.format("%02d", t);
								}
								else if (namingConvention.equals(XML.ATT_VAL_RFTPWS)){
									String tifname_wsc = selectedWells[j]+"_s"+selectedSites[s];
									selectedTifNamesWithFullPath[tifCounter] = expPath+folder+pathSep+"TimePoint_"+tp+pathSep+prefix+tifname_wsc+".TIF";
									selectedTifNamesNew[tifCounter] = tifname_wsc+"_t"+String.format("%02d", t);
								}
								else {
									message = "Naming convention not defined: "+namingConvention;
									return ERROR;
								}
								
								if (thisTifExcluded(selectedTifNamesNew[tifCounter])) {
									HCA_Viewer.printToLog("Excluded: "+selectedTifNamesNew[tifCounter]);
									
									validationStatus = WARNING;
									returnMsgWarning.append("Not scanned: "+selectedTifNamesNew[tifCounter]+" Intended for "+selectedTifNamesWithFullPath[tifCounter]);
									returnMsgWarning.append(newlineIndicator);
									
									selectedTifNamesWithFullPath[tifCounter] = dummyTifName;
								}
															
								tifCounter++;
							}
						}
					}
				}
				
				
			}
			
			if (validationStatus == ERROR) 
				message = returnMsgERROR.toString();
			else if (validationStatus == WARNING) 
				message = returnMsgWarning.toString();
			
			return validationStatus;
		}
		
		public void actionsImageJ(int operation){
			if (operation == COPY) {
				//get the destination folder from user
				HCA_Viewer.printToLog("Selecting destination folder...");
				DirectoryChooser dirchooser = new DirectoryChooser("Choose a destination folder");
				String destFolder = dirchooser.getDirectory();
				HCA_Viewer.printToLog("Destination folder: "+destFolder);
				
				//copy files
				String originalTifFullname,copyAsTifNewname;
				for (int i=0; i<totalNumTifs; i++) {
					originalTifFullname = selectedTifNamesWithFullPath[i];
					copyAsTifNewname = destFolder+selectedTifNamesNew[i]+".TIF";
					HCA_Viewer.printToLog("Copying: "+originalTifFullname+"\t to: "+copyAsTifNewname);
					
					try {
						Runtime.getRuntime().exec(new String[] {"cp", originalTifFullname, copyAsTifNewname});
					} catch (IOException e) { }
				}					
			}
			
			else { //This is an OPEN operation
				if (openAsStack) {
					//This is the old stack opener
						// String listTempFileName = "HCA_temp_list.txt";
						// IJ.runMacro("if (File.exists('"+listTempFileName+"')) File.delete('"+listTempFileName+"')");
						// 		
						// for (int i=0; i<totalNumTifs; i++) {
						// 	HCA_Viewer.printToLog("Listing: "+selectedTifNamesWithFullPath[i]);
						// 	IJ.runMacro("File.append('"+selectedTifNamesWithFullPath[i]+"','"+listTempFileName+"')");
						// 	// stackTitles[stackSliceCounter++] = newtitle;
						// }
						// 
						// IJ.runMacro("run(\"Stack From List...\",\"open="+listTempFileName+"\")");
						// 
						// //reset slice titles and get range for the stack
						// double stackMin=65535,stackMax=-1;
						// for (int i=1; i<=totalNumTifs; i++) {
						// 	IJ.runMacro("setSlice("+i+")");	
						// 	IJ.runMacro("setMetadata(\"Label\",'"+ selectedTifNamesNew[i-1]+"')");
						// 
						// 	ImagePlus imp = IJ.getImage();
						// 	ImageStatistics stats = imp.getStatistics(16,100); //ATTN 16 for MIN_MAX
						// 	HCA_Viewer.printToLog(selectedTifNamesNew[i-1]+": min "+stats.min+", max "+ stats.max);
						// 
						// 	if (stats.min < stackMin){
						// 		stackMin = stats.min;
						// 	}
						// 	if (stats.max > stackMax){
						// 		stackMax = stats.max;
						// 	}
						// }
						// 
						// //reset stack range
						// IJ.runMacro("setMinAndMax("+stackMin+","+stackMax+")");
						// IJ.runMacro("rename(\"Selected tifs\")");
						// 
						// }		
						// HCA_Viewer.printToLog("delete success: ");
						// new File(listTempFileName).delete();
						
					//This is the new stack opener
					//
					//
					//setup by getting the first slice
					HCA_Viewer.printToLog("Stack opening: "+selectedTifNamesWithFullPath[0]);
					IJ.showStatus("1/"+totalNumTifs);
					ImagePlus imp1 = new ImagePlus(selectedTifNamesWithFullPath[0]);
					ImageStatistics stats1 = imp1.getStatistics(16,100); //ATTN 16 for MIN_MAX					
					double stackMin=stats1.min,stackMax=stats1.max;
					ImageStack stack = new ImageStack(imp1.getWidth(),imp1.getHeight(),1);
					stack.addSlice(selectedTifNamesNew[0],imp1.getChannelProcessor());
					//read rest of the slices
					for (int i=1; i<totalNumTifs; i++) {
						HCA_Viewer.printToLog("Stack opening: "+selectedTifNamesWithFullPath[i]);
						
						int count = i+1;
						IJ.showStatus(count+"/"+totalNumTifs);

						ImagePlus imp = new ImagePlus(selectedTifNamesWithFullPath[i]);
						//Equivalently: ImagePlus imp = new Opener().openImage(selectedTifNamesWithFullPath[i]);

						stack.addSlice(selectedTifNamesNew[i],imp.getChannelProcessor());						
						
						//min and max
						ImageStatistics stats = imp.getStatistics(16,100); //ATTN 16 for MIN_MAX
						if (stats.min < stackMin){
							stackMin = stats.min;
						}
						if (stats.max > stackMax){
							stackMax = stats.max;
						}						
					}
					stack.deleteSlice(1);

					ImagePlus imp = new ImagePlus("Selected tifs",stack);
					imp.setDisplayRange(stackMin,stackMax);
					
					//overlays
					overlay.clear();
					for (int i=0; i<totalNumTifs; i++) {
						String thisTifID = selectedTifNamesNew[i];
						String[] parts = thisTifID.split("_");
						String overlayFileName = expPath+"cellMarkers"+pathSep+parts[0]+"_"+parts[1]+".csv";
						File overlayFile = new File(overlayFileName);
						
						int currentTP=-1;
						String tpStr = parts[parts.length-1];
						if (!tpStr.startsWith("t"))
							IJ.error("error: tifID "+thisTifID+" needs to end with _txx");
						else {
							currentTP = Integer.parseInt(tpStr.substring(1));
							HCA_Viewer.printToLog("current time: "+tpStr+"-"+tpStr.substring(2)+"("+currentTP+")");
						}
						
						if (overlayFile.exists()) {
							HCA_Viewer.printToLog("Reading overlay info for: "+thisTifID+": looking for "+overlayFileName+"   ... Found!");
							
							String filestring = IJ.openAsString(overlayFileName);
							String[] rows = filestring.split("\n");
							for (int r=0;r<rows.length; r++) {
								String[] columns=rows[r].split(",");
								int tp =Integer.parseInt(columns[0]);
								if (tp == currentTP) {
									double posX = Double.parseDouble(columns[2]);
									double posY = Double.parseDouble(columns[3]);
									TextRoi textRoi = new TextRoi(posX, posY, columns[1], cellMarkerFont);
									HCA_Viewer.printToLog("time: "+tp+": ("+posX+","+posY+"): "+columns[1]);
									textRoi.setPosition(i+1);
									textRoi.setStrokeColor(Color.green);
									textRoi.setJustification(TextRoi.CENTER);
									//textRoi.setNonScalable(true);
									overlay.add(textRoi);
								}
							}							
						}
						else {
							HCA_Viewer.printToLog("Reading overlay info for: "+thisTifID+": looking for "+overlayFileName+"   ... NOT Found!");				
						}
						
					}
					HCA_Viewer.printToLog("Total number of ROI markers: "+overlay.size());
					if (overlay.size() > 0)
						imp.setOverlay(overlay);
					
					imp.show();
					
					//This is untouched
					if(showBC)
						IJ.runMacro("run(\"Brightness/Contrast...\")");
					if(autoPlay) {
						//IJ.runMacro("run(\"Animation Options...\", \"speed=2 first=1 last="+totalNumTifs+" start\")");
						IJ.runMacro("run(\"Animation Options...\", \"speed=2 first=1 last="+totalNumTifs+"\")");
						IJ.runMacro("doCommand(\"Start Animation [\\\\]\");");
					}

					
				}
				else { //open as separate windows
					for (int i=0; i<totalNumTifs; i++) {
						String originalTifFullname = selectedTifNamesWithFullPath[i];
						HCA_Viewer.printToLog("Opening: "+originalTifFullname);
						
						Opener opener = new Opener();  
						ImagePlus imp = opener.openImage(originalTifFullname);  
						String oldtitle = imp.getTitle();
						imp.setTitle(selectedTifNamesNew[i]+" ("+oldtitle+")");
						
						imp.show();
					}
														
				}
			}
					
			HCA_Viewer.printToLog("\nEnd of session\n**************************************\n\n");
		}
		
		
		//private methods
		
		private int selectionValidation() {
			int returnStatus = VALID;
			
			if (numSelectedWells < 1) {
				returnStatus = ERROR;
				returnMsgERROR.append("Error: No wells was selected");
				returnMsgERROR.append(newlineIndicator);
			}
			
			if (numSelectedSites < 1) {
				returnStatus = ERROR;
				returnMsgERROR.append("Error: No sites was selected");
				returnMsgERROR.append(newlineIndicator);
			}
			
			if (numSelectedChannels < 1) {
				returnStatus = ERROR;
				returnMsgERROR.append("Error: No channels was selected");
				returnMsgERROR.append(newlineIndicator);
			}
			
			if (numSelectedTPs < 1) {
				returnStatus = ERROR;
				returnMsgERROR.append("Error: No time points was selected");
				returnMsgERROR.append(newlineIndicator);
			}
			
			
			totalNumTifs = numSelectedWells*numSelectedSites*numSelectedChannels*numSelectedTPs;
			if (totalNumTifs >= maxNumOfTifsBeforeWarning) {
				returnStatus = WARNING;
				returnMsgWarning.append("You are trying to open ");
				returnMsgWarning.append(totalNumTifs);
				returnMsgWarning.append(" tif files. Proceed?");
				returnMsgWarning.append(newlineIndicator);
			}
			

			
			return returnStatus;
		}
		
		private boolean thisTifExcluded(String x){
			// return false;
			
			boolean matched = false;
			for (int i=0; i<numExclPatterns; i++) {
				Pattern p = Pattern.compile(excluded[i]);
				Matcher m = p.matcher(x);
				boolean b = m.matches();
				
				if (b) {
					matched = true;
					break;
				}
			}
			
			return matched;
		}
	}
}