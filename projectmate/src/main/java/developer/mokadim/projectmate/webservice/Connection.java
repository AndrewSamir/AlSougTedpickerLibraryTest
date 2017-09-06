package developer.mokadim.projectmate.webservice;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.InputStreamEntity;
import developer.mokadim.projectmate.dialog.IndicatorStyle;
import developer.mokadim.projectmate.dialog.ProgressDialog;
import developer.mokadim.projectmate.network.NetworkStatus;
import developer.mokadim.projectmate.network.NetworkUtil;

/**
 * Created by ahmed.elmokadem on 2015-09-22.
 * Handle connection to web service and return the response.
 */
public class Connection {

    private static Connection instance = null;
    private AsyncHttpClient client;
    private String contentType;
    private Dialog progressDialog;
    private Gson gson;

    /**
     * A private Constructor prevents any other class from instantiating.
     */
    private Connection() {

//==================================================//


        //==============================//

        // client = new AsyncHttpClient(true, 0, 443);
        client = new AsyncHttpClient();
        client.setSSLSocketFactory(MySSLSocketFactory.getFixedSocketFactory());
        contentType = "application/json";
       client.setUserAgent(System.getProperty("http.agent"));
        //Mozilla/5.0 (Windows NT 10.0; WOW64; rv:47.0) Gecko/20100101 Firefox/47.0"
       // client.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:47.0) Gecko/20100101 Firefox/47.0");
        client.setTimeout(20 * 1000);
        client.setResponseTimeout(20 * 1000);
        client.setConnectTimeout(20 * 1000);
        //  client.setEnableRedirects(true);

        //  Log.e("agentss",System.getProperty("https.agent"));
        gson = new GsonBuilder().serializeNulls().create();
        //client.getHttpClient().getConnectionManager().shutdown();
/*
  try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory  sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.setSSLSocketFactory(sf);
        }
        catch (Exception e) {
        }
 */

    }


    /**
     * Make sure that there is only one Connection instance.
     *
     * @return Returns only one instance of Connection.
     */
    public static Connection getInstance() {

        if (instance == null) {
            instance = new Connection();
        }
        return instance;
    }

