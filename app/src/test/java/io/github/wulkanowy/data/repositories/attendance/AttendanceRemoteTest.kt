package io.github.wulkanowy.data.repositories.attendance

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.getStudentEntity
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Attendance
import io.github.wulkanowy.utils.init
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDate.of

class AttendanceRemoteTest {

    @SpyK
    private var mockSdk = Sdk()

    @MockK
    private lateinit var semesterMock: Semester

    private var student = getStudentEntity()

    @Before
    fun initApi() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getAttendanceTest() {
        every { mockSdk.init(student) } returns mockSdk
        coEvery {
            mockSdk.getAttendance(
                of(2018, 9, 10),
                of(2018, 9, 15),
                1
            )
        } returns listOf(
            getAttendance(of(2018, 9, 10)),
            getAttendance(of(2018, 9, 17))
        )

        every { semesterMock.studentId } returns 1
        every { semesterMock.diaryId } returns 1
        every { semesterMock.schoolYear } returns 2019
        every { semesterMock.semesterId } returns 1
        every { mockSdk.switchDiary(any(), any()) } returns mockSdk

        val attendance = runBlocking {
            AttendanceRemote(mockSdk).getAttendance(student, semesterMock,
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        }
        assertEquals(2, attendance.size)
    }

    private fun getAttendance(date: LocalDate): Attendance {
        return Attendance(
            subject = "Fizyka",
            name = "Obecność",
            date = date,
            timeId = 0,
            number = 0,
            deleted = false,
            excusable = false,
            excused = false,
            exemption = false,
            lateness = false,
            presence = false,
            categoryId = 1,
            absence = false,
            excuseStatus = null
        )
    }
}
