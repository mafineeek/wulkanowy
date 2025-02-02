package io.github.wulkanowy.ui.modules.message.tab

import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.repositories.message.MessageFolder
import io.github.wulkanowy.data.repositories.message.MessageRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.afterLoading
import io.github.wulkanowy.utils.flowWithResourceIn
import io.github.wulkanowy.utils.toFormattedString
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.xdrop.fuzzywuzzy.FuzzySearch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

class MessageTabPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val messageRepository: MessageRepository,
    private val semesterRepository: SemesterRepository,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<MessageTabView>(errorHandler, studentRepository) {

    lateinit var folder: MessageFolder

    private lateinit var lastError: Throwable

    private var lastSearchQuery = ""

    private var messages = emptyList<Message>()

    private val searchChannel = Channel<String>()

    @FlowPreview
    fun onAttachView(view: MessageTabView, folder: MessageFolder) {
        super.onAttachView(view)
        view.initView()
        initializeSearchStream()
        errorHandler.showErrorMessage = ::showErrorViewOnError
        this.folder = folder
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the $folder message")
        onParentViewLoadData(true)
    }

    fun onRetry() {
        view?.run {
            showErrorView(false)
            showProgress(true)
        }
        loadData(true)
    }

    fun onDetailsClick() {
        view?.showErrorDetailsDialog(lastError)
    }

    fun onDeleteMessage() {
        loadData(true)
    }

    fun onParentViewLoadData(forceRefresh: Boolean) {
        loadData(forceRefresh)
    }

    fun onMessageItemSelected(message: Message, position: Int) {
        Timber.i("Select message ${message.id} item (position: $position)")
        view?.openMessage(message)
    }

    private fun loadData(forceRefresh: Boolean) {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent()
            val semester = semesterRepository.getCurrentSemester(student)
            messageRepository.getMessages(student, semester, folder, forceRefresh)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading $folder message data started")
                Status.SUCCESS -> {
                    Timber.i("Loading $folder message result: Success")
                    messages = it.data!!
                    updateData(getFilteredData(lastSearchQuery))
                    analytics.logEvent(
                        "load_data",
                        "type" to "messages",
                        "items" to it.data.size,
                        "folder" to folder.name
                    )
                }
                Status.ERROR -> {
                    Timber.i("Loading $folder message result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                }
            }
        }.afterLoading {
            view?.run {
                showRefresh(false)
                showProgress(false)
                enableSwipe(true)
                notifyParentDataLoaded()
            }
        }.launch()
    }

    private fun showErrorViewOnError(message: String, error: Throwable) {
        view?.run {
            if (isViewEmpty) {
                lastError = error
                setErrorDetails(message)
                showErrorView(true)
                showEmpty(false)
            } else showError(message, error)
        }
    }

    fun onSearchQueryTextChange(query: String) {
        launch {
            searchChannel.send(query)
        }
    }

    @FlowPreview
    private fun initializeSearchStream() {
        launch {
            searchChannel.consumeAsFlow()
                .debounce(250)
                .map { query ->
                    lastSearchQuery = query
                    getFilteredData(query)
                }
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Applying filter. Full list: ${messages.size}, filtered: ${it.size}")
                    updateData(it)
                    view?.resetListPosition()
                }
        }
    }

    private fun getFilteredData(query: String): List<Message> {
        return if (query.trim().isEmpty()) {
            messages.sortedByDescending { it.date }
        } else {
            messages
                .map { it to calculateMatchRatio(it, query) }
                .sortedByDescending { it.second }
                .filter { it.second > 5000 }
                .map { it.first }
        }
    }

    private fun updateData(data: List<Message>) {
        view?.run {
            showEmpty(data.isEmpty())
            showContent(data.isNotEmpty())
            showErrorView(false)
            updateData(data)
        }
    }

    private fun calculateMatchRatio(message: Message, query: String): Int {
        val subjectRatio = FuzzySearch.tokenSortPartialRatio(
            query.toLowerCase(Locale.getDefault()),
            message.subject
        )

        val senderOrRecipientRatio = FuzzySearch.tokenSortPartialRatio(
            query.toLowerCase(Locale.getDefault()),
            if (message.sender.isNotEmpty()) message.sender.toLowerCase(Locale.getDefault())
            else message.recipient.toLowerCase(Locale.getDefault())
        )

        val dateRatio = listOf(
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("dd.MM").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("dd.MM.yyyy").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("d MMMM").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("d MMMM yyyy").toLowerCase(Locale.getDefault())
            )
        ).maxOrNull() ?: 0


        return (subjectRatio.toDouble().pow(2)
            + senderOrRecipientRatio.toDouble().pow(2)
            + dateRatio.toDouble().pow(2) * 2
            ).toInt()
    }
}
