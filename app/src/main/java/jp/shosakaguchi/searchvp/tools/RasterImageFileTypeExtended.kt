package jp.shosakaguchi.searchvp.tools

import jp.shosakaguchi.searchvp.R
import me.rosuh.filepicker.filetype.FileType

class RasterImageFileTypeExtended : FileType {

    override val fileType: String
        get() = "Image"
    override val fileIconResId: Int
        get() = R.drawable.ic_image_file_picker

    override fun verify(fileName: String): Boolean {
        val isHasSuffix = fileName.contains(".")
        if (!isHasSuffix) {
            return false
        }
        val suffix = fileName.substring(fileName.lastIndexOf(".") + 1)
        return when (suffix) {
            "jpeg", "jpg", "bmp", "dds", "gif", "png", "psd", "pspimage", "tga", "thm", "tif", "tiff", "yuv", "JPEG", "JPG", "BMP", "DDS", "GIF", "PNG", "PSD", "PSPIMAGE", "TGA", "THM", "TIF", "TIFF", "YUV"
            -> {
                true
            }
            else -> {
                false
            }
        }
    }
}