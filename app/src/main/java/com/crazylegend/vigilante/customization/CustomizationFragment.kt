package com.crazylegend.vigilante.customization

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.coroutines.onMain
import com.crazylegend.crashyreporter.CrashyReporter
import com.crazylegend.kotlinextensions.fragments.fragmentBooleanResult
import com.crazylegend.kotlinextensions.fragments.shortToast
import com.crazylegend.kotlinextensions.fragments.viewCoroutineScope
import com.crazylegend.kotlinextensions.views.height
import com.crazylegend.kotlinextensions.views.onItemSelected
import com.crazylegend.kotlinextensions.views.setOnClickListenerCooldown
import com.crazylegend.kotlinextensions.views.width
import com.crazylegend.navigation.navigateSafe
import com.crazylegend.navigation.navigateUpSafe
import com.crazylegend.viewbinding.viewBinding
import com.crazylegend.vigilante.R
import com.crazylegend.vigilante.abstracts.AbstractFragment
import com.crazylegend.vigilante.confirmation.DialogConfirmation
import com.crazylegend.vigilante.contracts.EdgeToEdgeScrolling
import com.crazylegend.vigilante.databinding.FragmentCustomizationBinding
import com.crazylegend.vigilante.home.HomeFragmentDirections
import com.crazylegend.vigilante.service.VigilanteService
import com.crazylegend.vigilante.settings.CAMERA_CUSTOMIZATION_BASE_PREF
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import dagger.hilt.android.AndroidEntryPoint


/**
 * Created by crazy on 11/8/20 to long live and prosper !
 */
@AndroidEntryPoint
class CustomizationFragment : AbstractFragment<FragmentCustomizationBinding>(R.layout.fragment_customization), EdgeToEdgeScrolling {

    override fun edgeToEdgeScrollingContent() {
    }

    companion object {
        private const val COLOR_DOT_STATE = "colorState"
        private const val LAYOUT_STATE = "layoutState"
        private const val SIZE_STATE = "sizeState"
        private const val COLOR_NOTIFICATION_STATE = "colorNotificationState"

        const val COLOR_DOT_PREF_ADDITION = "colorChoice"
        const val COLOR_NOTIFICATION_PREF_ADDITION = "colorChoiceNotification"
        const val SIZE_PREF_ADDITION = "sizeChoice"
        const val POSITION_PREF_ADDITION = "positionChoice"
    }

    override val binding by viewBinding(FragmentCustomizationBinding::bind)

    private val defaultDotColor get() = if (prefBaseName == CAMERA_CUSTOMIZATION_BASE_PREF) prefsProvider.getCameraColorPref else prefsProvider.getMicColorPref
    private val defaultNotificationLEDColor get() = if (prefBaseName == CAMERA_CUSTOMIZATION_BASE_PREF) prefsProvider.getCameraColorPref else prefsProvider.getMicColorPref
    private val defaultSize get() = if (prefBaseName == CAMERA_CUSTOMIZATION_BASE_PREF) prefsProvider.getCameraSizePref else prefsProvider.getMicSizePref
    private val defaultLayoutPosition get() = if (prefBaseName == CAMERA_CUSTOMIZATION_BASE_PREF) prefsProvider.getCameraPositionPref else prefsProvider.getMicPositionPref
    private val title get() = if (prefBaseName == CAMERA_CUSTOMIZATION_BASE_PREF) getString(R.string.camera_title) else getString(R.string.microphone_title)

    private var pickedDotColor: Int? = null
    private var pickedNotificationLEDColor: Int? = null
    private var pickedSize: Float? = null
    private var pickedLayoutPosition: Int? = null


    private val navArgs by navArgs<CustomizationFragmentArgs>()
    private val prefBaseName get() = navArgs.prefBaseName

