package io.filepicker;

public class FPFile {
	private String localpath;
	private String fpurl;

	public FPFile(String localpath, String fpurl) {
		this.localpath = localpath;
		this.fpurl = fpurl;
	}

	public String getLocalPath() {
		return this.localpath;
	}

	public String getFPUrl() {
		return this.fpurl;
	}
}
