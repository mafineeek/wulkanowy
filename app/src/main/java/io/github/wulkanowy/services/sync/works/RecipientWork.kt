package io.github.wulkanowy.services.sync.works

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.recipient.RecipientRepository
import io.github.wulkanowy.data.repositories.reportingunit.ReportingUnitRepository
import javax.inject.Inject

class RecipientWork @Inject constructor(
    private val reportingUnitRepository: ReportingUnitRepository,
    private val recipientRepository: RecipientRepository
) : Work {

    override suspend fun doWork(student: Student, semester: Semester) {
        reportingUnitRepository.refreshReportingUnits(student)

        reportingUnitRepository.getReportingUnits(student).let { units ->
            units.map {
                recipientRepository.refreshRecipients(student, 2, it)
            }
        }
    }
}
