package io.github.wulkanowy.ui.modules.grade

import io.github.wulkanowy.createSemesterEntity
import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.grade.GradeRepository
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.utils.flowWithResource
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate.now
import java.time.LocalDate.of
import java.time.LocalDateTime

class GradeAverageProviderTest {

    private suspend fun <T> Flow<Resource<T>>.getResult() = toList()[1].data!!

    @MockK
    lateinit var preferencesRepository: PreferencesRepository

    @MockK
    lateinit var semesterRepository: SemesterRepository

    @MockK
    lateinit var gradeRepository: GradeRepository

    private lateinit var gradeAverageProvider: GradeAverageProvider

    private val student = Student("", "", "", "SCRAPPER", "", "", false, "", "", "", 101, 0, "", "", "", "", "", "", 1, true, LocalDateTime.now())

    private val semesters = mutableListOf(
        createSemesterEntity(10, 21, of(2019, 1, 31), of(2019, 6, 23)),
        createSemesterEntity(11, 22, of(2019, 9, 1), of(2020, 1, 31)),
        createSemesterEntity(11, 23, of(2020, 2, 1), now(), semesterName = 2)
    )

    private val firstGrades = listOf(
        // avg: 3.5
        getGrade(22, "Matematyka", 4.0),
        getGrade(22, "Matematyka", 3.0),

        // avg: 3.5
        getGrade(22, "Fizyka", 6.0),
        getGrade(22, "Fizyka", 1.0)
    )

    private val firstSummaries = listOf(
        getSummary(semesterId = 22, subject = "Matematyka", average = 3.9),
        getSummary(semesterId = 22, subject = "Fizyka", average = 3.1)
    )

    private val secondGrades = listOf(
        // avg: 2.5
        getGrade(23, "Matematyka", 2.0),
        getGrade(23, "Matematyka", 3.0),

        // avg: 3.0
        getGrade(23, "Fizyka", 4.0),
        getGrade(23, "Fizyka", 2.0)
    )

    private val secondSummaries = listOf(
        getSummary(semesterId = 23, subject = "Matematyka", average = 2.9),
        getSummary(semesterId = 23, subject = "Fizyka", average = 3.4)
    )

    private val secondGradeWithModifier = listOf(
        // avg: 3.375
        getGrade(24, "Język polski", 3.0, -0.50),
        getGrade(24, "Język polski", 4.0, 0.25)
    )

    private val secondSummariesWithModifier = listOf(
        getSummary(24, "Język polski", 3.49)
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { preferencesRepository.gradeAverageForceCalc } returns false
        coEvery { semesterRepository.getSemesters(student) } returns semesters
        every { preferencesRepository.gradeMinusModifier } returns .33
        every { preferencesRepository.gradePlusModifier } returns .33

        gradeAverageProvider = GradeAverageProvider(semesterRepository, gradeRepository, preferencesRepository)
    }

