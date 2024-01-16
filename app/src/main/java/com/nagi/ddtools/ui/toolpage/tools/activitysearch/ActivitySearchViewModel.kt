package com.nagi.ddtools.ui.toolpage.tools.activitysearch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nagi.ddtools.database.AppDatabase
import com.nagi.ddtools.database.activityList.ActivityList
import com.nagi.ddtools.database.activityList.ActivityListDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ActivitySearchViewModel : ViewModel() {
    private val _activityData = MutableLiveData<List<ActivityList>>()
    private val _locationData = MutableLiveData<Set<String>>()
    private val _dateData = MutableLiveData<Set<String>>()
    private val database: ActivityListDao by lazy {
        AppDatabase.getInstance()!!.activityListDao()
    }

    val dateListData: LiveData<Set<String>> = _dateData
    val locationListData: LiveData<Set<String>> = _locationData
    val activityListData: LiveData<List<ActivityList>> = _activityData
    fun loadActivityList(jsonString: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val itemType = object : TypeToken<List<ActivityList>>() {}.type
                val activityList: List<ActivityList> = Gson().fromJson(jsonString, itemType)
                val dateList:Set<String> = activityList.map { it.duration_date }.toSet()
                val locationList:Set<String> = activityList.map { it.location }.toSet()
                _activityData.postValue(activityList)
                _locationData.postValue(locationList)
                _dateData.postValue(dateList)
                database.insertAll(activityList)
            }
        }
    }

    fun getActivityListByLocation(lo: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (lo == "全世界") database.getAll() else database.getByLocation(lo)
            }
            _activityData.postValue(result)
        }
    }

    fun getActivityListByDate(date: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                database.getByDateAsToday(date)
            }
            _activityData.postValue(result)
        }
    }

    fun getActivityListByStatus(date: String) {
        val currentDate = LocalDate.now()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateString = currentDate.format(dateFormat)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                when (date) {
                    "未开始" -> database.getByDateAfter(dateString)
                    "进行中" -> database.getByDateAsToday(dateString)
                    "已结束" -> database.getByDateBefore(dateString)
                    else -> {
                        database.getAll()
                    }
                }
            }
            _activityData.postValue(result)
        }
    }
}