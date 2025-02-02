package io.github.wulkanowy.data.repositories.exam

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.utils.endExamsDay
import io.github.wulkanowy.utils.networkBoundResource
import io.github.wulkanowy.utils.startExamsDay
import io.github.wulkanowy.utils.uniqueSubtract
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val local: ExamLocal,
    private val remote: ExamRemote
) {

    fun getExams(student: Student, semester: Semester, start: LocalDate, end: LocalDate, forceRefresh: Boolean) = networkBoundResource(
        shouldFetch = { it.isEmpty() || forceRefresh },
        query = { local.getExams(semester, start.startExamsDay, start.endExamsDay) },
        fetch = { remote.getExams(student, semester, start.startExamsDay, start.endExamsDay) },
        saveFetchResult = { old, new ->
            local.deleteExams(old uniqueSubtract new)
            local.saveExams(new uniqueSubtract old)
        },
        filterResult = { it.filter { item -> item.date in start..end } }
    )
}
