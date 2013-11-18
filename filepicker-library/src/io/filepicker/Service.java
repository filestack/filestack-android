package io.filepicker;

class Service extends Inode {
	private String[] mimetypes;
	private boolean saveSupported;
	private String id;

	public Service(String displayName, String path, String[] mimetypes,
			int drawable, boolean saveSupported, String id) {
		super(displayName, path, true, drawable);
		this.mimetypes = mimetypes;
		this.saveSupported = saveSupported;
		this.id = id;
	}

	public String[] getMimetypes() {
		return this.mimetypes;
	}

	public boolean isSaveSupported() {
		return this.saveSupported;
	}

	public String getServiceId() {
		return this.id;
	}

}
