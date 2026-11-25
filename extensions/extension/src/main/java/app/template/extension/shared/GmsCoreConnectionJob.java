package app.template.extension.shared;

import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Asks GmsCore to connect for push notifications again, if it is not connected, from time to time.
 * GmsCore does not reconnect on its own once Android has stopped its push service, so without
 * this job the notifications of the app would only come once some patched app is opened.
 */
public final class GmsCoreConnectionJob extends JobService {

    @Override
    public boolean onStartJob(JobParameters parameters) {
        GmsCoreSupport.connectGmsCore(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return false;
    }
}
