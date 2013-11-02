/**
 * 
 */
package ch.zhaw.mdp.lhb.citr.com.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import ch.zhaw.mdp.lhb.citr.R;
import ch.zhaw.mdp.lhb.citr.activities.CitrBaseActivity;
import ch.zhaw.mdp.lhb.citr.exceptions.CitrCommunicationException;

/**
 * @author Daniel Brun
 * 
 *         Background-Task to do the REST-Requests
 */
public class RESTBackgroundTask extends AsyncTask<String, Integer, String> {

	public static final String TAG = "RESTBackgroundTask";

	/**
	 * HTTP POST
	 */
	public static final int HTTP_POST_TASK = 1;

	/**
	 * HTTP GET
	 */
	public static final int HTTP_GET_TASK = 2;

	/**
	 * Connection timeout in ms
	 */
	private static final int CONN_TIMEOUT = 3000;

	/**
	 * Socket timeout in ms
	 */
	private static final int SOCKET_TIMEOUT = 5000;

	private CitrBaseActivity activity;

	private ProgressDialog uiProgressDialog = null;

	private ArrayList<NameValuePair> parameters;
	private int httpRequestType;

	/**
	 * Creates a new instance of this class.
	 * 
	 * @param aContext
	 *            The underlying context.
	 * @param anActivity
	 *            The underlying activity
	 * @param aHttpRequestType
	 *            The HTTP-Type
	 */
	public RESTBackgroundTask(CitrBaseActivity anActivity, int aHttpRequestType) {
		if(anActivity == null){
			throw new NullPointerException("The argument anActivity must not be null!");
		}
		
		activity = anActivity;
		httpRequestType = aHttpRequestType;
	}

	/**
	 * Adds a new parameter to the request.
	 * 
	 * @param aName
	 *            The name of the parameter.
	 * @param aValue
	 *            The value of the parameter.
	 */
	public void addParameter(String aName, String aValue) {
		parameters.add(new BasicNameValuePair(aName, aValue));
	}

	/**
	 * Displays a 'Working-Dialog'
	 */
	private void showWorkingDialog() {
		uiProgressDialog = new ProgressDialog(activity);
		uiProgressDialog
				.setMessage(activity.getString(R.string.conn_data_load));
		uiProgressDialog.setProgressDrawable(activity.getWallpaper());
		uiProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		uiProgressDialog.setCancelable(false);
		uiProgressDialog.show();
	}

	/*
	 * ConnectivityManager connMgr = (ConnectivityManager)
	 * getSystemService(Context.CONNECTIVITY_SERVICE); NetworkInfo networkInfo =
	 * connMgr.getActiveNetworkInfo(); if (networkInfo != null &&
	 * networkInfo.isConnected()) { // fetch data } else { // display error }
	 */
	/*
	 * /* (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {
		// Hide Keyboard
		activity.cleanScreen();
		showWorkingDialog();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected String doInBackground(String... someArguments) {
		if (someArguments == null || someArguments.length != 1) {
			throw new IllegalArgumentException(
					"Exactly one argument must be set!");
		}

		String requestUrl = someArguments[0];
		String result = "";

		HttpResponse reqRes = performRequest(requestUrl);

		if (reqRes != null) {
			try {
				result = readInputStream(reqRes.getEntity().getContent());
			} catch (IllegalStateException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
				throw new CitrCommunicationException("An error occurred during HTTP-Result-Processing!",e);
			} catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
				throw new CitrCommunicationException("An error occurred during HTTP-Result-Processing!",e);
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onCancelled()
	 */
	@Override
	protected void onCancelled() {

		uiProgressDialog.dismiss();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(String aResult) {
		activity.handleResponse(aResult);
		uiProgressDialog.dismiss();
	}

	/**
	 * Performs the HTTP-Request
	 * 
	 * @param aRequestUrl
	 *            the Request URL.
	 * @return the HTTPResponse.
	 */
	private HttpResponse performRequest(String aRequestUrl) {
		HttpResponse response = null;

		// Create HTTP-Parameters
		HttpParams httpParameter = new BasicHttpParams();

		HttpConnectionParams.setConnectionTimeout(httpParameter, CONN_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameter, SOCKET_TIMEOUT);

		// Create HTTP-Client
		HttpClient httpClient = new DefaultHttpClient(httpParameter);

		// TODO: Get frome somewhere else...
		// http://stackoverflow.com/questions/1925486/android-storing-username-and-password
		String authString = new String(Base64.encode(
				("user1" + ":" + new String(Hex.encodeHex(DigestUtils
						.sha512("strongpassword1")))).getBytes(),
				Base64.NO_WRAP));

		HttpRequestBase httpRequest = null;

		try {
			switch (httpRequestType) {

			// Create POST-Request
			case HTTP_POST_TASK:
				httpRequest = new HttpPost(aRequestUrl);

				// Add parameters
				((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(
						parameters));
				break;

			// Create GET-Request
			case HTTP_GET_TASK:
				httpRequest = new HttpGet(aRequestUrl);
				break;
			default: {
				throw new NotImplementedException();
			}
			}

			httpRequest.addHeader("Authorization", "Basic " + authString);
			response = httpClient.execute(httpRequest);

		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			throw new CitrCommunicationException("Invalid encoding for HTTP-request!",e);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			throw new CitrCommunicationException("Client exception occurred on performing HTTP-request!",e);
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			throw new CitrCommunicationException("IO-exception during HTTP-request!",e);
		}

		return response;
	}

	/**
	 * Reads the given input stream to a string.
	 * 
	 * @param anInputStream
	 *            The input stream to read
	 * @return
	 */
	private String readInputStream(InputStream anInputStream) {
		StringBuilder data = new StringBuilder();

		BufferedReader buffReader = new BufferedReader(new InputStreamReader(
				anInputStream));

		try {
			String line = null;
			while ((line = buffReader.readLine()) != null) {
				data.append(line);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			throw new CitrCommunicationException("IO-exception during HTTP-respone parsing!",e);
		} finally {
			try {
				buffReader.close();
			} catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
				throw new CitrCommunicationException("IO-exception during HTTP-respone parsing! On closing stream.",e);
			}
		}

		return data.toString();
	}
}
