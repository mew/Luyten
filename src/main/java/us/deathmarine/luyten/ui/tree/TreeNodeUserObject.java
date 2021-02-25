package us.deathmarine.luyten.ui.tree;

public class TreeNodeUserObject {

	private String originalName;
	private String displayName;
	private byte[] file;

	public TreeNodeUserObject(String name, byte[] file) {
		this(name, name, file);
	}

	public TreeNodeUserObject(String originalName, String displayName, byte[] file) {
		this.originalName = originalName;
		this.displayName = displayName;
		this.file = file;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

    public byte[] getFile() {
        return file;
    }

    @Override
	public String toString() {
		return displayName;
	}
}
