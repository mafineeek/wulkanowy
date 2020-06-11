package io.github.wulkanowy.ui.modules.grade

import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.grade.GradeRepository
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.ALL_YEAR
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.BOTH_SEMESTERS
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.ONE_SEMESTER
import io.github.wulkanowy.utils.calcAverage
import io.github.wulkanowy.utils.changeModifier
import io.reactivex.Single
import javax.inject.Inject

class GradeAverageProvider @Inject constructor(
    private val semesterRepository: SemesterRepository,
    private val gradeRepository: GradeRepository,
    private val preferencesRepository: PreferencesRepository
) {

    private val plusModifier get() = preferencesRepository.gradePlusModifier

    private val minusModifier get() = preferencesRepository.gradeMinusModifier

    fun getGradesDetailsWithAverage(student: Student, semesterId: Int, forceRefresh: Boolean = false): Single<List<GradeDetailsWithAverage>> {
        return semesterRepository.getSemesters(student).flatMap { semesters ->
            when (preferencesRepository.gradeAverageMode) {
                ONE_SEMESTER -> getSemesterDetailsWithAverage(student, semesters.single { it.semesterId == semesterId }, forceRefresh)
                BOTH_SEMESTERS -> calculateBothSemestersAverage(student, semesters, semesterId, forceRefresh)
                ALL_YEAR -> calculateAllYearAverage(student, semesters, semesterId, forceRefresh)
            }
        }
    }

    private fun calculateBothSemestersAverage(student: Student, semesters: List<Semester>, semesterId: Int, forceRefresh: Boolean): Single<List<GradeDetailsWithAverage>> {
        val selectedSemester = semesters.single { it.semesterId == semesterId }
        val firstSemester = semesters.single { it.diaryId == selectedSemester.diaryId && it.semesterName == 1 }

        return getSemesterDetailsWithAverage(student, selectedSemester, forceRefresh).flatMap { selectedDetails ->
            val isAnyAverage = selectedDetails.any { it.average != .0 }

            if (selectedSemester != firstSemester) {
                getSemesterDetailsWithAverage(student, firstSemester, forceRefresh).map { secondDetails ->
                    selectedDetails.map { selected ->
                        val second = secondDetails.singleOrNull { it.subject == selected.subject }
                        selected.copy(average = if (!isAnyAverage || preferencesRepository.gradeAverageForceCalc) {
                            val selectedGrades = selected.grades.updateModifiers(student).calcAverage()
                            (selectedGrades + (second?.grades?.updateModifiers(student)?.calcAverage() ?: selectedGrades)) / 2
                        } else (selected.average + (second?.average ?: selected.average)) / 2)
                    }
                }
            } else Single.just(selectedDetails)
        }
    }

    private fun calculateAllYearAverage(student: Student, semesters: List<Semester>, semesterId: Int, forceRefresh: Boolean): Single<List<GradeDetailsWithAverage>> {
        val selectedSemester = semesters.single { it.semesterId == semesterId }
        val firstSemester = semesters.single { it.diaryId == selectedSemester.diaryId && it.semesterName == 1 }

        return getSemesterDetailsWithAverage(student, selectedSemester, forceRefresh).flatMap { selectedDetails ->
            val isAnyAverage = selectedDetails.any { it.average != .0 }

            if (selectedSemester != firstSemester) {
                getSemesterDetailsWithAverage(student, firstSemester, forceRefresh).map { secondDetails ->
                    selectedDetails.map { selected ->
                        val second = secondDetails.singleOrNull { it.subject == selected.subject }
                        selected.copy(average = if (!isAnyAverage || preferencesRepository.gradeAverageForceCalc) {
                            (selected.grades.updateModifiers(student) + second?.grades?.updateModifiers(student).orEmpty()).calcAverage()
                        } else selected.average)
                    }
                }
            } else Single.just(selectedDetails)
        }
    }

    private fun getSemesterDetailsWithAverage(student: Student, semester: Semester, forceRefresh: Boolean): Single<List<GradeDetailsWithAverage>> {
        return gradeRepository.getGrades(student, semester, forceRefresh).map { (details, summaries) ->
            val isAnyAverage = summaries.any { it.average != .0 }
            val allGrades = details.groupBy { it.subject }

            summaries.emulateEmptySummaries(student, semester, allGrades.toList(), isAnyAverage).map { summary ->
                val grades = allGrades[summary.subject].orEmpty()
                GradeDetailsWithAverage(
                    subject = summary.subject,
                    average = if (!isAnyAverage || preferencesRepository.gradeAverageForceCalc) {
                        grades.updateModifiers(student).calcAverage()
                    } else summary.average,
                    points = summary.pointsSum,
                    summary = summary,
                    grades = grades
                )
            }
        }
    }

    private fun List<GradeSummary>.emulateEmptySummaries(student: Student, semester: Semester, grades: List<Pair<String, List<Grade>>>, calcAverage: Boolean): List<GradeSummary> {
        if (isNotEmpty() && size > grades.size) return this

        return grades.mapIndexed { i, (subject, details) ->
            singleOrNull { it.subject == subject }?.let { return@mapIndexed it }
            GradeSummary(
                studentId = student.studentId,
                semesterId = semester.semesterId,
                position = i,
                subject = subject,
                predictedGrade = "",
                finalGrade = "",
                proposedPoints = "",
                finalPoints = "",
                pointsSum = "",
                average = if (calcAverage) details.updateModifiers(student).calcAverage() else .0
            )
        }
    }

    private fun List<Grade>.updateModifiers(student: Student): List<Grade> {
        return if (student.loginMode == Sdk.Mode.SCRAPPER.name) {
            map { it.changeModifier(plusModifier, minusModifier) }
        } else this
    }
}
