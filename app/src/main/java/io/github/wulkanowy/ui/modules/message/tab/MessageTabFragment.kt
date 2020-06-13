package io.github.wulkanowy.ui.modules.message.tab

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.repositories.message.MessageFolder
import io.github.wulkanowy.databinding.FragmentMessageTabBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.message.MessageFragment
import io.github.wulkanowy.ui.modules.message.preview.MessagePreviewFragment
import io.github.wulkanowy.ui.widgets.DividerItemDecoration
import javax.inject.Inject

class MessageTabFragment : BaseFragment<FragmentMessageTabBinding>(R.layout.fragment_message_tab),
    MessageTabView {

    @Inject
    lateinit var presenter: MessageTabPresenter

    @Inject
    lateinit var tabAdapter: MessageTabAdapter

    private var searchMenuItem: MenuItem? = null

    companion object {
        const val MESSAGE_TAB_FOLDER_ID = "message_tab_folder_id"
        const val REQUEST_CODE_SPEECH_RECOGNITION = 10

        fun newInstance(folder: MessageFolder): MessageTabFragment {
            return MessageTabFragment().apply {
                arguments = Bundle().apply {
                    putString(MESSAGE_TAB_FOLDER_ID, folder.name)
                }
            }
        }
    }

    override val isViewEmpty
        get() = tabAdapter.itemCount == 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMessageTabBinding.bind(view)
        messageContainer = binding.messageTabRecycler
        presenter.onAttachView(this, MessageFolder.valueOf(
            (savedInstanceState ?: arguments)?.getString(MESSAGE_TAB_FOLDER_ID).orEmpty()
        ))
    }

    override fun initView() {
        tabAdapter.onClickListener = presenter::onMessageItemSelected

        with(binding.messageTabRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = tabAdapter
            addItemDecoration(DividerItemDecoration(context))
        }
        with(binding) {
            messageTabSwipe.setOnRefreshListener { presenter.onSwipeRefresh() }
            messageTabErrorRetry.setOnClickListener { presenter.onRetry() }
            messageTabErrorDetails.setOnClickListener { presenter.onDetailsClick() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.action_menu_message_tab, menu)

        searchMenuItem = menu.findItem(R.id.action_search)
        (searchMenuItem?.actionView as SearchView?)?.apply {
            queryHint = getString(R.string.all_search_hint)
            maxWidth = Int.MAX_VALUE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false
                override fun onQueryTextChange(query: String): Boolean {
                    presenter.onSearchQueryTextChange(query)
                    return true
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> true
            R.id.action_search_voice -> {
                startVoiceRecognition()
                true
            }
            else -> false
        }
    }

    override fun updateData(data: List<Message>) {
        tabAdapter.replaceAll(data)
    }

    override fun updateItem(item: Message, position: Int) {
        tabAdapter.updateItem(position, item)
    }

    override fun showProgress(show: Boolean) {
        binding.messageTabProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun enableSwipe(enable: Boolean) {
        binding.messageTabSwipe.isEnabled = enable
    }

    override fun resetListPosition() {
        binding.messageTabRecycler.scrollToPosition(0)
    }

    override fun showContent(show: Boolean) {
        binding.messageTabRecycler.visibility = if (show) VISIBLE else INVISIBLE
    }

    override fun showEmpty(show: Boolean) {
        binding.messageTabEmpty.visibility = if (show) VISIBLE else INVISIBLE
    }

    override fun showErrorView(show: Boolean) {
        binding.messageTabError.visibility = if (show) VISIBLE else GONE
    }

    override fun setErrorDetails(message: String) {
        binding.messageTabErrorMessage.text = message
    }

    override fun showRefresh(show: Boolean) {
        binding.messageTabSwipe.isRefreshing = show
    }

    override fun openMessage(message: Message) {
        (activity as? MainActivity)?.pushView(MessagePreviewFragment.newInstance(message))
    }

    override fun notifyParentDataLoaded() {
        (parentFragment as? MessageFragment)?.onChildFragmentLoaded()
    }

    fun onParentLoadData(forceRefresh: Boolean) {
        presenter.onParentViewLoadData(forceRefresh)
    }

    fun onParentDeleteMessage() {
        presenter.onDeleteMessage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(MESSAGE_TAB_FOLDER_ID, presenter.folder.name)
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        startActivityForResult(intent, REQUEST_CODE_SPEECH_RECOGNITION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SPEECH_RECOGNITION && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0).let { match ->
                searchMenuItem?.apply {
                    expandActionView()
                    val searchView = actionView as SearchView?
                    searchView?.setQuery(match, true)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
