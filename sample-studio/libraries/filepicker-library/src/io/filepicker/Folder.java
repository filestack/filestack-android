package io.filepicker;

public class Folder {
	private final Inode[] inodes;
	private final String view;
	private final String name;

	public Folder(Inode[] inodes, String view, String name) {
		this.inodes = inodes;
		this.view = view;
		this.name = name;
	}

	public Inode[] getInodes() {
		return this.inodes;
	}

	public String getView() {
		return this.view;
	}

	public String getName() {
		return this.name;
	}
}
