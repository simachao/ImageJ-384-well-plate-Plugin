/**
 * XML.java
 * @author Brendan Horan
 * 
 * Interface defining XML data tags
 */


public interface XML {
	
	// Default path for XML data file
//	public static final String DEFAULT_DIR = "/Applications/ImageJ/plugins/HCA/";
// public static final String DEFAULT_DIR = "/Volumes/bittner/HCA/Documents/ImageJ_plugin/";
	public static final String DEFAULT_DIR = "/Volumes/Images/ImageJ/";
	public static final String DEFAULT_NAME = "HCA_Viewer_DATA.xml";
	
	// XML tags
	public static final String TAG_BLOCK = "block";
	public static final String TAG_CHANNELS = "channels";
	public static final String TAG_DUMMY_TIF = "dummyTif";
	public static final String TAG_EXCLUSIONS = "exclusions";
	public static final String TAG_EXP_DEFAULTS = "experiment_defaults";
	public static final String TAG_EXP = "experiment";
	public static final String TAG_FOLDER = "folder";
	public static final String TAG_QUICK_LOOK = "quickLook";
	public static final String TAG_ROOT = "root";
	public static final String TAG_SCHEME = "scheme";
	public static final String TAG_SITES = "sites";
	public static final String TAG_TIF = "tif";
	public static final String TAG_TIF_NAMING_CONVENTION = "tifNamingConvention";
	public static final String TAG_TIME_POINTS = "timePoints";
	public static final String TAG_TOOL_TIP = "toolTip";
	public static final String TAG_WELLS = "wells";
	
	// XML attributes
	public static final String ATT_EXCLUDED = "excluded";
	public static final String ATT_FOLDERS = "folders";
	public static final String ATT_GRID = "grid";
	public static final String ATT_LAYOUT_TEXT = "layoutText";
	public static final String ATT_NAME = "name";
	public static final String ATT_NAMING_CONVENTION = "namingConvention";
	public static final String ATT_NUMBER = "number";
	public static final String ATT_PATH = "path";
	public static final String ATT_START_SCHEME = "startScheme";
	public static final String ATT_STYLE = "style";
	public static final String ATT_TIF_PREFIX = "tif_prefix";
	public static final String ATT_TP = "tp";
	
	// XML attribute values
	public static final String ATT_VAL_BY_FOLDER = "byFolder";
	public static final String ATT_VAL_DEFAULT = "default";
	public static final String ATT_VAL_NONE = "NONE";
	public static final String ATT_VAL_WRAP = "wrap";
	public static final String ATT_VAL_RFTPWSC = "root:folder:timepoint:prefix:well:site:channel";
	public static final String ATT_VAL_RFTPWS = "root:folder:timepoint:prefix:well:site";
}
