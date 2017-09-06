package developer.mokadim.projectmate.webservice;

/**
 * Created by ahmed.elmokadem on 2015-09-22.
 * Handel the response of a request.
 */
public interface ResponseHandler {

    /**
     * Handle the response when request finished.
     *
     * @param requestId     A unique id to simply identify request.
     * @param requestStatus The status of request (succeed, failed, no connection, no internet).
     * @param requestCode   Indicates the status of request. For example (200 means request was fulfilled).
     * @param response      The response of web service.
     */
    public void onRequestFinished(int requestId, RequestStatus requestStatus, int requestCode, String response);
    public void onRequestSucess(int requestId,String [] arr, RequestStatus requestStatus, int requestCode, String response);
}
