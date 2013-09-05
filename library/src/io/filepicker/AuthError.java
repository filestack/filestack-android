package io.filepicker;

public class AuthError extends Exception {
	private static final long serialVersionUID = 3873865053998671409L;
	private final String path;
	private final String service;

	public AuthError(String path, String service) {
		this.path = path;
		this.service = service;
	}

	public String getPath() {
		return this.path;
	}

	public String getService() {
		return this.service;
	}
}