    private fun updatePreviewColor(newValue: Int) {
        binding.preview.setColorFilter(newValue)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (prefBaseName.isEmpty()) {
            CrashyReporter.logException(IllegalStateException("Argument is missing in customization"))
            findNavController().navigateUpSafe()
        }

        binding.title.text = title

        pickedDotColor = defaultDotColor
        pickedNotificationLEDColor = defaultNotificationLEDColor
        updatePreviewColor(pickedDotColor!!)

        pickedSize = defaultSize
        binding.sizeSlider.value = pickedSize!!
        updatePreviewWidthAndHeight(pickedSize!!)

        pickedLayoutPosition = defaultLayoutPosition
        binding.layoutPosition.setSelection(pickedLayoutPosition!!)

        binding.sizeSlider.addOnChangeListener { _, value, _ ->
            updatePreviewWidthAndHeight(value)
            prefsProvider.saveSizePref(prefBaseName + SIZE_PREF_ADDITION, value)
        }

        binding.colorPick.setOnClickListenerCooldown {
            showDotColorPicker(requireContext())
        }

        binding.colorPickNotificationLed.setOnClickListenerCooldown {
            showNotificationLEDColorPicker(requireContext())
        }

        binding.layoutPosition.onItemSelected { _: AdapterView<*>?, _: View?, position: Int?, _: Long? ->
            pickedLayoutPosition = position
        }


        binding.backButton.root.setOnClickListenerCooldown {
            backButtonClick()
        }

        fragmentBooleanResult(DialogConfirmation.RESULT_KEY, DialogConfirmation.DEFAULT_REQ_KEY, onDenied = {
            findNavController().navigateUpSafe()
        }, onGranted = {
            prefsProvider.saveSizePref(prefBaseName + SIZE_PREF_ADDITION, binding.sizeSlider.value)
            pickedDotColor?.let { prefsProvider.saveColorPref(prefBaseName + COLOR_DOT_PREF_ADDITION, it) }
            pickedNotificationLEDColor?.let { prefsProvider.saveNotificationColorPref(prefBaseName + COLOR_NOTIFICATION_PREF_ADDITION, it) }
            pickedLayoutPosition?.let { prefsProvider.savePositionPref(prefBaseName + POSITION_PREF_ADDITION, it) }
            VigilanteService.serviceParamsListener?.updateForBasePref(prefBaseName)
            goBack()
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            backButtonClick()
        }
    }


    private fun backButtonClick() {
        findNavController().navigateSafe(
                HomeFragmentDirections.destinationConfirmation(
                        cancelButtonText = getString(R.string.discard_changes),
                        confirmationButtonText = getString(R.string.save),
                        titleText = getString(R.string.save_progress_expl)
                )
        )
    }

    private fun goBack() {
        viewCoroutineScope.launchWhenResumed {
            onMain {
                findNavController().navigateUpSafe()
                shortToast(R.string.saved)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pickedDotColor?.let { outState.putInt(COLOR_DOT_STATE, it) }
        pickedLayoutPosition?.let { outState.putInt(LAYOUT_STATE, it) }
        pickedSize?.let { outState.putFloat(SIZE_STATE, it) }
        pickedNotificationLEDColor?.let { outState.putInt(COLOR_NOTIFICATION_STATE, it) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        pickedDotColor = savedInstanceState?.getInt(COLOR_DOT_STATE, defaultDotColor)
                ?: defaultDotColor
        pickedLayoutPosition = savedInstanceState?.getInt(LAYOUT_STATE, defaultLayoutPosition)
                ?: defaultLayoutPosition
        pickedSize = savedInstanceState?.getFloat(SIZE_STATE, defaultSize) ?: defaultSize
        pickedNotificationLEDColor = savedInstanceState?.getInt(COLOR_NOTIFICATION_STATE, defaultNotificationLEDColor)

        updatePreviewColor(pickedDotColor!!)
        updatePreviewWidthAndHeight(pickedSize!!)
        binding.sizeSlider.value = pickedSize!!
        binding.layoutPosition.setSelection(pickedLayoutPosition!!)
    }

    private fun showDotColorPicker(context: Context) {
        ColorPickerDialog.Builder(context).apply {
            setTitle(getString(R.string.pick_dot_color))
            setPreferenceName(prefBaseName + COLOR_DOT_PREF_ADDITION)
            setPositiveButton(getString(R.string.select), ColorEnvelopeListener { envelope, _ -> setDotColor(envelope) })
            setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            setBottomSpace(12)
            show()
        }
    }


    private fun showNotificationLEDColorPicker(context: Context) {
        ColorPickerDialog.Builder(context).apply {
            setTitle(getString(R.string.pick_notification_LED_color))
            setPreferenceName(prefBaseName + COLOR_NOTIFICATION_PREF_ADDITION)
            setPositiveButton(getString(R.string.select), ColorEnvelopeListener { envelope, _ -> setDotColor(envelope) })
            setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            setBottomSpace(12)
            show()
        }
    }

    private fun setNotificationLEDColor(envelope: ColorEnvelope?) {
        envelope ?: return
        pickedNotificationLEDColor = envelope.color
    }

    private fun setDotColor(envelope: ColorEnvelope?) {
        envelope ?: return
        pickedDotColor = envelope.color
        updatePreviewColor(pickedDotColor!!)
    }

    private fun updatePreviewWidthAndHeight(value: Float) {
        binding.preview.width(value / 2)
        binding.preview.height(value / 2)
    }
}