    @Test
    fun `force calc average on no grades`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { emptyList<Grade>() to emptyList<GradeSummary>() }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { emptyList<Grade>() to emptyList<GradeSummary>() }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(0, items.size)
    }

    @Test
    fun `force calc current semester average with default modifiers in scraper mode`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { semesterRepository.getSemesters(student) } returns semesters
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGradeWithModifier to secondSummariesWithModifier }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(3.5, items.single { it.subject == "Język polski" }.average, .0) // from details and after set custom plus/minus
    }

    @Test
    fun `force calc current semester average with custom modifiers in scraper mode`() {
        val student = student.copy(loginMode = Sdk.Mode.SCRAPPER.name)

        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeMinusModifier } returns .33
        every { preferencesRepository.gradePlusModifier } returns .33

        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { semesterRepository.getSemesters(student) } returns semesters
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGradeWithModifier to secondSummariesWithModifier }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(3.5, items.single { it.subject == "Język polski" }.average, .0) // from details and after set custom plus/minus
    }

    @Test
    fun `force calc current semester average with custom modifiers in api mode`() {
        val student = student.copy(loginMode = Sdk.Mode.API.name)

        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeMinusModifier } returns .33  // useless in this mode
        every { preferencesRepository.gradePlusModifier } returns .33

        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { semesterRepository.getSemesters(student) } returns semesters
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGradeWithModifier to secondSummariesWithModifier }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(3.375, items.single { it.subject == "Język polski" }.average, .0) // (from details): 3.375
    }

    @Test
    fun `force calc current semester average with custom modifiers in hybrid mode`() {
        val student = student.copy(loginMode = Sdk.Mode.HYBRID.name)

        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeMinusModifier } returns .33 // useless in this mode
        every { preferencesRepository.gradePlusModifier } returns .33

        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { semesterRepository.getSemesters(student) } returns semesters
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGradeWithModifier to secondSummariesWithModifier }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(3.375, items.single { it.subject == "Język polski" }.average, .0) // (from details): 3.375
    }

    @Test
    fun `calc current semester average`() {
        every { preferencesRepository.gradeAverageForceCalc } returns false
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to secondSummaries }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(2.9, items.single { it.subject == "Matematyka" }.average, .0) // from summary: 2,9
        assertEquals(3.4, items.single { it.subject == "Fizyka" }.average, .0) // from details: 3,4
    }

    @Test
    fun `force calc current semester average`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ONE_SEMESTER
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to secondSummaries }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(2.5, items.single { it.subject == "Matematyka" }.average, .0) // from details: 2,5
        assertEquals(3.0, items.single { it.subject == "Fizyka" }.average, .0) // from details: 3,0
    }

    @Test
    fun `force calc full year average when current is first`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR
        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to firstSummaries }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[1].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.5, items.single { it.subject == "Matematyka" }.average, .0) // (from summary): 3,5
        assertEquals(3.5, items.single { it.subject == "Fizyka" }.average, .0) // (from summary): 3,5
    }

    @Test
    fun `calc both semesters average`() {
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS
        every { preferencesRepository.gradeAverageForceCalc } returns false
        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            firstGrades to listOf(
                getSummary(22, "Matematyka", 3.0),
                getSummary(22, "Fizyka", 3.5)
            )
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            secondGrades to listOf(
                getSummary(22, "Matematyka", 3.5),
                getSummary(22, "Fizyka", 4.0)
            )
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.25, items.single { it.subject == "Matematyka" }.average, .0) // (from summaries ↑): 3,0 + 3,5 → 3,25
        assertEquals(3.75, items.single { it.subject == "Fizyka" }.average, .0) // (from summaries ↑): 3,5 + 4,0 → 3,75
    }

    @Test
    fun `force calc full year average`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR
        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to firstSummaries }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            secondGrades to listOf(
                getSummary(22, "Matematyka", 1.1),
                getSummary(22, "Fizyka", 7.26)
            )
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0) // (from details): 3,5 + 2,5 → 3,0
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0) // (from details): 3,5  + 3,0 → 3,25
    }

    @Test
    fun `calc both semesters average when no summaries`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to emptyList<GradeSummary>() }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to emptyList<GradeSummary>() }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0) // (from details): 3,5 + 2,5 → 3,0
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0) // (from details): 3,5 + 3,0 → 3,25
    }

    @Test
    fun `force calc full year average when no summaries`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to emptyList<GradeSummary>() }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to emptyList<GradeSummary>() }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0) // (from details): 3,5 + 2,5 → 3,0
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0) // (from details): 3,5  + 3,0 → 3,25
    }

    @Test
    fun `calc both semesters average when missing summaries in both semesters`() {
        every { preferencesRepository.gradeAverageForceCalc } returns false
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            firstGrades to listOf(
                getSummary(22, "Matematyka", 4.0)
            )
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            secondGrades to listOf(
                getSummary(23, "Matematyka", 3.0)
            )
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.5, items.single { it.subject == "Matematyka" }.average, .0) // (from summaries ↑): 4,0 + 3,0 → 3,5
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0) // (from details): 3,5  + 3,0 → 3,25
    }

    @Test
    fun `calc both semesters average when missing summary in second semester`() {
        every { preferencesRepository.gradeAverageForceCalc } returns false
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to firstSummaries }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to secondSummaries.dropLast(1) }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.4, items.single { it.subject == "Matematyka" }.average, .0) // (from summaries): 3,9 + 2,9 → 3,4
        assertEquals(3.05, items.single { it.subject == "Fizyka" }.average, .0) // 3,1 (from summary) + 3,0 (from details) → 3,05
    }

    @Test
    fun `calc both semesters average when missing summary in first semester`() {
        every { preferencesRepository.gradeAverageForceCalc } returns false
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to firstSummaries.dropLast(1) }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to secondSummaries }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.4, items.single { it.subject == "Matematyka" }.average, .0) // (from summaries): 3,9 + 2,9 → 3,4
        assertEquals(3.45, items.single { it.subject == "Fizyka" }.average, .0) // 3,5 (from details) + 3,4 (from summary) → 3,45
    }

    @Test
    fun `force calc full year average when missing summary in first semester`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource { firstGrades to firstSummaries.dropLast(1) }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource { secondGrades to secondSummaries }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0) // (from details): 3,5 + 2,5 → 3,0
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0) // (from details): 3,5  + 3,0 → 3,25
    }

    @Test
    fun `force calc both semesters average with different average from all grades and from two semesters`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            listOf(
                getGrade(22, "Fizyka", 5.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 5.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0)
            ) to listOf(getSummary(semesterId = 22, subject = "Fizyka", average = .0))
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            listOf(
                getGrade(23, "Fizyka", 5.0, weight = 1.0),
                getGrade(23, "Fizyka", 5.0, weight = 2.0),
                getGrade(23, "Fizyka", 4.0, modifier = 0.3, weight = 2.0)
            ) to listOf(getSummary(semesterId = 23, subject = "Fizyka", average = .0))
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(5.2296, items.single { it.subject == "Fizyka" }.average, .0001) // (from details): 5.72727272 + 4,732 → 5.229636363636364
    }

    @Test
    fun `force calc full year average with different average from all grades and from two semesters`() {
        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            listOf(
                getGrade(22, "Fizyka", 5.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 5.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0)
            ) to listOf(getSummary(semesterId = 22, subject = "Fizyka", average = .0))
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            listOf(
                getGrade(23, "Fizyka", 5.0, weight = 1.0),
                getGrade(23, "Fizyka", 5.0, weight = 2.0),
                getGrade(23, "Fizyka", 4.0, modifier = 0.3, weight = 2.0)
            ) to listOf(getSummary(semesterId = 23, subject = "Fizyka", average = .0))
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(5.5429, items.single { it.subject == "Fizyka" }.average, .0001) // (from details): 5.72727272 + 4,732 → .average()
    }

    @Test
    fun `force calc both semesters average with different average from all grades and from two semesters with custom modifiers`() {
        val student = student.copy(loginMode = Sdk.Mode.SCRAPPER.name)

        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeMinusModifier } returns .33
        every { preferencesRepository.gradePlusModifier } returns .5

        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.BOTH_SEMESTERS
        coEvery { semesterRepository.getSemesters(student) } returns semesters

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            listOf(
                getGrade(22, "Fizyka", 5.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 5.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0)
            ) to listOf(getSummary(semesterId = 22, subject = "Fizyka", average = .0))
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            listOf(
                getGrade(23, "Fizyka", 5.0, weight = 1.0),
                getGrade(23, "Fizyka", 5.0, weight = 2.0),
                getGrade(23, "Fizyka", 4.0, modifier = 0.33, weight = 2.0)
            ) to listOf(getSummary(semesterId = 23, subject = "Fizyka", average = .0))
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(5.2636, items.single { it.subject == "Fizyka" }.average, .0001) // (from details): 5.72727272 + 4,8 → 5.26363636
    }

    @Test
    fun `force calc full year average with different average from all grades and from two semesters with custom modifiers`() {
        val student = student.copy(loginMode = Sdk.Mode.SCRAPPER.name)

        every { preferencesRepository.gradeAverageForceCalc } returns true
        every { preferencesRepository.gradeMinusModifier } returns .33
        every { preferencesRepository.gradePlusModifier } returns .5

        every { preferencesRepository.gradeAverageMode } returns GradeAverageMode.ALL_YEAR
        coEvery { semesterRepository.getSemesters(student) } returns semesters

        coEvery { gradeRepository.getGrades(student, semesters[1], true) } returns flowWithResource {
            listOf(
                getGrade(22, "Fizyka", 5.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 5.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 4.0),
                getGrade(22, "Fizyka", 6.0, weight = 2.0)
            ) to listOf(getSummary(semesterId = 22, subject = "Fizyka", average = .0))
        }
        coEvery { gradeRepository.getGrades(student, semesters[2], true) } returns flowWithResource {
            listOf(
                getGrade(23, "Fizyka", 5.0, weight = 1.0),
                getGrade(23, "Fizyka", 5.0, weight = 2.0),
                getGrade(23, "Fizyka", 4.0, modifier = 0.33, weight = 2.0)
            ) to listOf(getSummary(semesterId = 23, subject = "Fizyka", average = .0))
        }

        val items = runBlocking { gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).getResult() }

        assertEquals(5.5555, items.single { it.subject == "Fizyka" }.average, .0001) // (from details): 5.72727272  + 4,8 → .average()
    }

    private fun getGrade(semesterId: Int, subject: String, value: Double, modifier: Double = 0.0, weight: Double = 1.0): Grade {
        return Grade(
            studentId = 101,
            semesterId = semesterId,
            subject = subject,
            value = value,
            modifier = modifier,
            weightValue = weight,
            teacher = "",
            date = now(),
            weight = "",
            gradeSymbol = "",
            entry = "",
            description = "",
            comment = "",
            color = ""
        )
    }

    private fun getSummary(semesterId: Int, subject: String, average: Double): GradeSummary {
        return GradeSummary(
            studentId = 101,
            semesterId = semesterId,
            subject = subject,
            average = average,
            pointsSum = "",
            proposedPoints = "",
            finalPoints = "",
            finalGrade = "",
            predictedGrade = "",
            position = 0
        )
    }
}
