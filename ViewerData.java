/**
 * ViewerData.java
 * @author Brendan
 * 
 * class defines fields needed by ViewerPanel
 */


public abstract class ViewerData {
	
	protected String expName, expPath, quickLookString;
	protected String[] schemeNames;
	protected String startScheme;
	protected Block[] blocks;
	protected ItemGrid optionGrid, timeGrid;
	protected boolean openAsStack, showBC, autoPlay;
	
	protected String message;
	
	public static final int UPDATE_EXP = 1, UPDATE_WELL = 2, UPDATE_TIME = 4, UPDATE_OPT = 8;
	public static final int VALID = 0, WARNING = -1, ERROR = -2;
	public static final int OPEN = 0, COPY = 1;
	
	protected ViewerData() {
		String blank = "";
		expName = blank;
		expPath = blank;
		quickLookString = blank;
		schemeNames = new String[] {""};
		startScheme = blank;
		blocks = new Block[] {new Block(0, 0, 16, 24, "")};
		optionGrid = new ItemGrid(new String[] {""}, new String[][] {{""},{""}},
				new String[][] {{""},{""}});
		timeGrid = new ItemGrid(new String[] {""}, new String[][] {{""},{""}},
				new String[][] {{""},{""}});
		openAsStack = false;
		showBC = true;
		autoPlay = true;
		message = blank;
	}
	
	public abstract void quickLook();
	
	public abstract String getToolTip(String itemName);
	
	public abstract int changeScheme(String scheme);
	
	public abstract int validateSelection();
	
	public abstract void process(int operation);
	
	
	public void setOpenAsStack(boolean openAsStack) {
		this.openAsStack = openAsStack;
	}
	
	public class Block {
		public int x, y, rows, cols;
		public String layoutText;
		public boolean[][] wellChecked;
		
		public String[][] wellNames;
		
		public Block(int x, int y, int rows, int cols, String layoutText) {
			this.x = x; this.y = y; this.rows = rows; this.cols = cols;
			this.layoutText = layoutText;
			wellChecked = new boolean[rows][cols];
			wellNames = new String[rows][cols];
			String blank = "";
			for(int i = 0; i < rows; i++) {
				for(int j = 0; j < cols; j++) {
					wellChecked[i][j] = false;
					wellNames[i][j] = blank;
				}
			}
		}
	}
	
	public class ItemGrid {
		public String[] labels;
		public String[][] itemNames, itemNumbers;
		public boolean[][] itemChecked;
		
		public ItemGrid(String[] labels, String[][] itemNames, String[][] itemNumbers) {
			this.labels = labels;
			this.itemNames = itemNames;
			this.itemNumbers = itemNumbers;
			itemNumbers = new String[itemNames.length][];
			itemChecked = new boolean[itemNames.length][];
			for(int i = 0; i < itemChecked.length; i++) {
				itemChecked[i] = new boolean[itemNames[i].length];
				itemNumbers[i] = new String[itemNames[i].length];
				for(int j = 0; j < itemChecked[i].length; j++)
					itemChecked[i][j] = false;
			}
		}
	}
}
