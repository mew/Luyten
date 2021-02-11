package us.deathmarine.luyten;

/**
 * Do not instantiate this class, get the instance from ConfigSaver. All
 * not-static fields will be saved automatically named by the field's java
 * variable name. (Watch for collisions with existing IDs defined in
 * ConfigSaver.) Only String, boolean and int fields are supported. Write
 * default values into the field declarations.
 */
public class LuytenPreferences {
	private String theme = "Darcula";
	private String fileOpenCurrentDirectory = "";
	private String fileSaveCurrentDirectory = "";
	private int font_size = 10;

	private boolean isPackageExplorerStyle = true;
	private boolean isFilterOutInnerClassEntries = true;
	private boolean isSingleClickOpenEnabled = true;
	private boolean isExitByEscEnabled = false;

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public String getFileOpenCurrentDirectory() {
		return fileOpenCurrentDirectory;
	}

	public void setFileOpenCurrentDirectory(String fileOpenCurrentDirectory) {
		this.fileOpenCurrentDirectory = fileOpenCurrentDirectory;
	}

	public String getFileSaveCurrentDirectory() {
		return fileSaveCurrentDirectory;
	}

	public void setFileSaveCurrentDirectory(String fileSaveCurrentDirectory) {
		this.fileSaveCurrentDirectory = fileSaveCurrentDirectory;
	}

	public boolean isPackageExplorerStyle() {
		return isPackageExplorerStyle;
	}

	public void setPackageExplorerStyle(boolean isPackageExplorerStyle) {
		this.isPackageExplorerStyle = isPackageExplorerStyle;
	}

	public boolean isFilterOutInnerClassEntries() {
		return isFilterOutInnerClassEntries;
	}

	public void setFilterOutInnerClassEntries(boolean isFilterOutInnerClassEntries) {
		this.isFilterOutInnerClassEntries = isFilterOutInnerClassEntries;
	}

	public boolean isSingleClickOpenEnabled() {
		return isSingleClickOpenEnabled;
	}

	public void setSingleClickOpenEnabled(boolean isSingleClickOpenEnabled) {
		this.isSingleClickOpenEnabled = isSingleClickOpenEnabled;
	}

	public boolean isExitByEscEnabled() {
		return isExitByEscEnabled;
	}

	public void setExitByEscEnabled(boolean isExitByEscEnabled) {
		this.isExitByEscEnabled = isExitByEscEnabled;
	}

	public int getFont_size() {
		return font_size;
	}

	public void setFont_size(int font_size) {
		this.font_size = font_size;
	}
}
