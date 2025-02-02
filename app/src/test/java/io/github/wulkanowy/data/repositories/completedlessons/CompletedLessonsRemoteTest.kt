package io.github.wulkanowy.data.repositories.completedlessons

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.getStudentEntity
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.CompletedLesson
import io.github.wulkanowy.utils.init
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDate.of

class CompletedLessonsRemoteTest {

    @SpyK
    private var mockSdk = Sdk()

    @MockK
    private lateinit var semesterMock: Semester

    private val student = getStudentEntity()

    @Before
    fun initApi() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getCompletedLessonsTest() {
        every { mockSdk.init(student) } returns mockSdk
        coEvery {
            mockSdk.getCompletedLessons(
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        } returns listOf(
            getCompletedLesson(of(2018, 9, 10)),
            getCompletedLesson(of(2018, 9, 17))
        )

        every { semesterMock.studentId } returns 1
        every { semesterMock.diaryId } returns 1
        every { semesterMock.schoolYear } returns 2019
        every { semesterMock.semesterId } returns 1
        every { mockSdk.switchDiary(any(), any()) } returns mockSdk

        val completed = runBlocking {
            CompletedLessonsRemote(mockSdk).getCompletedLessons(student, semesterMock,
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        }
        Assert.assertEquals(2, completed.size)
    }

    private fun getCompletedLesson(date: LocalDate): CompletedLesson {
        return CompletedLesson(
            date = date,
            subject = "",
            absence = "",
            resources = "",
            substitution = "",
            teacherSymbol = "",
            teacher = "",
            topic = "",
            number = 1
        )
    }
}
