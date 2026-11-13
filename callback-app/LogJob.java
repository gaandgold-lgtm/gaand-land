package kr.gaand.callback;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.provider.CallLog;

public class LogJob extends JobService {
    static void schedule(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        JobInfo ji = new JobInfo.Builder(7, new ComponentName(c, LogJob.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(CallLog.Calls.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .setTriggerContentUpdateDelay(1000)
                .setTriggerContentMaxDelay(5000)
                .build();
        js.schedule(ji);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(() -> {
            Core.check(getApplicationContext());
            schedule(getApplicationContext());
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) { return false; }
}
