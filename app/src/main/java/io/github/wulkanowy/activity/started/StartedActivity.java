package io.github.wulkanowy.activity.started;

import android.app.Activity;
import android.os.Bundle;

import io.github.wulkanowy.R;

public class StartedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_started);
        Task();
    }

    public void Task(){

        new LoadingTask(this).execute();
    }
}