    /**
     * Perform a HTTP GET request.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public RequestHandle get(final int requestID, final String[] arr, final HashMap<String, String> headerMap, final Context context, final String url,
                             final ResponseHandler responseHandler,
                             boolean showLoadingIndicator, IndicatorStyle style) {

        RequestHandle rQ = null;
        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }

                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:

                if (headerMap != null) {


                    Iterator myVeryOwnIterator = headerMap.keySet().iterator();
                    while (myVeryOwnIterator.hasNext()) {
                        String key = (String) myVeryOwnIterator.next();
                        String value = (String) headerMap.get(key);
                        client.addHeader(key, value);
                        // Toast.makeText(ctx, "Key: "+key+" Value: "+value, Toast.LENGTH_LONG).show();
                    }


                }

                rQ = client.get(url, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);

                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                      //  Header[] header=headers;

                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            //  Log.e("statuescode", statusCode + "");
                            responseHandler.onRequestSucess(requestID, arr,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                            // Log.e("statuescode",statusCode+"");

                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        error.printStackTrace();
                        Header[] header=headers;
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }
                });
                // rQ.cancel(true);
                break;
        }
        return rQ;
    }


    public void postParams(final int requestID, final String[] arr, final HashMap<String, String> headerMap, Context context, final RequestParams params,
                           final String url, final ResponseHandler responseHandler) {
        if (NetworkUtil.getConnectivityStatus(context) == NetworkStatus.OFFLINE) {
            responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
        }
        if (NetworkUtil.getConnectivityStatus(context) == NetworkStatus.WIFI_CONNECTED_WITHOUT_INTERNET) {
            responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
        } else {
            client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
            //  client.addHeader("", params.);

            if (headerMap != null) {


                Iterator myVeryOwnIterator = headerMap.keySet().iterator();
                while (myVeryOwnIterator.hasNext()) {
                    String key = (String) myVeryOwnIterator.next();
                    String value = (String) headerMap.get(key);
                    client.addHeader(key, value);
                    // Toast.makeText(ctx, "Key: "+key+" Value: "+value, Toast.LENGTH_LONG).show();
                }


            }

            client.post(context, url, params, new AsyncHttpResponseHandler() {

                @Override
                public void onStart() {
                    Log.v("URL", url);
                    Log.v("Request", params.toString() + "");
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    if (responseBody != null) {
                        String response = new String(responseBody);
                        Log.v("Responsesucess", response + "");
                        responseHandler.onRequestSucess(requestID, arr, RequestStatus.SUCCEED, statusCode, response);
                    } else {
                        responseHandler.onRequestFinished(requestID, RequestStatus.SUCCEED, statusCode, null);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    if (responseBody != null) {
                        String response = new String(responseBody);
                        Log.v("Responsefailed", response + "");
                        responseHandler.onRequestFinished(requestID, RequestStatus.FAILED, statusCode, response);
                    } else {
                        responseHandler.onRequestFinished(requestID, RequestStatus.FAILED, statusCode, null);
                    }
                }
            });
        }
    }

    /**
     * Perform a HTTP POST request.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param json                 Json which will send with the request.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void postJson(final int requestID, final Context context, final String url,
                         final String json, final ResponseHandler responseHandler,
                         boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                InputStream is = null;
                InputStreamEntity entity = null;
                try {
                    is = new ByteArrayInputStream(json.getBytes("UTF-8"));
                    entity = new InputStreamEntity(is, is.available());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                client.post(context, url, entity, contentType, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Log.v("Request", json + "");
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }
                });
                break;
        }
    }

    /**
     * Perform a HTTP POST request with parameters.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param params               Additional POST parameters or files to send with the request.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param progressListener     The progress listener instance that should listen to download progress.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void postParams(final int requestID, Context context, final String url,
                           final RequestParams params, final ResponseHandler responseHandler,
                           final OnProgressListener progressListener,
                           boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                // client.addHeader("ApiKey", "9c4a06e4dddceb70722de4f3fda4f2c7");
                // client.addHeader("Authorization", "e291471bd570246fc12962e3b0b22821");

                client.post(context, url, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Log.v("Request", params.toString() + "");
                    }

                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {
                        super.onProgress(bytesWritten, totalSize);
                        progressListener.onProgress(bytesWritten, totalSize);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }
                });
                break;
        }
    }


    public void postParams(final int requestID, final String[] arr, final HashMap<String, String> headerMap, Context context, final String url,
                           final RequestParams params, final ResponseHandler responseHandler,
                           final OnProgressListener progressListener,
                           boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                // client.addHeader("ApiKey", "9c4a06e4dddceb70722de4f3fda4f2c7");
                // client.addHeader("Authorization", "9575af2f2e60e1a560a90adb7affb586");

                if (headerMap != null) {


                    Iterator myVeryOwnIterator = headerMap.keySet().iterator();
                    while (myVeryOwnIterator.hasNext()) {
                        String key = (String) myVeryOwnIterator.next();
                        String value = (String) headerMap.get(key);
                        client.addHeader(key, value);
                        // Toast.makeText(ctx, "Key: "+key+" Value: "+value, Toast.LENGTH_LONG).show();

                    }
                }


                client.post(context, url, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Log.v("Request", params.toString() + "");
                    }

                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {
                        super.onProgress(bytesWritten, totalSize);
                        progressListener.onProgress(bytesWritten, totalSize);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestSucess(requestID, arr,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }

                        try {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {

                        }
                    }
                });
                break;
        }

    }


    /**
     * Perform a HTTP PUT request.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void put(final int requestID, Context context, final String url,
                    final ResponseHandler responseHandler,
                    boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);

                client.put(url, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }
                    }
                });

                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
        }
    }

    /**
     * Perform a HTTP PUT request with parameters.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param params               Additional POST parameters or files to send with the request.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void putParams(final int requestID, Context context, final String url,
                          final RequestParams params, final ResponseHandler responseHandler,
                          boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                client.put(context, url, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Log.v("Request", params.toString() + "");
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }
                    }
                });

                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
        }
    }

    /**
     * Perform a HTTP DELETE request.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void delete(final int requestID, Context context, final String url,
                       final ResponseHandler responseHandler,
                       boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                client.delete(url, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }
                    }
                });

                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
        }
    }

    /**
     * Perform a HTTP DELETE request with parameters.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The URL to send the request to.
     * @param params               Additional POST parameters or files to send with the request.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public void deleteParams(final int requestID, Context context, final String url,
                             final RequestParams params, final ResponseHandler responseHandler,
                             boolean showLoadingIndicator, IndicatorStyle style) {

        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                client.delete(url, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Log.v("Request", params.toString() + "");
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        if (responseBody != null) {
                            String response = new String(responseBody);
                            Log.v("Response", response + "");
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, response);
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }
                    }
                });

                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
        }
    }

    /**
     * Download any file.
     *
     * @param requestID            A unique id to simply identify request.
     * @param context              The android Context instance associated to the request.
     * @param url                  The file URL to download it.
     * @param responseHandler      The response handler instance that should handle the response.
     * @param progressListener     The progress listener instance that should listen to download progress.
     * @param showLoadingIndicator True if you want to show loading indicator otherwise false.
     * @param style                You can choose from 28 different types of indicators.
     */
    public RequestHandle downloadFile(final int requestID, final Context context, final String url,
                                      final ResponseHandler responseHandler,
                                      final OnProgressListener progressListener,
                                      boolean showLoadingIndicator, IndicatorStyle style, final String videoTtile) {
        RequestHandle rq = null;
        if (showLoadingIndicator) {
            progressDialog = new ProgressDialog(context, style).show();
        }

        switch (NetworkUtil.getConnectivityStatus(context)) {
            case OFFLINE:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_CONNECTION, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case WIFI_CONNECTED_WITHOUT_INTERNET:
                responseHandler.onRequestFinished(requestID, RequestStatus.NO_INTERNET, 0, null);
                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
            case MOBILE_DATA_CONNECTED:
            case WIFI_CONNECTED_WITH_INTERNET:
                client.setTimeout(AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT);
                rq = client.get(url, new FileAsyncHttpResponseHandler(context) {

                    @Override
                    public void onStart() {
                        Log.v("URL", url);
                        Toast.makeText(context, "started download " + videoTtile, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {
                        super.onProgress(bytesWritten, totalSize);
                        progressListener.onProgress(bytesWritten, totalSize);
                        if (bytesWritten == totalSize) {
                            Toast.makeText(context, "download " + videoTtile + " finished", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, File file) {
                        if (file != null) {
                            Log.v("Response", "File name: " + file.getName() +
                                    ", File path: " + file.getAbsolutePath());
                            copyFileFromUri(context, Uri.parse("file://" + file.toString()), videoTtile);
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, file.getAbsolutePath());
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.SUCCEED, statusCode, null);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                        if (file != null) {
                            Log.v("Response", "File name: " + file.getName() +
                                    ", File path: " + file.getAbsolutePath());
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, file.getAbsolutePath());
                        } else {
                            responseHandler.onRequestFinished(requestID,
                                    RequestStatus.FAILED, statusCode, null);
                        }
                    }
                });

                try {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }
                break;
        }
        return rq;
    }

    public boolean copyFileFromUri(Context context, Uri fileUri, String VideoTitle) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            ContentResolver content = context.getContentResolver();
            inputStream = content.openInputStream(fileUri);

            File root = Environment.getExternalStorageDirectory();
            if (root == null) {
                Log.d("hhh", "Failed to get root");
            }

            // create a directory
            File saveDirectory = new File(Environment.getExternalStorageDirectory() + File.separator + "AIOVideoDownlader" + File.separator);
            // create direcotory if it doesn't exists
            saveDirectory.mkdirs();

            outputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + "AIOVideoDownlader" + File.separator + VideoTitle + ".MP4"); // filename.png, .mp3, .mp4 ...
            if (outputStream != null) {
                //Log.e("jjj", "Output Stream Opened successfully");
            }

            byte[] buffer = new byte[1000];
            int bytesRead = 0;
            int i = 0;
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                outputStream.write(buffer, 0, buffer.length);
                //  Log.e("i iss",++i+"/");
            }
        } catch (Exception e) {
            //Log.e("ujjj", "Exception occurred " + e.getMessage());
        } finally {

        }
        return true;
    }

    /**
     * Cancels all pending (or potentially active) requests. Used in the onDestroy method
     * of your android activity to destroy all requests which are no longer required.
     *
     * @param mayInterruptIfRunning specifies if active requests should be cancelled
     *                              along with pending requests.
     */
    public void cancelAllRequests(boolean mayInterruptIfRunning, RequestHandle cnt) {
        // client.get

    }

    /**
     * Deserialize the specified Json into an object of the specified class.
     *
     * @param response   The string from which the object is to be deserialized.
     * @param modelClass The model class.
     * @return An object from the json and returns null if json is null.
     */
    public Object parseJsonToObject(String response, Class<?> modelClass) {

        try {
            return gson.fromJson(response, modelClass);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Serializes the specified object into its equivalent Json representation.
     *
     * @param object The object which Json will represent.
     * @return Json representation of src.
     */
    public String parseObjectToJson(Object object) {

        return gson.toJson(object);
    }
}