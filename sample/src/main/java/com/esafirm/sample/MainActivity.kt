package com.esafirm.sample

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.esafirm.imagepicker.features.ImagePickerConfig
import com.esafirm.imagepicker.features.ImagePickerSavePath
import com.esafirm.imagepicker.features.registerImagePicker
import com.esafirm.imagepicker.model.Image
import com.esafirm.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        binding.run {
            buttonPickImage.setOnClickListener { openImagePicker() }
        }
    }

    private val images = arrayListOf<Image>()
    private val imagePickerLauncher = registerImagePicker {
        images.clear()
        images.addAll(it)
        printImages(images)
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch(createConfig())
    }

    private fun createConfig(): ImagePickerConfig {
        return ImagePickerConfig {
            language = "tr" // Set image picker language
            theme = R.style.ImagePickerTheme
            isIncludeVideo = true // include video (false by default)
            isFolderMode = false // set folder mode (false by default)
            arrowColor = Color.WHITE // set toolbar arrow up color
            folderTitle = "Tümü" // folder selection title
            imageTitle = "Seçmek için tıklayın" // image selection title
            doneButtonText = "TAMAM" // done button text
            showDoneButtonAlways = false    // Show done button always or not
            limit = 5 - images.size// max images can be selected (99 by default)
            isShowCamera = true // show camera or not (true by default)
            savePath = ImagePickerSavePath("Camera") // captured image directory name ("Camera" folder by default)
            savePath = ImagePickerSavePath(Environment.getExternalStorageDirectory().path, isRelative = false) // can be a full path
            selectedImagePathList = images.map { it.path }

//            mode = if (false) {
//                ImagePickerMode.SINGLE
//            } else {
//                ImagePickerMode.MULTIPLE // multi mode (default mode)
//            }

            // set whether pick action or camera action should return immediate result or not. Only works in single mode for image picker
//            returnMode = if (false) ReturnMode.ALL else ReturnMode.NONE
//            isOnlyVideo = false // include video (false by default)
        }
    }

    private fun printImages(images: List<Image>?) {
        if (images == null) return
        binding.textView.text = images.joinToString("\n")
        binding.textView.setOnClickListener {
            ImageViewerActivity.start(this@MainActivity, images)
        }
    }
}