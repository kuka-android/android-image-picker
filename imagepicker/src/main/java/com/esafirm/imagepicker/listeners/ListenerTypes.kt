package com.esafirm.imagepicker.listeners

import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image

typealias OnImageClickListener = (image: Image, isSelected: Boolean) -> Boolean
typealias OnFolderClickListener = (Folder) -> Unit
typealias OnImageSelectedListener = (List<Image>) -> Unit
