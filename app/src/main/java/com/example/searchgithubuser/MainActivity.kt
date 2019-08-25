package com.example.searchgithubuser

import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.bumptech.glide.Glide
import com.example.searchgithubuser.model.SearchUserResponse
import com.example.searchgithubuser.model.User
import com.example.searchgithubuser.service.GithubServiceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_user.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.support.v4.content.ContextCompat.getSystemService
import android.net.ConnectivityManager



class Contract {
    interface View {
        fun showNoSearchResult()
        fun showSearchError()
        fun updateUserList(data: List<User>)
        fun showNoNetworkConnected()
    }

    interface Presenter {
        fun searchUser(userName: String = "")
        fun checkLoadNextPage(position: Int)
    }
}

class MainActivity : AppCompatActivity(), Contract.View {
    private lateinit var adapter: UserAdapter
    private lateinit var presenter: Contract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = MainPresenter(this, this)
        initView()
    }

    override fun showNoSearchResult() {
        stopLoading()
        showResultMessage(true, R.string.message_no_result)
    }

    override fun showSearchError() {
        stopLoading()
        showResultMessage(true, R.string.message_error)
    }

    override fun showNoNetworkConnected() {
        stopLoading()
        showResultMessage(true, R.string.message_no_network_connected)
    }

    override fun updateUserList(data: List<User>) {
        stopLoading()
        adapter.data = data
        showResultMessage(false)
    }

    private fun initView() {
        adapter = UserAdapter(this, presenter)
        userList.adapter = adapter
        userList.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        button.setOnClickListener {
            hideKeyboard(it)
            startLoading()
            presenter.searchUser(searchInput.text.toString())
        }
    }

    private fun startLoading() {
        button.isEnabled = false
        progressBar.visibility = View.VISIBLE
    }

    private fun stopLoading() {
        button.isEnabled = true
        progressBar.visibility = View.GONE
    }

    private fun showResultMessage(showMessage: Boolean = false, messageId: Int = 0) {
        if (showMessage) {
            message.setText(messageId)
        }

        message.visibility = if (showMessage) View.VISIBLE else View.GONE
        userList.visibility = if (showMessage) View.GONE else View.VISIBLE
    }

    private fun hideKeyboard(focusView: View) {
        focusView.clearFocus()
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(focusView.windowToken, 0)
    }

    class UserAdapter(private val activity: Activity,
                      private val presenter: Contract.Presenter) : RecyclerView.Adapter<UserViewHolder>() {

        var data: List<User> = ArrayList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent,false)
            return UserViewHolder(view)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            data[position].let { user ->
                holder.number.text = (position + 1).toString()
                holder.userName.text = user.account
                Glide.with(activity)
                    .load(user.avatar)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar).circleCrop()
                    .into(holder.userAvatar)
            }

            presenter.checkLoadNextPage(position)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number = view.number!!
        val userAvatar = view.avatar!!
        val userName = view.userName!!
    }
}

class MainPresenter(private val context: Context,
                    private val view: Contract.View) : Contract.Presenter {

    companion object {
        private const val COUNT_PER_PAGE = 30
        private const val AUTO_RELOAD_COUNT = 5
        private const val FIRST_PAGE = 1
    }

    private var resultCount = 0
    private var currentPage = 1
    private var isIncomplete = false
    private var userName = ""
    private var userList: List<User>? = ArrayList()
        set(value) {
            field = value

            if (field.isNullOrEmpty()) {
                view.showNoSearchResult()
                return
            }

            field?.let { view.updateUserList(it) }
        }

    override fun searchUser(userName: String) {
        this.userName = userName
        currentPage = 1

        if (userName.isEmpty()) {
            view.showNoSearchResult()
            return
        }

        doSearch(this.userName, currentPage)
    }

    override fun checkLoadNextPage(position: Int) {
        val maxPosition = currentPage * COUNT_PER_PAGE
        val hasMore = resultCount > maxPosition
        val shouldLoad = maxPosition - position <= AUTO_RELOAD_COUNT

        if (hasMore && shouldLoad) {
            currentPage ++
            doSearch(userName, currentPage)
        }
    }

    private fun doSearch(userName: String, searchPage: Int) {
        if (!isNetworkConnected()) return

        GithubServiceManager.instance
            .searchUsers(userName, searchPage, COUNT_PER_PAGE)
            .enqueue(object : Callback<SearchUserResponse> {
                override fun onFailure(call: Call<SearchUserResponse>, t: Throwable) {
                    call.cancel()
                    view.showSearchError()
                }

                override fun onResponse(call: Call<SearchUserResponse>, response: Response<SearchUserResponse>) {
                    Log.i("MainPresenter", "response : " + response.body().toString())
                    if (!response.isSuccessful) {
                        view.showSearchError()
                        return
                    }

                    val result = response.body()

                    result?.let {
                        resultCount = it.totalCount
                        isIncomplete = it.isIncomplete

                        userList = if (searchPage == FIRST_PAGE) {
                            it.users
                        } else {
                            userList?.plus(it.users)
                        }
                    }
                }

            })
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(context, ConnectivityManager::class.java) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected

        if (!isConnected) {
            view.showNoNetworkConnected()
        }

        return isConnected
    }
}
