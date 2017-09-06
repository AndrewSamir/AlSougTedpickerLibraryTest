package developer.mokadim.projectmate.webservice;

/**
 * Created by ahmed on 4/11/16.
 */
public interface OnProgressListener {

    void onProgress(long bytesWritten, long totalSize);
}
