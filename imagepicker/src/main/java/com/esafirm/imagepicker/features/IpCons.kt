package com.esafirm.imagepicker.features

object IpCons {
    const val MAX_LIMIT = 999
    const val MAX_VIDEO_LIMIT = 1
    const val VIDEO_DURATION_LIMIT = 60000
    const val VIDEO_SIZE_LIMIT = 180

    @Deprecated("You should use the new API to start image picker")
    const val RC_IMAGE_PICKER = 0x229
    const val EXTRA_SELECTED_IMAGES = "selectedImages"
}