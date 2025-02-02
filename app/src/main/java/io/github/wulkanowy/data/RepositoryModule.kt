package io.github.wulkanowy.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wulkanowy.data.db.AppDatabase
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.sdk.Sdk
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
internal class RepositoryModule {

    @Singleton
    @Provides
    fun provideSdk(chuckerCollector: ChuckerCollector, @ApplicationContext context: Context): Sdk {
        return Sdk().apply {
            androidVersion = android.os.Build.VERSION.RELEASE
            buildTag = android.os.Build.MODEL
            setSimpleHttpLogger { Timber.d(it) }

            // for debug only
            addInterceptor(ChuckerInterceptor(context, chuckerCollector), true)
        }
    }

    @Singleton
    @Provides
    fun provideChuckerCollector(@ApplicationContext context: Context, prefRepository: PreferencesRepository): ChuckerCollector {
        return ChuckerCollector(
            context = context,
            showNotification = prefRepository.isDebugNotificationEnable,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context, sharedPrefProvider: SharedPrefProvider) = AppDatabase.newInstance(context, sharedPrefProvider)

    @Singleton
    @Provides
    fun provideResources(@ApplicationContext context: Context): Resources = context.resources

    @Singleton
    @Provides
    fun provideAssets(@ApplicationContext context: Context): AssetManager = context.assets

    @Singleton
    @Provides
    fun provideSharedPref(@ApplicationContext context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideStudentDao(database: AppDatabase) = database.studentDao

    @Singleton
    @Provides
    fun provideSemesterDao(database: AppDatabase) = database.semesterDao

    @Singleton
    @Provides
    fun provideGradeDao(database: AppDatabase) = database.gradeDao

    @Singleton
    @Provides
    fun provideGradeSummaryDao(database: AppDatabase) = database.gradeSummaryDao

    @Singleton
    @Provides
    fun provideGradeStatisticsDao(database: AppDatabase) = database.gradeStatistics

    @Singleton
    @Provides
    fun provideGradePointsStatisticsDao(database: AppDatabase) = database.gradePointsStatistics

    @Singleton
    @Provides
    fun provideMessagesDao(database: AppDatabase) = database.messagesDao

    @Singleton
    @Provides
    fun provideMessageAttachmentsDao(database: AppDatabase) = database.messageAttachmentDao

    @Singleton
    @Provides
    fun provideExamDao(database: AppDatabase) = database.examsDao

    @Singleton
    @Provides
    fun provideAttendanceDao(database: AppDatabase) = database.attendanceDao

    @Singleton
    @Provides
    fun provideAttendanceSummaryDao(database: AppDatabase) = database.attendanceSummaryDao

    @Singleton
    @Provides
    fun provideTimetableDao(database: AppDatabase) = database.timetableDao

    @Singleton
    @Provides
    fun provideNoteDao(database: AppDatabase) = database.noteDao

    @Singleton
    @Provides
    fun provideHomeworkDao(database: AppDatabase) = database.homeworkDao

    @Singleton
    @Provides
    fun provideSubjectDao(database: AppDatabase) = database.subjectDao

    @Singleton
    @Provides
    fun provideLuckyNumberDao(database: AppDatabase) = database.luckyNumberDao

    @Singleton
    @Provides
    fun provideCompletedLessonsDao(database: AppDatabase) = database.completedLessonsDao

    @Singleton
    @Provides
    fun provideReportingUnitDao(database: AppDatabase) = database.reportingUnitDao

    @Singleton
    @Provides
    fun provideRecipientDao(database: AppDatabase) = database.recipientDao

    @Singleton
    @Provides
    fun provideMobileDevicesDao(database: AppDatabase) = database.mobileDeviceDao

    @Singleton
    @Provides
    fun provideTeacherDao(database: AppDatabase) = database.teacherDao

    @Singleton
    @Provides
    fun provideSchoolInfoDao(database: AppDatabase) = database.schoolDao
}
