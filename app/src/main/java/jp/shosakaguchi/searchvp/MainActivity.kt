package jp.shosakaguchi.searchvp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.Observer
import jp.shosakaguchi.searchvp.databinding.ActivityMainBinding
import jp.shosakaguchi.searchvp.viewmodel.MainViewModel
import me.rosuh.filepicker.config.FilePickerManager
import org.opencv.android.OpenCVLoader
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions


@RuntimePermissions
class MainActivity : AppCompatActivity() {
    lateinit var mainViewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = MainViewModel()
        val binding: ActivityMainBinding = setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        mainViewModel = viewModel
        if (OpenCVLoader.initDebug()) {
            Log.d("Debug", "Load OpenCV Successfully")
        }
        startObserve()
    }

    private fun startObserve() {
        mainViewModel.startImagePick.observe(this, Observer {
            if (it) {
                imagePick()
            }
        })
        mainViewModel.imagePathUri.observe(this, Observer {
            it?.let {
                mainViewModel.processImageWrite()
            }
        })
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    private fun imagePick() {
        FilePickerManager
            .from(this)
            .maxSelectable(1)
            .forResult(FilePickerManager.REQUEST_CODE)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FilePickerManager.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val filePath = FilePickerManager.obtainData()[0]
                    mainViewModel.setImageUri(filePath)
                    FilePickerManager.release()
                } else {
                    // noop
                }
            }
        }

    }
}

