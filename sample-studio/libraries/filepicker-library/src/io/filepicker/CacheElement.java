package io.filepicker;

public class CacheElement {
	private Folder data;
	public CacheElement(Folder data) {
		this.data = data;
	}
	
	public Folder getData() {
		return this.data;
	}
}
