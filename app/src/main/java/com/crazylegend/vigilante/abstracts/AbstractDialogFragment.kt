package com.crazylegend.vigilante.abstracts

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.crazylegend.vigilante.R


/**
 * Created by crazy on 4/27/20 to long live and prosper !
 */
abstract class AbstractDialogFragment(contentLayoutId: Int) : DialogFragment(contentLayoutId) {

    abstract val binding: ViewBinding
    abstract val dimAmount: Float?
    abstract val isCancellable: Boolean


    override fun onCreateDialog(savedInstanceState: Bundle?) = with(super.onCreateDialog(savedInstanceState)) {
        window?.attributes?.windowAnimations = R.style.DialogAnimation
        setCancelable(isCancellable)
        window?.setDimAmount(dimAmount ?: 0f)
        setCanceledOnTouchOutside(isCancellable)
        window?.setBackgroundDrawableResource(R.drawable.rounded_bg_theme_compatible)
        this
    }
}