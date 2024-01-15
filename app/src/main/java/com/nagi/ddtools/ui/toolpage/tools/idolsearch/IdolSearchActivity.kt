package com.nagi.ddtools.ui.toolpage.tools.idolsearch

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nagi.ddtools.R
import com.nagi.ddtools.database.idolGroupList.IdolGroupList
import com.nagi.ddtools.databinding.ActivityIdolSearchBinding
import com.nagi.ddtools.ui.adapter.IdolGroupListAdapter
import com.nagi.ddtools.utils.FileUtils
import com.nagi.ddtools.utils.LogUtils
import com.nagi.ddtools.utils.NetUtils
import com.nagi.ddtools.utils.UiUtils
import com.nagi.ddtools.utils.UiUtils.toast
import java.io.File


class IdolSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIdolSearchBinding
    private lateinit var adapter: IdolGroupListAdapter
    private var isAdapterInitialized = false
    private var chooseWhich = 0
    private val viewModel: IdolSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdolSearchBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        initView()
        setupStatusBar()
        viewModel.idolGroupData.observe(this) { data -> updateAdapter(data) }
    }

    private fun initView() {
        binding.searchTitleBack.setOnClickListener { finish() }
        binding.searchResearch.setOnClickListener { reGetData() }
        binding.searchLocation.setOnClickListener { updateIdolGroupData() }
        binding.searchSwitchSearch.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchColors(isChecked)
        }
        initAdapter()
    }

    private fun initAdapter() {
        val inputStream = File(applicationContext.filesDir, FileUtils.IDOL_GROUP_FILE)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        viewModel.loadIdolGroupData(jsonString)
        if (!isAdapterInitialized) {
            adapter = IdolGroupListAdapter(mutableListOf())
        }
        binding.searchRecycler.adapter = adapter
        isAdapterInitialized = true
    }

    private fun updateSwitchColors(isChecked: Boolean) {
        if (isChecked) {
            binding.searchSwitchTextLeft.setTextColor(Color.BLACK)
            binding.searchSwitchTextRight.setTextColor(resources.getColor(R.color.lty, null))
        } else {
            binding.searchSwitchTextLeft.setTextColor(resources.getColor(R.color.lty, null))
            binding.searchSwitchTextRight.setTextColor(Color.BLACK)
        }
    }

    private fun updateIdolGroupData() {
        viewModel.locationData.observe(this) { options ->
            val data = options.toList() as ArrayList<String>
            val builder = AlertDialog.Builder(this)
            data.add(0, resources.getText(R.string.search_location_choose).toString())
            builder.setTitle(resources.getText(R.string.please_choose).toString())
            builder.setSingleChoiceItems(
                data.toTypedArray(),
                chooseWhich
            ) { _, which ->
                chooseWhich = which
            }
            builder.setPositiveButton(getText(R.string.sure)) { _, _ ->
                binding.searchLocation.text = data[chooseWhich]
                viewModel.getIdolGroupListByLocation(data[chooseWhich])
            }
            builder.setNegativeButton(getText(R.string.cancel)) { _, _ -> }
            val dialog = builder.create()
            dialog.show()
        }


    }

    private var lastClickTime: Long = 0
    private val debounceTime: Long = 10000
    private fun reGetData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            UiUtils.showLoading(this)
            try {
                NetUtils.fetchAndSave(
                    "https://wiki.chika-idol.live/request/ddtools/getChikaIdolList.php/.",
                    NetUtils.HttpMethod.POST,
                    emptyMap(),
                    File(filesDir, FileUtils.IDOL_GROUP_FILE).path
                ) { success, message ->
                    if (!success) {
                        LogUtils.e("Failed to fetch idol group list: $message")
                    } else {
                        runOnUiThread { initAdapter() }
                    }
                    UiUtils.hideLoading()
                }
            } catch (e: Exception) {
                LogUtils.e("Exception during fetching idol group list: ${e.message}")
                UiUtils.hideLoading()
            }
        } else {
            applicationContext.toast("请等待10秒后再尝试", Toast.LENGTH_LONG)
        }
    }

    private fun updateAdapter(data: List<IdolGroupList>) {
        adapter.updateData(data)
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.lty)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
    }
}