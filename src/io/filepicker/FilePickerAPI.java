package io.filepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.params.HttpParams;
import org.apache.http.conn.scheme.SocketFactory;
import java.net.InetAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Folder {
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

class FPFile {
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

class AuthError extends Exception {
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

class FixedSizeList<T> {
	private final int maxSize;
	private final LinkedList<T> list = new LinkedList<T>();

	public FixedSizeList(int maxSize) {
		this.maxSize = maxSize < 0 ? 0 : maxSize; // don't make bigger than 0
	}

	public T add(T t) {
		list.add(t);
		return list.size() > maxSize ? list.remove() : null;
	}
}

public class FilePickerAPI {

	public final static String FPHOSTNAME = "www.filepicker.io";
	public final static String FPBASEURL = "https://" + FPHOSTNAME + "/";
	public static String FPAPIKEY = "";
	public final static String FILE_GET_JS_SESSION_PART = "{\"app\":{\"apikey\":\""
			+ FPAPIKEY + "\"}";
	public final static int REQUEST_CODE_AUTH = 600;
	public final static int REQUEST_CODE_GETFILE = 601;
	public final static int REQUEST_CODE_SAVEFILE = 602;
	protected final static int REQUEST_CODE_GETFILE_LOCAL = 603;

	private static FilePickerAPI filepickerapi = null;
	private HttpContext httpContext;
	private CookieStore cookieStore;
	private static FixedSizeList<PrecacheTask> precacheTaskList = new FixedSizeList<PrecacheTask>(
			16); // FIXME: arbitrary constant
	private static String TAG = "FilePickerAPI";
	private static boolean debug = false;

	public static FilePickerAPI getInstance() {
		if (filepickerapi == null)
			return filepickerapi = new FilePickerAPI();
		else
			return filepickerapi;
	}

	public static void setKey(String key) {
		FPAPIKEY = key;
	}

	protected static boolean isKeySet() {
		return FPAPIKEY.length() > 0;
	}

	protected static void debug(String msg) {
		if (debug)
			Log.d(TAG, msg);
	}

	private String getJSSession() {
		return FILE_GET_JS_SESSION_PART + "}";
	}

	private String getJSSessionWithOption(String s) {
		return FILE_GET_JS_SESSION_PART + "," + s + "}";
	}

	private String getJSSessionWithMimetypes(String mimetype) {
		return getJSSessionWithOption("\"mimetypes\": [\"" + mimetype + "\"]");
	}

	private FilePickerAPI() {
		cookieStore = new BasicCookieStore();
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	public void setSessionCookie(String sessionCookie) {
		BasicClientCookie cookie = new BasicClientCookie("session",
				sessionCookie);
		cookie.setDomain(FilePickerAPI.FPHOSTNAME);
		cookieStore.addCookie(cookie);
	}

	public ArrayList<Service> getProviders() {
		ArrayList<Service> services = new ArrayList<Service>();
		services.add(new Service("Gallery", "/Gallery/",
				new String[] { "image/*" }, R.drawable.glyphicons_008_film,
				false, ""));
		services.add(new Service("Camera", "/Camera/",
				new String[] { "image/jpg" }, R.drawable.glyphicons_011_camera,
				false, ""));
		services.add(new Service("Dropbox", "/Dropbox/",
				new String[] { "*/*" }, R.drawable.glyphicons_361_dropbox,
				true, "dropbox"));
		services.add(new Service("Facebook", "/Facebook/",
				new String[] { "image/*" }, R.drawable.glyphicons_390_facebook,
				true, "facebook"));
		services.add(new Service("Box", "/Box/", new String[] { "*/*" },
				R.drawable.glyphicons_154_show_big_thumbnails, true, "box"));
		services.add(new Service("Gmail", "/Gmail/", new String[] { "*/*" },
				R.drawable.glyphicons_399_email, false, "gmail"));
		services.add(new Service("Github", "/Github/", new String[] { "*/*" },
				R.drawable.glyphicons_381_github, false, "github"));
		services.add(new Service("Google Drive", "/GDrive/",
				new String[] { "*/*" }, R.drawable.gdrive, false, "github"));
		return services;
	}

	private boolean isInstanceOf(String child, String parent) {
		child = child.toLowerCase();
		parent = parent.toLowerCase();
		if (parent.equals("*/*"))
			return true;
		try {
			String p_base = parent.split("/")[0];
			if (parent.contains("*"))
				return child.startsWith(p_base);
			else
				return child.equals(parent);
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		return false;

	}

	public Service[] getProvidersForMimetype(String mimetype, boolean save) {
		ArrayList<Service> services = new ArrayList<Service>();
		for (Service s : getProviders()) {
			for (String m : s.getMimetypes()) {
				if (isInstanceOf(mimetype, m) || isInstanceOf(m, mimetype)) {
					if (!(save && !s.isSaveSupported())) {
						services.add(s);
						break;
					}
				}
			}
		}
		return services.toArray(new Service[services.size()]);
	}

	public Inode[] getProvidersForServiceArray(String[] selectedServices) {
		ArrayList<Service> services = new ArrayList<Service>();
		for (Service s : getProviders()) {
			for (String selectedService : selectedServices) {
				if (s.getDisplayName().equals(selectedService)) {
					services.add(s);
					break;
				}
			}
		}
		return services.toArray(new Service[services.size()]);
	}

	private Inode inodeForJSONObject(JSONObject content) throws JSONException {
		String displayName = content.getString("display_name");
		String path = content.getString("link_path");
		boolean is_dir = content.getBoolean("is_dir");
		Inode inode = new Inode(displayName, path, is_dir);
		boolean thumb_exists = content.optBoolean("thumb_exists", false);
		if (content.has("disabled"))
			inode.setDisabled(content.getBoolean("disabled"));
		String thumbnail = null;
		if (thumb_exists) {
			thumbnail = content.getString("thumbnail");
			if (!thumbnail.startsWith("http"))
				thumbnail = FPBASEURL + thumbnail; // pathUrlEncode(thumbnail).replace("%3F",
													// "?").replace(
			// "%3D", "=");
			inode.setThumb_exists(thumb_exists); // true
			inode.setThumbnail(thumbnail);
		} else if (is_dir) {
			// special case?
		}
		return inode;
	}

	public Folder parseFolder(String folderJSON, String path)
			throws JSONException, AuthError {
		JSONObject folder = new JSONObject(folderJSON);
		if (folder.has("auth")) {
			if (!folder.getBoolean("auth")) {
				// need to auth
				String service = folder.getString("client");
				throw new AuthError(path, service);
			}
		}
		if (folder.has("contents")) {
			JSONArray contents = folder.getJSONArray("contents");
			Inode[] inodes = new Inode[contents.length()];
			for (int i = 0; i < contents.length(); i++) {
				JSONObject content = contents.optJSONObject(i);
				inodes[i] = inodeForJSONObject(content);
			}

			String view;
			if (folder.has("view"))
				view = folder.getString("view");
			else
				view = "list";

			String filename = path;
			if (folder.has("filename"))
				filename = folder.getString("filename");

			return new Folder(inodes, view, filename);
		} else {
			return null;
		}
	}

	public Bitmap getThumbnail(String url) {
		try {
			HttpGet httpget = new HttpGet(url);
			AndroidHttpClient httpClient = getHttpClient();
			workAroundReverseDnsBugInHoneycombAndEarlier(httpClient);
			HttpResponse httpResponse;
			httpResponse = httpClient.execute(httpget, httpContext);
			if (httpResponse.getStatusLine().getStatusCode() != 200) {
				debug("Http error: "
						+ httpResponse.getStatusLine().getStatusCode());
				httpClient.close();
			} else {
				Bitmap bitmap = BitmapFactory.decodeStream(httpResponse
						.getEntity().getContent());
				httpClient.close();
				return bitmap;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String pathUrlEncode(String path) {
		try {
			return URLEncoder.encode(path, "utf-8").replace("+", "%20")
					.replace("%2F", "/");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return path;
	}

	class PrecacheTask extends AsyncTask<String, Integer, Folder> {

		@Override
		protected Folder doInBackground(String... args) {
			if (args.length != 2) {
				debug("Invalid arguments to precachetask");
				return null;
			}
			try {
				Folder folder = getPath(args[0], args[1]);
				if (folder == null)
					return null;
				DataCache.getInstance().put(args[0] + args[1], folder);
				return folder;
			} catch (AuthError e) {
				debug("Auth error: not caching");
				return null;
			}
		}
	}

	@SuppressLint("NewApi")
	public PrecacheTask precache(String path, String mimetypes) {
		if (DataCache.getInstance().get(path + mimetypes) != null)
			return null;
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		PrecacheTask task = new PrecacheTask();
		if (SDK_INT >= 12)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path,
					mimetypes);
		else
			task.execute(path, mimetypes);
		PrecacheTask oldTask = precacheTaskList.add(task);
		if (oldTask != null) {
			if (oldTask.getStatus() != AsyncTask.Status.FINISHED) {
				// oldTask.cancel(true);
				oldTask.cancel(false);
			}
		}
		return task;
	}

	public Folder getPath(String path, String mimetypes) throws AuthError {
		Log.d(FilePickerAPI.class.toString(), "path: " + path);
		Folder cached = DataCache.getInstance().get(path + mimetypes);
		if (cached != null)
			return cached;
		try {
			HttpGet httpget = new HttpGet(FPBASEURL
					+ "api/path"
					+ pathUrlEncode(path)
					+ "?format=info&js_session="
					+ URLEncoder.encode(getJSSessionWithMimetypes(mimetypes),
							"utf-8"));
			String response = getStringFromNetworkRequest(httpget);
			return parseFolder(response, path);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	public void unauth(Service service) {
		HttpGet httpget = new HttpGet(FPBASEURL + "api/client/" + service.getServiceId() + "/unauth/");
		try {
			getStringFromNetworkRequest(httpget);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File getTempFileForName(String filename, Context context) {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		File dir = null;
		if (mExternalStorageWriteable) {
			dir = context.getExternalCacheDir();
		} else {
			dir = context.getCacheDir();
		}
		File tempFile = new File(dir, filename);
		return tempFile;
	}
	
	// Download uri and store into a tmp file
	public String downloadUrl(String URI, String filename, Context context)
			throws IllegalStateException, IOException {
		HttpGet httpget = new HttpGet(URI.replace(" ", "%20"));
		AndroidHttpClient httpClient = getHttpClient();
		HttpResponse httpResponse = httpClient.execute(httpget, httpContext);
		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			debug("Http error: " + httpResponse.getStatusLine().getStatusCode());
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpResponse.getEntity().getContent()));
			debug(reader.readLine());
		} else {
			try {
				File f = getTempFileForName(filename, context);
				String filePath = f.getPath();
				FileOutputStream fout = new FileOutputStream(f);
				httpResponse.getEntity().writeTo(fout);
				httpClient.close();
				fout.flush();
				fout.close();
				return filePath;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				httpClient.close();
			}
		}
		return "";
	}

	public void saveFileAs(String path, Uri contentURI, Context context)
			throws IOException {
		String url = uploadFileToTemp(contentURI, context);
		HttpPost httppost = new HttpPost(URI.create(FPBASEURL + "api/path"
				+ pathUrlEncode(path) + "?js_session="
				+ URLEncoder.encode(getJSSessionWithMimetypes("*/*"), "utf-8")));
		StringEntity entity = new StringEntity("url=" + Uri.encode(url));
		httppost.setEntity(entity);
		httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		String response = getStringFromNetworkRequest(httppost);
	}

	private byte[] readBinaryInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}

	public String uploadFileToTemp(Uri contentURI, Context context)
			throws IOException {
		String postUrl = FPBASEURL + "api/path/computer/" + "?js_session="
				+ URLEncoder.encode(getJSSession(), "utf-8");
		HttpPost httppost = new HttpPost(URI.create(postUrl));
		ByteArrayEntity entity = new ByteArrayEntity(
				readBinaryInputStream(context.getContentResolver()
						.openInputStream(contentURI)));
		httppost.setEntity(entity);
		entity.setChunked(false);
		httppost.setHeader("X-File-Name", "testfile.file");
		httppost.setHeader("Content-Type", "application/octet-stream");
		String response = getStringFromNetworkRequest(httppost);
		try {
			JSONObject json = new JSONObject(response);
			JSONObject data = json.getJSONObject("data");
			String url = data.getString("url");
			return url;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException();
		}
	}

	public FPFile getLocalFileForPath(String path, Context context)
			throws AuthError {
		try {
			String query = getJSSession();
			HttpGet httpget = new HttpGet(FPBASEURL + "api/path"
					+ pathUrlEncode(path) + "?format=fpurl&js_session="
					+ URLEncoder.encode(query, "utf-8"));
			String response = getStringFromNetworkRequest(httpget);
			// return parseFolder(builder.toString(), path);
			JSONObject json;
			try {
				json = new JSONObject(response);
				String url = json.getString("url");
				String filename = json.getString("filename");
				return new FPFile(downloadUrl(url, filename, context), url);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// FUCKING ANDROID
	private void workAroundReverseDnsBugInHoneycombAndEarlier(HttpClient client) {
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		if (SDK_INT >= 14) // working on ICS and greater
			return;
		// Android had a bug where HTTPS made reverse DNS lookups (fixed in Ice
		// Cream Sandwich)
		// http://code.google.com/p/android/issues/detail?id=13117
		SocketFactory socketFactory = new LayeredSocketFactory() {
			SSLSocketFactory delegate = SSLSocketFactory.getSocketFactory();

			// @Override
			public Socket createSocket() throws IOException {
				System.out.println("CREATE SOCKET");
				return delegate.createSocket();
			}

			// @Override
			public Socket connectSocket(Socket sock, String host, int port,
					InetAddress localAddress, int localPort, HttpParams params)
					throws IOException {
				return delegate.connectSocket(sock, host, port, localAddress,
						localPort, params);
			}

			// @Override
			public boolean isSecure(Socket sock)
					throws IllegalArgumentException {
				return delegate.isSecure(sock);
			}

			// @Override
			public Socket createSocket(Socket socket, String host, int port,
					boolean autoClose) throws IOException {
				injectHostname(socket, host);
				return delegate.createSocket(socket, host, port, autoClose);
			}

			private void injectHostname(Socket socket, String host) {
				try {
					Field field = InetAddress.class
							.getDeclaredField("hostName");
					field.setAccessible(true);
					field.set(socket.getInetAddress(), host);
				} catch (Exception ignored) {
				}
			}
		};
		client.getConnectionManager().getSchemeRegistry()
				.register(new Scheme("https", socketFactory, 443));
	}

	private AndroidHttpClient getHttpClient() {
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
		workAroundReverseDnsBugInHoneycombAndEarlier(httpClient);
		// ConnManagerParams.setMaxTotalConnections(params, 20);
		return httpClient;
	}


	private String getStringFromNetworkRequest(HttpUriRequest request)
			throws IOException {
		AndroidHttpClient.modifyRequestToAcceptGzipResponse(request);
		AndroidHttpClient httpClient = getHttpClient();
		try {
			workAroundReverseDnsBugInHoneycombAndEarlier(httpClient);
			HttpResponse httpResponse = httpClient
					.execute(request, httpContext);
			if (httpResponse.getStatusLine().getStatusCode() != 200) {
				debug("Http error: "
						+ httpResponse.getStatusLine().getStatusCode());
				// TODO: read error
				throw new IOException();
			} else {
				// success
				HttpEntity responseEntity = httpResponse.getEntity();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								AndroidHttpClient
										.getUngzippedContent(responseEntity)));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				if (debug)
					debug(builder.toString());
				httpClient.close();
				return builder.toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
			httpClient.close();
			throw new IOException();
		}

	}

}
