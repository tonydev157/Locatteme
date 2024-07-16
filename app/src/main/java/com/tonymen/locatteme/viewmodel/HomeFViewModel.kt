package com.tonymen.locatteme.viewmodel

import PostDataSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.tonymen.locatteme.model.Post
import kotlinx.coroutines.flow.Flow

class HomeFViewModel : ViewModel() {

    val posts: Flow<PagingData<Post>> = Pager(PagingConfig(pageSize = 10)) {
        PostDataSource()
    }.flow.cachedIn(viewModelScope)
}
