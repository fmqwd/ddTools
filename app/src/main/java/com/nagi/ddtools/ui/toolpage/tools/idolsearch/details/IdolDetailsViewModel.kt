package com.nagi.ddtools.ui.toolpage.tools.idolsearch.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nagi.ddtools.data.MediaList
import com.nagi.ddtools.data.Resource
import com.nagi.ddtools.data.TagsList
import com.nagi.ddtools.database.AppDatabase
import com.nagi.ddtools.database.activityList.ActivityList
import com.nagi.ddtools.database.activityList.ActivityListDao
import com.nagi.ddtools.database.idolGroupList.IdolGroupList
import com.nagi.ddtools.database.idolGroupList.IdolGroupListDao
import com.nagi.ddtools.database.user.User
import com.nagi.ddtools.resourceGet.NetGet
import com.nagi.ddtools.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdolDetailsViewModel : ViewModel() {
    private val _users = MutableLiveData<User>()
    val users: LiveData<User> = _users

    private val _data = MutableLiveData<IdolGroupList>()
    val data: LiveData<IdolGroupList> = _data

    private val _tags = MutableLiveData<List<TagsList>>()
    val tags: LiveData<List<TagsList>> = _tags

    private val _medias = MutableLiveData<List<MediaList>>()
    val medias: LiveData<List<MediaList>> = _medias

    private val _memberList = MutableLiveData<List<IdolGroupList>>()
    val memberList: LiveData<List<IdolGroupList>> = _memberList

    private val _activityList = MutableLiveData<List<ActivityList>>()
    val activityList: LiveData<List<ActivityList>> = _activityList

    private val activityListDao: ActivityListDao by lazy {
        AppDatabase.getInstance().activityListDao()
    }
    private val groupListDao: IdolGroupListDao by lazy {
        AppDatabase.getInstance().idolGroupListDao()
    }

    fun setId(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val detailsData = groupListDao.getById(id)
                _data.postValue(detailsData)
                val memberResult = mutableListOf<IdolGroupList>()
                val memberResultString = detailsData.memberIds
                if (!memberResultString.isNullOrEmpty()) {
                    val memberResultList = memberResultString.split(",")
                    for (memberId in memberResultList) {
                        if (memberId.isNotEmpty()) {
                            groupListDao.getById(memberId.toInt()).let { member ->
                                memberResult.add(member)
                            }
                        }
                    }
                    _memberList.postValue(memberResult)
                }
                val activityResult = mutableListOf<ActivityList>()
                val activityIdListString = detailsData.activityIds
                if (!activityIdListString.isNullOrEmpty()) {
                    val activityIdList = activityIdListString.split(",")
                    for (activityId in activityIdList) {
                        if (activityId.isNotEmpty()) {
                            activityListDao.getById(activityId).let { activity ->
                                activityResult.add(activity)
                            }
                        }
                    }
                    _activityList.postValue(activityResult)
                }
                var mediaResult = mutableListOf<MediaList>()
                val mediaJsonString = detailsData.ext
                if (mediaJsonString.isNotEmpty()) {
                    try {
                        val mediaJson = JsonParser.parseString(detailsData.ext).asJsonObject
                        if (mediaJson.has("media")) {
                            val mediaList = mediaJson.get("media").asJsonArray
                            val itemType = object : TypeToken<List<MediaList>>() {}.type
                            mediaResult = Gson().fromJson(mediaList, itemType)
                        }
                        _medias.postValue(mediaResult)
                    } catch (e: Exception) {
                        LogUtils.e("Error getting media : + ${e.message}")
                    }
                }
            }
        }
    }

    fun getUser() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (AppDatabase.getInstance().userDao().getAll().isNotEmpty()) {
                    _users.postValue(AppDatabase.getInstance().userDao().getAll()[0])
                }
            }
        }
    }

    fun getTags(type: String, typeId: String) {
        NetGet.getTags(type, typeId) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val itemType = object : TypeToken<List<TagsList>>() {}.type
                    val tagList: List<TagsList> = Gson().fromJson(resource.data, itemType)

                    _tags.postValue(tagList)
                }

                is Resource.Error -> {
                    LogUtils.e("Error getting tags : + ${resource.message}")
                }
            }
        }
    }

    fun addTags(type: String, typeId: Int, content: String, callback: (String) -> Unit) {
        users.value?.let {
            NetGet.sendEvaluate(type, typeId, content, it.id) { result ->
                when (result) {
                    is Resource.Success -> callback(result.data)
                    is Resource.Error -> callback("error")
                }
            }
            getTags(type, typeId.toString())
        }
    }
}