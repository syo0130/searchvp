package jp.shosakaguchi.searchvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.shosakaguchi.searchvp.tools.Vp
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _startImagePick: MutableLiveData<Boolean> = MutableLiveData(false)
    val startImagePick
        get() = _startImagePick

    private val _imagePathUri: MutableLiveData<String> = MutableLiveData(null)
    val imagePathUri: LiveData<String>
        get() = _imagePathUri

    fun startImagePickAction() {
        _startImagePick.value = true
    }

    fun setImageUri(imagePath: String) {
        _imagePathUri.value = imagePath
    }

    fun processImageWrite() {
        viewModelScope.launch {
            Vp().imageProcessForVanishingPoint(_imagePathUri.value.toString())
        }
    }
}