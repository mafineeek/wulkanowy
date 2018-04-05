package io.github.wulkanowy;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.greenrobot.greendao.query.QueryBuilder;

import javax.inject.Inject;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.utils.Log;
import io.fabric.sdk.android.Fabric;
import io.github.wulkanowy.data.RepositoryContract;
import io.github.wulkanowy.di.component.ApplicationComponent;
import io.github.wulkanowy.di.component.DaggerApplicationComponent;
import io.github.wulkanowy.di.modules.ApplicationModule;
import io.github.wulkanowy.utils.LogUtils;

public class WulkanowyApp extends Application {

    protected ApplicationComponent applicationComponent;

    @Inject
    RepositoryContract repository;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        applicationComponent.inject(this);

        if (BuildConfig.DEBUG) {
            enableDebugLog();
        }
        initializeFabric();
        initializeUserSession();
    }

    private void initializeUserSession() {
        if (repository.getCurrentUserId() != 0) {
            try {
                repository.initLastUser();
            } catch (Exception e) {
                LogUtils.error("An error occurred when the application was started", e);
            }
        }
    }

    private void enableDebugLog() {
        QueryBuilder.LOG_VALUES = true;
        FlexibleAdapter.enableLogs(Log.Level.DEBUG);
    }

    private void initializeFabric() {
        Fabric.with(new Fabric.Builder(this)
                .kits(new Crashlytics.Builder()
                        .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                        .build())
                .debuggable(BuildConfig.DEBUG)
                .build());
    }

    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }
}
