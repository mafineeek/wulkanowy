package io.github.wulkanowy.ui.modules.grade

import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary

data class GradeDetailsWithAverage(
    val subject: String,
    val average: Double,
    val points: String,
    val summary: GradeSummary,
    val grades: List<Grade>
)
