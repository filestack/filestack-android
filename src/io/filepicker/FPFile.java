package io.filepicker;

public class FPFile {
	private String localpath;
	private String fpurl;
	private String key;

	public FPFile(String localpath, String fpurl) {
		this(localpath, fpurl, null);
	}
	
	public FPFile(String localpath, String fpurl, String key) {
		this.localpath = localpath;
		this.fpurl = fpurl;
		this.key = key;
	}

	public String getLocalPath() {
		return this.localpath;
	}

	public String getFPUrl() {
		return this.fpurl;
	}
	
	public String getKey() {
		return this.key;
	}
}
