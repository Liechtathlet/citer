/**
 * 
 */
package ch.zhaw.mdp.lhb.citr.com.rest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.util.Log;
import ch.zhaw.mdp.lhb.citr.activities.CitrBaseActivity;
import ch.zhaw.mdp.lhb.citr.exceptions.CitrCommunicationException;
import ch.zhaw.mdp.lhb.citr.exceptions.CitrExceptionTypeEnum;
import ch.zhaw.mdp.lhb.citr.response.ResponseObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Daniel Brun
 * 
 *         Abstract base class for all Client-Side implementations of the REST-Services.
 */
public abstract class AbstractClientRBaseServiceImpl {

    private static final String TAG = "AbstractClientRBaseServiceImpl";

    protected CitrBaseActivity activity;
    protected RESTBackgroundTask restTask;
    protected ObjectMapper mapper;

    /**
     * Creates a new instance of this class.
     * 
     * @param aActivity The underlining activity.
     */
    public AbstractClientRBaseServiceImpl(CitrBaseActivity aActivity) {
	super();
	activity = aActivity;

	mapper = new ObjectMapper();
    }

    /**
     * Performs the pre initialization.
     * 
     * @param aHttpType The http type.
     */
    protected void preInit(int aHttpType) {
	restTask = new RESTBackgroundTask(activity);
	restTask.setHttpRequestType(aHttpType);
    }

    /**
     * Executes the prepared call with the given url.
     * 
     * @param aUrl The url to call.
     * @param aReference The result class reference (Generic class of the ResponseObject).
     * @return The result object.
     */
    protected <T> ResponseObject<T> execute(String aUrl, TypeReference<ResponseObject<T>> aReference) {
	if (aUrl == null) {
	    throw new IllegalArgumentException(
		    "The argument aUrl must not be null!");
	}

	ResponseObject<T> resObj = null;
	try {
	    AsyncTask<String, Integer, String> task = restTask.execute(aUrl);

	    if (task != null) {
		resObj = map(task.get(),aReference);
	    }
	} catch (InterruptedException e) {
	    Log.e(TAG, "Exception during task processing", e);
	    throw new CitrCommunicationException(
		    "Exception during task processing", e,
		    CitrExceptionTypeEnum.BACKGROUND_ERROR);
	} catch (ExecutionException e) {
	    Log.e(TAG, "Exception during task processing", e);
	    throw new CitrCommunicationException(
		    "Exception during task processing", e,
		    CitrExceptionTypeEnum.BACKGROUND_ERROR);
	}

	return resObj;
    }

    /**
     * Maps the given string to an object.
     * 
     * @param <T> The expected return type.
     * @param aResult The string to map.
     * @param aReference The result class reference (Generic class of the ResponseObject).
     * @return the mapped object.
     */
    private <T> ResponseObject<T> map(String aResult, TypeReference<ResponseObject<T>> aReference) {
	ResponseObject<T> resp = null;

	if (aResult != null && aResult.trim().length() > 0) {
	    try {
		resp = mapper.readValue(aResult,aReference);
	    } catch (JsonParseException e) {
		Log.e(TAG, "Exception during parse process of JSON-Data", e);
		throw new CitrCommunicationException(
			"Exception during parse process of JSON-Data", e,
			CitrExceptionTypeEnum.DESERIALIZATION_ERROR);
	    } catch (JsonMappingException e) {
		Log.e(TAG, "Exception during map process of JSON-Data", e);
		throw new CitrCommunicationException(
			"Exception during map process of JSON-Data", e,
			CitrExceptionTypeEnum.DESERIALIZATION_ERROR);
	    } catch (IOException e) {
		Log.e(TAG, "Exception during io process of JSON-Data", e);
		throw new CitrCommunicationException(
			"Exception during io process of JSON-Data", e,
			CitrExceptionTypeEnum.DESERIALIZATION_ERROR);
	    }
	}

	return resp;
    }

}