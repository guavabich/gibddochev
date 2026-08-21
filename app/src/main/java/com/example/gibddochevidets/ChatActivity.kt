package com.example.gibddochevidets


import android.content.ContentValues
import android.util.LruCache
import android.util.Log
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.gibddochevidets.network.ApiRepository
import com.example.gibddochevidets.network.MessageResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class ChatActivity : Activity() {

    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var chatRoot: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var messagesContainer: LinearLayout

    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView
    private lateinit var backButton: TextView
    private lateinit var attachButton: TextView

    private lateinit var attachmentPanel: LinearLayout
    private lateinit var photoOptions: LinearLayout
    private lateinit var locationOptions: LinearLayout

    private lateinit var photoTab: LinearLayout
    private lateinit var locationTab: LinearLayout

    private lateinit var photoTabTitle: TextView
    private lateinit var locationTabTitle: TextView

    private lateinit var openCamera: TextView
    private lateinit var openGallery: TextView

    private lateinit var currentLocation: TextView
    private lateinit var chooseOnMap: TextView
    private lateinit var shareLocation: TextView

    private lateinit var galleryGrid: LinearLayout

    // ============================================================
    // ATTACHMENT POPUP
    // ============================================================

    private var attachmentPopup: PopupWindow? = null
    private var attachmentPopupRoot: LinearLayout? = null

    private lateinit var popupPhotoTab: LinearLayout
    private lateinit var popupLocationTab: LinearLayout

    // ============================================================
    // NETWORK
    // ============================================================

    private lateinit var repository: ApiRepository

    private val httpClient =
        OkHttpClient.Builder()
            .build()

    // ============================================================
    // COROUTINES
    // ============================================================

    private val activityJob =
        SupervisorJob()

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate +
                    activityJob
        )

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var pollingJob: Job? = null

    // ============================================================
    // STATE
    // ============================================================


    // ============================================================
// SELECTED MEDIA
// ============================================================

    private val selectedMediaUris =
        mutableListOf<Uri>()
    private var cameraPhotoUri:
            Uri? = null

    private var sendSelectedMediaButton:
            TextView? = null

    private var isSending = false

    // ============================================================
// MEDIA CACHE
// ============================================================

    private val mediaBitmapCache =
        object : LruCache<String, Bitmap>(
            30 * 1024 * 1024
        ) {

            override fun sizeOf(
                key: String,
                bitmap: Bitmap
            ): Int {
                return bitmap.byteCount
            }
        }

    private val mediaLoading =
        mutableSetOf<String>()

    private var lastMessagesSignature: String? = null

    private var isAttachmentOpen = false

    private var isLiveLocationActive = false

    private var liveLocationMessageId: String? = null

    private var liveLocationJob: Job? = null

    // ============================================================
    // LOCATION
    // ============================================================

    private lateinit var locationManager: LocationManager

    private var locationListener: LocationListener? = null

    // ============================================================
    // CONSTANTS
    // ============================================================

    private companion object {

        const val REQUEST_GALLERY = 1001
        const val REQUEST_CAMERA = 1002
        const val REQUEST_LOCATION = 1003
        const val REQUEST_MAP_PICKER = 1004
        const val REQUEST_PHOTOS_PERMISSION = 1005

        const val LIVE_LOCATION_DURATION_MS =
            15 * 60 * 1000L

        const val LIVE_LOCATION_INTERVAL_MS =
            5000L
    }

    // ============================================================
    // CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_chat
        )

        repository =
            ApiRepository(
                applicationContext
            )

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        initViews()

        setupSystemInsets()

        setupButtons()

        loadMessages()

        startPolling()
    }

    // ============================================================
    // INIT
    // ============================================================

    private fun initViews() {

        chatRoot =
            findViewById(
                R.id.chatRoot
            )

        chatScroll =
            findViewById(
                R.id.chatScroll
            )

        messagesContainer =
            findViewById(
                R.id.messagesContainer
            )

        messageInput =
            findViewById(
                R.id.messageInput
            )

        sendButton =
            findViewById(
                R.id.sendButton
            )

        backButton =
            findViewById(
                R.id.backButton
            )

        attachButton =
            findViewById(
                R.id.attachButton
            )

        attachmentPanel =
            findViewById(
                R.id.attachmentPanel
            )

        photoOptions =
            findViewById(
                R.id.photoOptions
            )

        locationOptions =
            findViewById(
                R.id.locationOptions
            )

        photoTab =
            findViewById(
                R.id.photoTab
            )

        locationTab =
            findViewById(
                R.id.locationTab
            )

        photoTabTitle =
            findViewById(
                R.id.photoTabTitle
            )

        locationTabTitle =
            findViewById(
                R.id.locationTabTitle
            )

        openCamera =
            findViewById(
                R.id.openCamera
            )

        openGallery =
            findViewById(
                R.id.openGallery
            )

        currentLocation =
            findViewById(
                R.id.currentLocation
            )

        chooseOnMap =
            findViewById(
                R.id.chooseOnMap
            )

        shareLocation =
            findViewById(
                R.id.shareLocation
            )

        galleryGrid =
            findViewById(
                R.id.galleryGrid
            )
    }

    // ============================================================
    // SYSTEM INSETS
    // ============================================================

    private fun setupSystemInsets() {

        chatRoot.setOnApplyWindowInsetsListener {
                view,
                insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsets.Type.systemBars()
                )

            val ime =
                insets.getInsets(
                    WindowInsets.Type.ime()
                )

            val bottom =
                maxOf(
                    systemBars.bottom,
                    ime.bottom
                )

            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                bottom
            )

            insets
        }

        chatRoot.requestApplyInsets()
    }

    // ============================================================
    // BUTTONS
    // ============================================================

    private fun setupButtons() {

        backButton.setOnClickListener {
            finish()
        }

        sendButton.setOnClickListener {
            sendCurrentMessage()
        }

        attachButton.setOnClickListener {
            toggleAttachmentPanel()
        }

        photoTab.setOnClickListener {
            showPhotoOptions()
        }

        locationTab.setOnClickListener {
            showLocationOptions()
        }

        openGallery.setOnClickListener {
            openGallery()
        }

        openCamera.setOnClickListener {
            openCamera()
        }

        currentLocation.setOnClickListener {
            sendCurrentLocation()
        }

        chooseOnMap.setOnClickListener {
            openMapPicker()
        }

        shareLocation.setOnClickListener {
            startLiveLocation()
        }

        messageInput.setOnEditorActionListener {
                _,
                _,
                _ ->

            sendCurrentMessage()

            true
        }
    }

    // ============================================================
    // ATTACHMENT PANEL
    // ============================================================

    private fun toggleAttachmentPanel() {

        if (isAttachmentOpen) {
            closeAttachmentPanel()
        } else {
            showAttachmentPanel()
        }
    }

    private fun showAttachmentPanel() {

        closeAttachmentPanel()

        isAttachmentOpen = true

        messageInput.clearFocus()

        val root =
            createAttachmentPopup()

        attachmentPopupRoot =
            root

        val popupHeight =
            minOf(
                dp(430),
                (
                        resources.displayMetrics.heightPixels *
                                0.58f
                        ).toInt()
            )

        val popup =
            PopupWindow(
                root,
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeight,
                true
            )

        popup.setBackgroundDrawable(
            roundedBackground(
                Color.WHITE,
                dp(22).toFloat()
            )
        )

        popup.isFocusable = true
        popup.isOutsideTouchable = true
        popup.elevation = dp(12).toFloat()

        popup.setOnDismissListener {

            isAttachmentOpen = false

            attachmentPopup = null

            attachmentPopupRoot = null
        }

        attachmentPopup =
            popup

        popup.showAtLocation(
            window.decorView,
            Gravity.BOTTOM,
            0,
            messageComposerBottomOffset()
        )

        showPhotoOptions()
    }

    // ============================================================
    // CLOSE ATTACHMENT
    // ============================================================

    private fun closeAttachmentPanel() {

        isAttachmentOpen = false

        attachmentPopup?.setOnDismissListener(null)

        attachmentPopup?.dismiss()

        attachmentPopup = null

        attachmentPopupRoot = null

        if (::attachmentPanel.isInitialized) {

            attachmentPanel.visibility =
                View.GONE
        }
    }

    // ============================================================
    // POPUP POSITION
    // ============================================================

    private fun messageComposerBottomOffset(): Int {

        val inputHeight =
            if (
                ::messageInput.isInitialized &&
                messageInput.height > 0
            ) {
                messageInput.height
            } else {
                dp(56)
            }

        val bottomPadding =
            if (::chatRoot.isInitialized) {
                chatRoot.paddingBottom
            } else {
                0
            }

        return inputHeight +
                bottomPadding +
                dp(4)
    }

    // ============================================================
    // CREATE POPUP
    // ============================================================

    private fun createAttachmentPopup():
            LinearLayout {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(4)
        )

        root.background =
            roundedBackground(
                Color.WHITE,
                dp(22).toFloat()
            )

        root.clipToOutline = true

        // --------------------------------------------------------
        // HANDLE
        // --------------------------------------------------------

        val handle =
            View(this)

        handle.background =
            roundedBackground(
                Color.rgb(
                    75,
                    75,
                    82
                ),
                dp(3).toFloat()
            )

        root.addView(
            handle,
            LinearLayout.LayoutParams(
                dp(40),
                dp(5)
            ).apply {

                gravity =
                    Gravity.CENTER_HORIZONTAL

                bottomMargin =
                    dp(6)
            }
        )

        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

        val titleRow =
            LinearLayout(this)

        titleRow.orientation =
            LinearLayout.HORIZONTAL

        titleRow.gravity =
            Gravity.CENTER_VERTICAL

        val close =
            TextView(this)

        close.text =
            "×"

        close.textSize =
            30f

        close.gravity =
            Gravity.CENTER

        close.setTextColor(
            Color.rgb(
                55,
                75,
                105
            )
        )

        close.setOnClickListener {
            closeAttachmentPanel()
        }

        titleRow.addView(
            close,
            LinearLayout.LayoutParams(
                dp(46),
                dp(42)
            )
        )

        val title =
            TextView(this)

        title.text =
            "Фото и видео"

        title.textSize =
            19f

        title.typeface =
            Typeface.DEFAULT_BOLD

        title.gravity =
            Gravity.CENTER

        title.setTextColor(
            Color.rgb(
                25,
                45,
                95
            )
        )

        titleRow.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(42),
                1f
            )
        )

        titleRow.addView(
            View(this),
            LinearLayout.LayoutParams(
                dp(46),
                dp(42)
            )
        )

        root.addView(
            titleRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        // --------------------------------------------------------
        // PHOTO
        // --------------------------------------------------------

        photoOptions =
            LinearLayout(this)

        photoOptions.orientation =
            LinearLayout.VERTICAL

        photoOptions.setPadding(
            0,
            dp(2),
            0,
            0
        )

        root.addView(
            photoOptions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // --------------------------------------------------------
        // LOCATION
        // --------------------------------------------------------

        locationOptions =
            createPopupLocationOptions()

        locationOptions.visibility =
            View.GONE

        root.addView(
            locationOptions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // --------------------------------------------------------
        // TABS
        // --------------------------------------------------------

        val tabs =
            LinearLayout(this)

        tabs.orientation =
            LinearLayout.HORIZONTAL

        tabs.gravity =
            Gravity.CENTER

        val galleryTab =
            createPopupTab(
                "▧",
                "Галерея"
            )

        val locationTabView =
            createPopupTab(
                "●",
                "Геопозиция"
            )

        galleryTab.setOnClickListener {
            showPhotoOptions()
        }

        locationTabView.setOnClickListener {
            showLocationOptions()
        }

        tabs.addView(
            galleryTab,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        tabs.addView(
            locationTabView,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        root.addView(
            tabs,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        popupPhotoTab =
            galleryTab

        popupLocationTab =
            locationTabView

        return root
    }

    // ============================================================
    // POPUP TAB
    // ============================================================

    private fun createPopupTab(
        icon: String,
        text: String
    ): LinearLayout {

        val tab =
            LinearLayout(this)

        tab.orientation =
            LinearLayout.VERTICAL

        tab.gravity =
            Gravity.CENTER

        val iconView =
            TextView(this)

        iconView.text =
            icon

        iconView.textSize =
            17f

        iconView.gravity =
            Gravity.CENTER

        iconView.setTextColor(
            Color.rgb(
                80,
                90,
                105
            )
        )

        val textView =
            TextView(this)

        textView.text =
            text

        textView.textSize =
            12f

        textView.gravity =
            Gravity.CENTER

        textView.setTextColor(
            Color.rgb(
                95,
                105,
                120
            )
        )

        tab.addView(
            iconView,
            LinearLayout.LayoutParams(
                -2,
                dp(21)
            )
        )

        tab.addView(
            textView,
            LinearLayout.LayoutParams(
                -2,
                dp(22)
            )
        )

        return tab
    }

    // ============================================================
    // LOCATION PANEL
    // ============================================================

    private fun createPopupLocationOptions():
            LinearLayout {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.gravity =
            Gravity.CENTER

        root.setPadding(
            dp(2),
            dp(4),
            dp(2),
            dp(4)
        )

        fun createLocationButton(
            text: String,
            dark: Boolean,
            action: () -> Unit
        ): TextView {

            val view =
                TextView(this)

            view.text =
                text

            view.textSize =
                16f

            view.typeface =
                Typeface.DEFAULT_BOLD

            view.gravity =
                Gravity.CENTER

            view.setTextColor(
                if (dark) {
                    Color.WHITE
                } else {
                    Color.rgb(
                        25,
                        45,
                        95
                    )
                }
            )

            view.setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8)
            )

            view.background =
                roundedBackground(
                    if (dark) {
                        Color.rgb(
                            25,
                            45,
                            75
                        )
                    } else {
                        Color.rgb(
                            242,
                            245,
                            250
                        )
                    },
                    dp(15).toFloat()
                )

            view.setOnClickListener {
                action()
            }

            return view
        }

        root.addView(
            createLocationButton(
                "📍  Моя геопозиция",
                false
            ) {
                sendCurrentLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                bottomMargin =
                    dp(8)
            }
        )

        root.addView(
            createLocationButton(
                "🗺  Выбрать на карте",
                false
            ) {
                openMapPicker()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                bottomMargin =
                    dp(8)
            }
        )

        root.addView(
            createLocationButton(
                "📡  Передавать геопозицию",
                true
            ) {
                startLiveLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        return root
    }

    // ============================================================
    // BACKGROUND
    // ============================================================

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                radius
        }
    }

    // ============================================================
    // PHOTO TAB
    // ============================================================

    private fun showPhotoOptions() {

        if (attachmentPopupRoot == null) {
            return
        }

        photoOptions.visibility =
            View.VISIBLE

        locationOptions.visibility =
            View.GONE

        buildPopupPhotoPanel()

        if (hasPhotoPermission()) {

            loadGalleryPreview()

        } else {

            requestPhotoPermission()
        }
    }

    // ============================================================
    // PHOTO PANEL
    // ============================================================

    private fun buildPopupPhotoPanel() {

        photoOptions.removeAllViews()

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setPadding(
            0,
            0,
            0,
            dp(4)
        )

        // ========================================================
        // GALLERY SCROLL
        // ========================================================

        val scroll =
            ScrollView(this)

        scroll.isFillViewport =
            true

        scroll.overScrollMode =
            View.OVER_SCROLL_NEVER

        galleryGrid =
            LinearLayout(this)

        galleryGrid.orientation =
            LinearLayout.VERTICAL

        galleryGrid.setPadding(
            0,
            dp(2),
            0,
            dp(2)
        )

        scroll.addView(
            galleryGrid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ========================================================
        // SEND BUTTON
        // ========================================================

        sendSelectedMediaButton =
            TextView(this)

        sendSelectedMediaButton!!.text =
            "Выберите фото"

        sendSelectedMediaButton!!.textSize =
            15f

        sendSelectedMediaButton!!.typeface =
            Typeface.DEFAULT_BOLD

        sendSelectedMediaButton!!.gravity =
            Gravity.CENTER

        sendSelectedMediaButton!!.setTextColor(
            Color.WHITE
        )

        sendSelectedMediaButton!!.background =
            roundedBackground(
                Color.rgb(
                    45,
                    130,
                    220
                ),
                dp(16).toFloat()
            )

        sendSelectedMediaButton!!.isEnabled =
            false

        sendSelectedMediaButton!!.alpha =
            0.5f

        sendSelectedMediaButton!!.setOnClickListener {

            sendSelectedMedia()
        }

        val buttonParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )

        buttonParams.setMargins(
            dp(8),
            dp(6),
            dp(8),
            dp(4)
        )

        container.addView(
            sendSelectedMediaButton,
            buttonParams
        )

        photoOptions.addView(
            container,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        updateSelectedMediaButton()
    }

    // ============================================================
    // LOCATION TAB
    // ============================================================

    private fun showLocationOptions() {

        if (attachmentPopupRoot == null) {
            return
        }

        photoOptions.visibility =
            View.GONE

        locationOptions.visibility =
            View.VISIBLE
    }

    // ============================================================
    // GALLERY
    // ============================================================

    private fun openGallery() {

        try {

            val intent =
                if (Build.VERSION.SDK_INT >= 33) {

                    Intent(
                        MediaStore.ACTION_PICK_IMAGES
                    ).apply {

                        type =
                            "image/*"

                        putExtra(
                            MediaStore.EXTRA_PICK_IMAGES_MAX,
                            10
                        )
                    }

                } else {

                    Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                    ).apply {

                        type =
                            "image/*"

                        putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            true
                        )

                        addCategory(
                            Intent.CATEGORY_OPENABLE
                        )
                    }
                }

            startActivityForResult(
                intent,
                REQUEST_GALLERY
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Галерея недоступна",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
// CAMERA
// ============================================================

    private fun openCamera() {

        if (
            checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ),
                REQUEST_CAMERA
            )

            return
        }

        try {

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        "camera_${System.currentTimeMillis()}.jpg"
                    )

                    put(
                        MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg"
                    )
                }

            cameraPhotoUri =
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

            val uri =
                cameraPhotoUri
                    ?: throw IllegalStateException(
                        "Не удалось создать файл фотографии"
                    )

            val intent =
                Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
                ).apply {

                    putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivityForResult(
                intent,
                REQUEST_CAMERA
            )

        } catch (e: Exception) {

            cameraPhotoUri =
                null

            Toast.makeText(
                this,
                "Камера недоступна: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    // ============================================================
    // PHOTO SELECTED
    // ============================================================

    private fun onPhotoSelected(
        uri: Uri
    ) {

        if (isSending) {
            return
        }

        isSending = true

        sendButton.isEnabled = false
        attachButton.isEnabled = false
        messageInput.isEnabled = false

        Toast.makeText(
            this,
            "Отправляю фото...",
            Toast.LENGTH_SHORT
        ).show()

        scope.launch {

            try {

                val result =
                    withContext(
                        Dispatchers.IO
                    ) {

                        repository.uploadMedia(
                            uri
                        )
                    }

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                val deviceId =
                    repository.getDeviceId()

                addMessage(
                    result,
                    deviceId
                )

                chatScroll.post {

                    chatScroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }

                Toast.makeText(
                    this@ChatActivity,
                    "Фото отправлено",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    Toast.makeText(
                        this@ChatActivity,
                        "Не удалось отправить фото: ${
                            e.message
                                ?: "неизвестная ошибка"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                isSending = false

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    sendButton.isEnabled = true
                    attachButton.isEnabled = true
                    messageInput.isEnabled = true
                }
            }
        }
    }

    // ============================================================
    // ACTIVITY RESULT
    // ============================================================

    @Deprecated(
        "Deprecated in Android API"
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == REQUEST_GALLERY &&
            resultCode == RESULT_OK
        ) {

            val clipData =
                data?.clipData

            if (clipData != null) {

                for (i in 0 until clipData.itemCount) {

                    val uri =
                        clipData
                            .getItemAt(i)
                            .uri

                    if (
                        !selectedMediaUris.contains(uri)
                    ) {

                        selectedMediaUris.add(uri)
                    }
                }

            } else {

                val uri =
                    data?.data

                if (uri != null) {

                    if (
                        !selectedMediaUris.contains(uri)
                    ) {

                        selectedMediaUris.add(uri)
                    }
                }
            }

            showPhotoOptions()

            updateSelectedMediaButton()

            return
        }

        if (
            requestCode == REQUEST_CAMERA &&
            resultCode == RESULT_OK
        ) {

            val uri =
                cameraPhotoUri

            cameraPhotoUri =
                null

            if (uri != null) {

                onPhotoSelected(uri)

            } else {

                Toast.makeText(
                    this,
                    "Не удалось получить фотографию",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }

        if (
            requestCode ==
            REQUEST_MAP_PICKER &&
            resultCode ==
            RESULT_OK
        ) {

            val latitude =
                data?.getDoubleExtra(
                    "latitude",
                    Double.NaN
                )
                    ?: Double.NaN

            val longitude =
                data?.getDoubleExtra(
                    "longitude",
                    Double.NaN
                )
                    ?: Double.NaN

            if (
                latitude.isNaN() ||
                longitude.isNaN()
            ) {
                return
            }

            closeAttachmentPanel()

            sendLocation(
                latitude,
                longitude
            )
        }
    }

    // ============================================================
    // LOAD RECENT PHOTOS
    // ============================================================

    private fun loadGalleryPreview() {

        if (!::galleryGrid.isInitialized) {
            return
        }

        galleryGrid.removeAllViews()

        if (!hasPhotoPermission()) {
            return
        }

        // ========================================================
        // CAMERA — ALWAYS FIRST
        // ========================================================

        addCameraTile()

        scope.launch {

            val photos =
                withContext(Dispatchers.IO) {

                    queryRecentPhotoUris(14)
                }

            if (
                isFinishing ||
                isDestroyed
            ) {
                return@launch
            }

            photos.forEach { uri ->

                addGalleryThumbnail(
                    uri
                )
            }

            if (photos.isEmpty()) {

                val empty =
                    TextView(
                        this@ChatActivity
                    )

                empty.text =
                    "Нет фотографий в галерее"

                empty.textSize =
                    14f

                empty.gravity =
                    Gravity.CENTER

                empty.setTextColor(
                    Color.rgb(
                        125,
                        130,
                        140
                    )
                )

                empty.setPadding(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )

                galleryGrid.addView(
                    empty,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                    )
                )
            }
        }
    }

    // ============================================================
    // QUERY PHOTOS
    // ============================================================

    private fun queryRecentPhotoUris(
        limit: Int
    ): List<Uri> {

        val result =
            mutableListOf<Uri>()

        val projection =
            arrayOf(
                MediaStore.Images.Media._ID
            )

        val collection =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media._ID
                    )

                while (
                    cursor.moveToNext() &&
                    result.size < limit
                ) {

                    val id =
                        cursor.getLong(
                            idColumn
                        )

                    result.add(
                        Uri.withAppendedPath(
                            collection,
                            id.toString()
                        )
                    )
                }
            }

        } catch (
            _: SecurityException
        ) {
        } catch (
            _: Exception
        ) {
        }

        return result
    }

    // ============================================================
    // CAMERA TILE
    // ============================================================

    private fun addCameraTile() {

        addGalleryTile(
            size =
                galleryTileSize(),

            content =
                createCameraView(),

            onClick = {
                openCamera()
            }
        )
    }

    // ============================================================
    // CAMERA VIEW
    // ============================================================

    private fun createCameraView():
            View {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.gravity =
            Gravity.CENTER

        layout.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        layout.background =
            roundedBackground(
                Color.rgb(
                    24,
                    39,
                    65
                ),
                dp(12).toFloat()
            )

        val icon =
            TextView(this)

        icon.text =
            "📷"

        icon.textSize =
            28f

        icon.gravity =
            Gravity.CENTER

        val text =
            TextView(this)

        text.text =
            "Камера"

        text.textSize =
            11f

        text.typeface =
            Typeface.DEFAULT_BOLD

        text.setTextColor(
            Color.WHITE
        )

        text.gravity =
            Gravity.CENTER

        layout.addView(
            icon,
            LinearLayout.LayoutParams(
                -2,
                dp(36)
            )
        )

        layout.addView(
            text,
            LinearLayout.LayoutParams(
                -2,
                dp(20)
            )
        )

        return layout
    }

    // ============================================================
    // PHOTO THUMBNAIL
    // ============================================================

    private fun addGalleryThumbnail(
        uri: Uri
    ) {

        val image =
            ImageView(this)

        image.scaleType =
            ImageView.ScaleType.CENTER_CROP

        image.background =
            roundedBackground(
                Color.rgb(
                    235,
                    235,
                    238
                ),
                dp(12).toFloat()
            )

        image.clipToOutline =
            true
        if (
            selectedMediaUris.contains(uri)
        ) {

            image.background =
                GradientDrawable().apply {

                    setColor(
                        Color.rgb(
                            220,
                            235,
                            255
                        )
                    )

                    setStroke(
                        dp(3),
                        Color.rgb(
                            45,
                            130,
                            220
                        )
                    )

                    cornerRadius =
                        dp(12).toFloat()
                }

            image.alpha =
                0.7f

        } else {

            image.background =
                roundedBackground(
                    Color.rgb(
                        235,
                        235,
                        238
                    ),
                    dp(12).toFloat()
                )

            image.alpha =
                1f
        }

        // ========================================================
        // LOAD PREVIEW
        // ========================================================

        scope.launch {

            val bitmap =
                withContext(Dispatchers.IO) {

                    try {

                        contentResolver
                            .openInputStream(uri)
                            ?.use { input ->

                                BitmapFactory
                                    .decodeStream(input)
                            }

                    } catch (_: Exception) {

                        null
                    }
                }

            if (
                isFinishing ||
                isDestroyed
            ) {
                return@launch
            }

            if (bitmap != null) {

                image.setImageBitmap(
                    bitmap
                )
            }
        }

        // ========================================================
        // SELECT INSTEAD OF SEND
        // ========================================================

        addGalleryTile(
            size =
                galleryTileSize(),

            content =
                image,

            onClick = {

                toggleMediaSelection(
                    uri
                )
            }
        )
    }

    // ============================================================
// TOGGLE MEDIA SELECTION
// ============================================================

    private fun toggleMediaSelection(
        uri: Uri
    ) {

        if (
            selectedMediaUris.contains(uri)
        ) {

            selectedMediaUris.remove(uri)

        } else {

            selectedMediaUris.add(uri)
        }

        updateSelectedMediaButton()

        loadGalleryPreview()
    }


    // ============================================================
// UPDATE SEND BUTTON
// ============================================================

    private fun updateSelectedMediaButton() {

        val button =
            sendSelectedMediaButton
                ?: return

        val count =
            selectedMediaUris.size

        if (count == 0) {

            button.text =
                "Выберите фото"

            button.isEnabled =
                false

            button.alpha =
                0.5f

        } else {

            button.text =
                "Отправить ($count)"

            button.isEnabled =
                true

            button.alpha =
                1f
        }
    }

    // ============================================================
// SEND SELECTED MEDIA
// ============================================================

    private fun sendSelectedMedia() {

        if (selectedMediaUris.isEmpty()) {
            return
        }

        if (isSending) {
            return
        }

        val files =
            selectedMediaUris.toList()

        isSending =
            true

        sendSelectedMediaButton?.isEnabled =
            false

        sendButton.isEnabled =
            false

        attachButton.isEnabled =
            false

        messageInput.isEnabled =
            false

        Toast.makeText(
            this,
            "Отправляю ${files.size} файл(ов)...",
            Toast.LENGTH_SHORT
        ).show()

        scope.launch {

            try {

                for (uri in files) {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@launch
                    }

                    withContext(Dispatchers.IO) {

                        repository.uploadMedia(
                            uri
                        )
                    }
                }

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                selectedMediaUris.clear()

                closeAttachmentPanel()

                val messages =
                    withContext(Dispatchers.IO) {

                        repository.getMessages(null)
                    }

                renderMessages(
                    messages,
                    true
                )

                Toast.makeText(
                    this@ChatActivity,
                    "Файлы отправлены",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    Toast.makeText(
                        this@ChatActivity,
                        "Не удалось отправить файл: ${
                            e.message ?: "неизвестная ошибка"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                isSending =
                    false

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    sendButton.isEnabled =
                        true

                    attachButton.isEnabled =
                        true

                    messageInput.isEnabled =
                        true

                    updateSelectedMediaButton()
                }
            }
        }
    }

    // ============================================================
    // PHOTO TILE
    // ============================================================

    private fun addGalleryTile(
        size: Int,
        content: View,
        onClick: () -> Unit
    ) {

        val tile =
            FrameLayout(this)

        tile.background =
            roundedBackground(
                Color.rgb(
                    245,
                    245,
                    247
                ),
                dp(12).toFloat()
            )

        tile.clipToOutline =
            true

        tile.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        tile.setOnClickListener {
            onClick()
        }

        val params =
            LinearLayout.LayoutParams(
                size,
                size
            ).apply {

                setMargins(
                    dp(2),
                    dp(2),
                    dp(2),
                    dp(2)
                )
            }

        var row =
            galleryGrid.getChildAt(
                galleryGrid.childCount - 1
            ) as? LinearLayout

        if (
            row == null ||
            row.childCount >= 3
        ) {

            row =
                LinearLayout(this)

            row.orientation =
                LinearLayout.HORIZONTAL

            row.gravity =
                Gravity.START

            galleryGrid.addView(
                row,
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        row.addView(
            tile,
            params
        )
    }

    // ============================================================
    // TILE SIZE
    // ============================================================

    private fun galleryTileSize(): Int {

        val width =
            resources
                .displayMetrics
                .widthPixels

        return (
                (
                        width -
                                dp(20) -
                                dp(12)
                        ) / 3
                ).coerceAtLeast(
                dp(70)
            )
    }

    // ============================================================
    // PHOTO PERMISSION
    // ============================================================

    private fun hasPhotoPermission():
            Boolean {

        return when {

            Build.VERSION.SDK_INT >= 34 -> {

                checkSelfPermission(
                    Manifest.permission.READ_MEDIA_IMAGES
                ) ==
                        PackageManager.PERMISSION_GRANTED ||

                        checkSelfPermission(
                            Manifest.permission
                                .READ_MEDIA_VISUAL_USER_SELECTED
                        ) ==
                        PackageManager.PERMISSION_GRANTED
            }

            Build.VERSION.SDK_INT >= 33 -> {

                checkSelfPermission(
                    Manifest.permission.READ_MEDIA_IMAGES
                ) ==
                        PackageManager.PERMISSION_GRANTED
            }

            else -> {

                checkSelfPermission(
                    Manifest.permission
                        .READ_EXTERNAL_STORAGE
                ) ==
                        PackageManager.PERMISSION_GRANTED
            }
        }
    }

    // ============================================================
    // REQUEST PHOTO PERMISSION
    // ============================================================

    private fun requestPhotoPermission() {

        if (Build.VERSION.SDK_INT >= 34) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission
                        .READ_MEDIA_VISUAL_USER_SELECTED
                ),
                REQUEST_PHOTOS_PERMISSION
            )

        } else if (
            Build.VERSION.SDK_INT >= 33
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                ),
                REQUEST_PHOTOS_PERMISSION
            )

        } else {

            requestPermissions(
                arrayOf(
                    Manifest.permission
                        .READ_EXTERNAL_STORAGE
                ),
                REQUEST_PHOTOS_PERMISSION
            )
        }
    }

    // ============================================================
    // LOCATION PERMISSION
    // ============================================================

    private fun hasLocationPermission():
            Boolean {

        return checkSelfPermission(
            Manifest.permission
                .ACCESS_FINE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED ||

                checkSelfPermission(
                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {

        requestPermissions(
            arrayOf(
                Manifest.permission
                    .ACCESS_FINE_LOCATION,

                Manifest.permission
                    .ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION
        )
    }

    // ============================================================
// CURRENT LOCATION
// ============================================================

    private fun sendCurrentLocation() {

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        closeAttachmentPanel()

        getLocationAndSend()
    }

    // ============================================================
// GET LOCATION AND SEND
// ============================================================

    private fun getLocationAndSend() {

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        var bestLocation: Location? = null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        // ========================================================
        // СНАЧАЛА ПРОБУЕМ ПОСЛЕДНЮЮ ИЗВЕСТНУЮ КООРДИНАТУ
        // ========================================================

        for (provider in providers) {

            try {

                if (
                    !locationManager.isProviderEnabled(
                        provider
                    )
                ) {
                    continue
                }

                val location =
                    locationManager.getLastKnownLocation(
                        provider
                    )

                if (location != null) {

                    if (
                        bestLocation == null ||
                        location.time > bestLocation!!.time
                    ) {

                        bestLocation =
                            location
                    }
                }

            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        // ========================================================
        // ЕСЛИ КООРДИНАТА УЖЕ ЕСТЬ — ОТПРАВЛЯЕМ
        // ========================================================

        if (bestLocation != null) {

            sendLocation(
                bestLocation!!.latitude,
                bestLocation!!.longitude
            )

            return
        }

        // ========================================================
        // ИНАЧЕ ПОЛУЧАЕМ НОВУЮ
        // ========================================================

        Toast.makeText(
            this,
            "Получаю местоположение...",
            Toast.LENGTH_SHORT
        ).show()

        requestOneLocation()
    }

    // ============================================================
// ONE LOCATION REQUEST
// ============================================================

    private fun requestOneLocation() {

        if (!hasLocationPermission()) {
            return
        }

        // Если старый listener ещё существует —
        // сначала удаляем его.

        locationListener?.let {

            try {

                locationManager.removeUpdates(it)

            } catch (_: Exception) {
            }
        }

        locationListener = null

        var delivered = false

        val listener =
            object : LocationListener {

                override fun onLocationChanged(
                    location: Location
                ) {

                    if (delivered) {
                        return
                    }

                    delivered = true

                    try {

                        locationManager.removeUpdates(
                            this
                        )

                    } catch (_: Exception) {
                    }

                    if (
                        locationListener === this
                    ) {

                        locationListener = null
                    }

                    handler.removeCallbacksAndMessages(
                        this
                    )

                    if (
                        !isFinishing &&
                        !isDestroyed
                    ) {

                        sendLocation(
                            location.latitude,
                            location.longitude
                        )
                    }
                }

                override fun onProviderDisabled(
                    provider: String
                ) {
                    // Ничего не делаем.
                    // Второй provider может продолжить работать.
                }

                override fun onProviderEnabled(
                    provider: String
                ) {
                    // Ничего не делаем.
                }
            }

        locationListener = listener

        var requested = false

        // ========================================================
        // GPS
        // ========================================================

        try {

            if (
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )

                requested = true
            }

        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        // ========================================================
        // NETWORK
        // ========================================================

        try {

            if (
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )

                requested = true
            }

        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        // ========================================================
        // НИ ОДИН PROVIDER НЕ РАБОТАЕТ
        // ========================================================

        if (!requested) {

            locationListener = null

            Toast.makeText(
                this,
                "Не удалось определить местоположение. Включи GPS и геолокацию.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ========================================================
        // TIMEOUT
        // ========================================================

        handler.postDelayed(
            {

                if (
                    delivered ||
                    locationListener !== listener
                ) {
                    return@postDelayed
                }

                delivered = true

                try {

                    locationManager.removeUpdates(
                        listener
                    )

                } catch (_: Exception) {
                }

                locationListener = null

                // ====================================================
                // ПОСЛЕДНЯЯ ПОПЫТКА ВЗЯТЬ LAST KNOWN
                // ====================================================

                var fallback: Location? = null

                val providers =
                    listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER
                    )

                for (provider in providers) {

                    try {

                        val location =
                            locationManager.getLastKnownLocation(
                                provider
                            )

                        if (location != null) {

                            if (
                                fallback == null ||
                                location.time > fallback!!.time
                            ) {

                                fallback = location
                            }
                        }

                    } catch (_: SecurityException) {
                    } catch (_: Exception) {
                    }
                }

                if (fallback != null) {

                    sendLocation(
                        fallback!!.latitude,
                        fallback!!.longitude
                    )

                } else {

                    Toast.makeText(
                        this,
                        "Не удалось получить местоположение. Проверь, включена ли геолокация.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            },
            15_000L
        )
    }

    // ============================================================
    // SEND STATIC LOCATION
    // ============================================================

    private fun sendLocation(
        latitude: Double,
        longitude: Double
    ) {

        if (isSending) {
            return
        }

        isSending = true

        sendButton.isEnabled =
            false

        attachButton.isEnabled =
            false

        scope.launch {

            try {

                withContext(
                    Dispatchers.IO
                ) {

                    repository
                        .sendStaticLocation(
                            latitude,
                            longitude
                        )
                }

                val messages =
                    withContext(
                        Dispatchers.IO
                    ) {

                        repository
                            .getMessages(null)
                    }

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                renderMessages(
                    messages,
                    true
                )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    Toast.makeText(
                        this@ChatActivity,
                        getErrorMessage(e),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                isSending =
                    false

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    sendButton.isEnabled =
                        true

                    attachButton.isEnabled =
                        true
                }
            }
        }
    }

    // ============================================================
    // MAP
    // ============================================================

    private fun openMapPicker() {

        try {

            val intent =
                Intent(
                    this,
                    MapPickerActivity::class.java
                )

            startActivityForResult(
                intent,
                REQUEST_MAP_PICKER
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Не удалось открыть карту: ${
                    e.message
                }",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // LIVE LOCATION
    // ============================================================

    private fun startLiveLocation() {

        if (isLiveLocationActive) {

            Toast.makeText(
                this,
                "Передача геопозиции уже запущена",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        closeAttachmentPanel()

        scope.launch {

            try {

                val message =
                    withContext(
                        Dispatchers.IO
                    ) {

                        repository
                            .startLiveLocation()
                    }

                liveLocationMessageId =
                    message.message_id

                // ========================================================
// СРАЗУ ПОКАЗЫВАЕМ LIVE LOCATION В ЧАТЕ
// ========================================================

                val currentDeviceId =
                    repository.getDeviceId()

                addMessage(
                    message,
                    currentDeviceId
                )

                chatScroll.post {

                    chatScroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }

                isLiveLocationActive =
                    true

                Toast.makeText(
                    this@ChatActivity,
                    "Геопозиция передаётся 15 минут",
                    Toast.LENGTH_LONG
                ).show()

                startLiveLocationUpdates(
                    message.message_id
                )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this@ChatActivity,
                    getErrorMessage(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // LIVE LOCATION UPDATES
    // ============================================================

    private fun startLiveLocationUpdates(
        messageId: String
    ) {

        liveLocationJob?.cancel()

        liveLocationJob =
            scope.launch {

                val startedAt =
                    System.currentTimeMillis()

                while (
                    isLiveLocationActive &&
                    System.currentTimeMillis() -
                    startedAt <
                    LIVE_LOCATION_DURATION_MS
                ) {

                    sendCurrentLivePoint(
                        messageId
                    )

                    delay(
                        LIVE_LOCATION_INTERVAL_MS
                    )
                }

                if (
                    isLiveLocationActive &&
                    liveLocationMessageId ==
                    messageId
                ) {

                    stopLiveLocationInternal(
                        messageId
                    )
                }
            }
    }

    // ============================================================
    // SEND LIVE POINT
    // ============================================================

    private suspend fun sendCurrentLivePoint(
        messageId: String
    ) {

        if (!hasLocationPermission()) {
            return
        }

        var location:
                Location? = null

        val providers =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

        for (provider in providers) {

            try {

                val candidate =
                    locationManager
                        .getLastKnownLocation(
                            provider
                        )

                if (
                    candidate != null &&
                    (
                            location == null ||
                                    candidate.time >
                                    location!!.time
                            )
                ) {

                    location =
                        candidate
                }

            } catch (
                _: SecurityException
            ) {
            }
        }

        val current =
            location ?: return

        try {

            withContext(
                Dispatchers.IO
            ) {

                repository
                    .sendLiveLocationPoint(
                        messageId =
                            messageId,

                        latitude =
                            current.latitude,

                        longitude =
                            current.longitude
                    )
            }

        } catch (
            _: Exception
        ) {
        }
    }

    // ============================================================
    // STOP LIVE LOCATION
    // ============================================================

    private suspend fun stopLiveLocationInternal(
        messageId: String
    ) {

        try {

            withContext(
                Dispatchers.IO
            ) {

                repository
                    .stopLiveLocation(
                        messageId
                    )
            }

        } catch (
            _: Exception
        ) {
        }

        isLiveLocationActive =
            false

        liveLocationMessageId =
            null

        liveLocationJob =
            null

        if (
            !isFinishing &&
            !isDestroyed
        ) {

            Toast.makeText(
                this@ChatActivity,
                "Передача геопозиции завершена",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
// PERMISSIONS
// ============================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        // ========================================================
        // LOCATION
        // ========================================================

        if (
            requestCode == REQUEST_LOCATION
        ) {

            val granted =
                grantResults.any {
                    it ==
                            PackageManager.PERMISSION_GRANTED
                }

            if (granted) {

                // СРАЗУ продолжаем действие,
                // которое пользователь запросил.

                getLocationAndSend()

            } else {

                Toast.makeText(
                    this,
                    "Разрешение на геолокацию не предоставлено",
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        // ========================================================
        // PHOTOS
        // ========================================================

        if (
            requestCode == REQUEST_PHOTOS_PERMISSION
        ) {

            if (
                hasPhotoPermission()
            ) {

                if (
                    attachmentPopupRoot != null
                ) {

                    buildPopupPhotoPanel()

                    loadGalleryPreview()
                }

            } else {

                Toast.makeText(
                    this,
                    "Разреши доступ к фотографиям, чтобы показать галерею",
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        // ========================================================
        // CAMERA
        // ========================================================

        if (
            requestCode == REQUEST_CAMERA
        ) {

            val granted =
                grantResults.any {
                    it ==
                            PackageManager.PERMISSION_GRANTED
                }

            if (granted) {

                openCamera()

            } else {

                Toast.makeText(
                    this,
                    "Разрешение на камеру не предоставлено",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }
    }

    // ============================================================
    // LOAD MESSAGES
    // ============================================================

    private fun loadMessages() {

        scope.launch {

            try {

                val messages =
                    withContext(
                        Dispatchers.IO
                    ) {

                        repository
                            .getMessages(null)
                    }

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                renderMessages(
                    messages,
                    true
                )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                Toast.makeText(
                    this@ChatActivity,
                    getErrorMessage(e),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ============================================================
    // POLLING
    // ============================================================

    private fun startPolling() {

        pollingJob?.cancel()

        pollingJob =
            scope.launch {

                while (true) {

                    delay(
                        3000L
                    )

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        break
                    }

                    loadNewMessages()
                }
            }
    }

    private suspend fun loadNewMessages() {

        if (isSending) {
            return
        }

        try {

            val messages =
                withContext(
                    Dispatchers.IO
                ) {

                    repository
                        .getMessages(null)
                }

            if (
                isFinishing ||
                isDestroyed
            ) {
                return
            }

            renderMessages(
                messages,
                false
            )

        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            _: Exception
        ) {
        }
    }

    // ============================================================
// RENDER MESSAGES
// ============================================================

    private fun renderMessages(
        messages: List<MessageResponse>,
        scrollToBottom: Boolean
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        // ========================================================
        // СОЗДАЁМ СИГНАТУРУ СООБЩЕНИЙ
        //
        // Если с сервера пришёл тот же самый список,
        // ничего не перерисовываем.
        // Это главное против мигания фотографий.
        // ========================================================

        val signature =
            messages.joinToString("|") { message ->

                buildString {

                    append(message.message_id)
                    append(":")
                    append(message.created_at)
                    append(":")
                    append(message.text)
                    append(":")
                    append(message.message_type)
                    append(":")
                    append(message.delivered_at)
                }
            }

        if (
            signature == lastMessagesSignature &&
            messagesContainer.childCount > 0
        ) {

            // Сообщения не изменились.
            // НИЧЕГО не удаляем и не создаём заново.

            if (scrollToBottom) {

                handler.postDelayed({

                    if (
                        !isFinishing &&
                        !isDestroyed
                    ) {

                        chatScroll.fullScroll(
                            ScrollView.FOCUS_DOWN
                        )
                    }

                }, 50L)
            }

            return
        }

        lastMessagesSignature =
            signature

        // ========================================================
        // ПОЛНАЯ ПЕРЕРИСОВКА НУЖНА ТОЛЬКО ЕСЛИ
        // СПИСОК СООБЩЕНИЙ ДЕЙСТВИТЕЛЬНО ИЗМЕНИЛСЯ
        // ========================================================

        messagesContainer.removeAllViews()

        var previousDate: String? = null

        val currentDeviceId =
            repository.getDeviceId()

        for (message in messages) {

            val dateKey =
                getDateKey(
                    message.created_at
                )

            if (
                !dateKey.isNullOrEmpty() &&
                dateKey != previousDate
            ) {

                addDateSeparator(
                    message.created_at
                )

                previousDate =
                    dateKey
            }

            addMessage(
                message,
                currentDeviceId
            )
        }

        // ========================================================
        // SCROLL
        // ========================================================

        if (scrollToBottom) {

            handler.postDelayed({

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    chatScroll.fullScroll(
                        ScrollView.FOCUS_DOWN
                    )
                }

            }, 100L)
        }
    }

    // ============================================================
    // DATE SEPARATOR
    // ============================================================

    private fun addDateSeparator(
        createdAt: String?
    ) {

        val date =
            parseServerDate(
                createdAt
            ) ?: return

        val dateView =
            TextView(this)

        dateView.text =
            formatDate(date)

        dateView.textSize =
            13f

        dateView.setTextColor(
            Color.rgb(
                105,
                125,
                145
            )
        )

        dateView.typeface =
            Typeface.create(
                "sans",
                Typeface.BOLD
            )

        dateView.gravity =
            Gravity.CENTER

        dateView.setPadding(
            dp(14),
            dp(6),
            dp(14),
            dp(6)
        )

        val background =
            GradientDrawable()

        background.setColor(
            Color.rgb(
                225,
                235,
                244
            )
        )

        background.cornerRadius =
            dp(18).toFloat()

        dateView.background =
            background

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.gravity =
            Gravity.CENTER_HORIZONTAL

        params.topMargin =
            dp(8)

        params.bottomMargin =
            dp(8)

        messagesContainer.addView(
            dateView,
            params
        )
    }

    // ============================================================
    // MESSAGE
    // ============================================================

    private fun addMessage(
        message: MessageResponse,
        currentDeviceId: String?
    ) {

        val isMine =
            !currentDeviceId.isNullOrBlank() &&
                    message.observer_device_id ==
                    currentDeviceId

        val wrapper =
            LinearLayout(this)

        wrapper.orientation =
            LinearLayout.VERTICAL

        wrapper.gravity =
            if (isMine) {
                Gravity.END
            } else {
                Gravity.START
            }

        val bubble =
            LinearLayout(this)

        bubble.orientation =
            LinearLayout.VERTICAL

        bubble.setPadding(
            dp(13),
            dp(9),
            dp(10),
            dp(6)
        )

        val background =
            GradientDrawable()

        if (isMine) {

            background.setColor(
                Color.rgb(
                    224,
                    237,
                    255
                )
            )

            background.cornerRadii =
                floatArrayOf(
                    dp(18).toFloat(),
                    dp(18).toFloat(),

                    dp(5).toFloat(),
                    dp(5).toFloat(),

                    dp(18).toFloat(),
                    dp(18).toFloat(),

                    dp(18).toFloat(),
                    dp(18).toFloat()
                )

        } else {

            background.setColor(
                Color.WHITE
            )

            background.cornerRadii =
                floatArrayOf(
                    dp(5).toFloat(),
                    dp(5).toFloat(),

                    dp(18).toFloat(),
                    dp(18).toFloat(),

                    dp(18).toFloat(),
                    dp(18).toFloat(),

                    dp(18).toFloat(),
                    dp(18).toFloat()
                )
        }

        bubble.background =
            background

        // ========================================================
// MESSAGE CONTENT
// ========================================================

        val messageType =
            message.message_type
                ?.uppercase(Locale.ROOT)

        when {

            // ====================================================
            // STATIC LOCATION
            // ====================================================

            message.static_location != null &&
                    (
                            messageType == "STATIC_LOCATION" ||
                                    messageType == "LOCATION" ||
                                    messageType == "GEOLOCATION"
                            ) -> {

                addLocationBubbleContent(
                    bubble,
                    message
                )
            }

            // ====================================================
            // LIVE LOCATION
            // ====================================================

            messageType == "LIVE_LOCATION" ||
                    messageType == "LIVE_GEOLOCATION" ||
                    messageType == "LIVE_LOCATION_START" -> {

                addLiveLocationBubbleContent(
                    bubble,
                    message
                )
            }

            // ====================================================
            // MEDIA
            // ====================================================

            message.media != null -> {

                addMediaBubbleContent(
                    bubble,
                    message
                )
            }

            // ====================================================
            // TEXT
            // ====================================================

            else -> {

                val text =
                    TextView(this)

                text.text =
                    message.text ?: ""

                text.textSize =
                    16f

                text.setTextColor(
                    Color.rgb(
                        32,
                        32,
                        32
                    )
                )

                text.setLineSpacing(
                    0f,
                    1.05f
                )

                bubble.addView(
                    text
                )
            }
        }
        // ========================================================
        // META
        // ========================================================

        val meta =
            LinearLayout(this)

        meta.orientation =
            LinearLayout.HORIZONTAL

        meta.gravity =
            Gravity.CENTER_VERTICAL

        val time =
            TextView(this)

        val parsedDate =
            parseServerDate(
                message.created_at
            )

        time.text =
            if (parsedDate != null) {
                formatTime(parsedDate)
            } else {
                ""
            }

        time.textSize =
            11f

        time.setTextColor(
            Color.rgb(
                120,
                130,
                140
            )
        )

        time.includeFontPadding =
            false

        meta.addView(
            time
        )

        if (isMine) {

            val status =
                TextView(this)

            status.text =
                if (
                    message.delivered_at != null
                ) {
                    "  ✓✓"
                } else {
                    "  ✓"
                }

            status.textSize =
                12f

            status.typeface =
                Typeface.DEFAULT_BOLD

            status.includeFontPadding =
                false

            status.setTextColor(
                if (
                    message.delivered_at != null
                ) {
                    Color.rgb(
                        23,
                        100,
                        200
                    )
                } else {
                    Color.rgb(
                        120,
                        130,
                        140
                    )
                }
            )

            meta.addView(
                status
            )
        }

        val metaParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(18)
            )

        metaParams.gravity =
            Gravity.END

        metaParams.topMargin =
            dp(3)

        bubble.addView(
            meta,
            metaParams
        )

        // ========================================================
        // BUBBLE SIZE
        // ========================================================

        val bubbleParams =
            LinearLayout.LayoutParams(
                (
                        resources.displayMetrics.widthPixels *
                                0.78f
                        ).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        bubbleParams.gravity =
            if (isMine) {
                Gravity.END
            } else {
                Gravity.START
            }

        bubbleParams.topMargin =
            dp(3)

        bubbleParams.bottomMargin =
            dp(3)

        wrapper.addView(
            bubble,
            bubbleParams
        )

        val wrapperParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        wrapperParams.topMargin =
            dp(1)

        wrapperParams.bottomMargin =
            dp(1)

        messagesContainer.addView(
            wrapper,
            wrapperParams
        )
    }

    // ============================================================
    // MEDIA MESSAGE
    // ============================================================

    // ============================================================
// MEDIA MESSAGE
// ============================================================

    private fun addMediaBubbleContent(
        bubble: LinearLayout,
        message: MessageResponse
    ) {

        val media =
            message.media
                ?: return

        val messageId =
            message.message_id

        if (messageId.isBlank()) {
            return
        }

        val imageView =
            ImageView(this)

        imageView.scaleType =
            ImageView.ScaleType.CENTER_CROP

        imageView.adjustViewBounds =
            true

        // ========================================================
        // IMAGE SIZE
        // ========================================================

        val imageParams =
            LinearLayout.LayoutParams(
                dp(250),
                dp(250)
            )

        imageParams.gravity =
            Gravity.CENTER

        imageView.layoutParams =
            imageParams

        // ========================================================
        // ROUNDED CORNERS
        // ========================================================

        val imageBackground =
            GradientDrawable()

        imageBackground.setColor(
            Color.TRANSPARENT
        )

        imageBackground.cornerRadius =
            dp(16).toFloat()

        imageView.background =
            imageBackground

        imageView.clipToOutline =
            true

        // ========================================================
        // ПРОВЕРЯЕМ КЭШ
        // ========================================================

        val cachedBitmap =
            mediaBitmapCache.get(
                messageId
            )

        if (cachedBitmap != null) {

            // ====================================================
            // ФОТО УЖЕ ЕСТЬ В ПАМЯТИ
            //
            // Никакого placeholder.
            // Никакой загрузки.
            // Никакого мигания.
            // ====================================================

            imageView.setImageBitmap(
                cachedBitmap
            )

        } else {

            // ====================================================
            // ТОЛЬКО ПЕРВАЯ ЗАГРУЗКА
            // ====================================================

            imageView.setImageResource(
                android.R.drawable.ic_menu_gallery
            )

            loadMediaIntoImageView(
                messageId,
                imageView
            )
        }

        bubble.addView(
            imageView
        )

        // ========================================================
        // OPEN FULLSCREEN
        // ========================================================

        imageView.setOnClickListener {

            openMediaFullscreen(
                messageId
            )
        }
    }

    // ============================================================
// LOAD MEDIA INTO IMAGE VIEW
// ============================================================

    private fun loadMediaIntoImageView(
        messageId: String,
        imageView: ImageView
    ) {

        // Если это фото уже кто-то загружает —
        // второй запрос не создаём.

        if (
            mediaLoading.contains(
                messageId
            )
        ) {
            return
        }

        mediaLoading.add(
            messageId
        )

        scope.launch {

            try {

                val bitmap =
                    loadMediaBitmap(
                        messageId
                    )

                if (
                    bitmap != null
                ) {

                    // =================================================
                    // СОХРАНЯЕМ В КЭШ
                    // =================================================

                    mediaBitmapCache.put(
                        messageId,
                        bitmap
                    )

                    if (
                        !isFinishing &&
                        !isDestroyed
                    ) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {

            } finally {

                mediaLoading.remove(
                    messageId
                )
            }
        }
    }

    // ============================================================
    // LOAD MEDIA BITMAP
    // ============================================================

    private suspend fun loadMediaBitmap(
        messageId: String
    ): Bitmap? {

        return try {

            Log.d(
                "MEDIA_DEBUG",
                "Начинаю загрузку: $messageId"
            )

            val bytes =
                withContext(
                    Dispatchers.IO
                ) {

                    repository
                        .downloadMedia(
                            messageId
                        )
                }

            Log.d(
                "MEDIA_DEBUG",
                "Получено байт: ${bytes.size}"
            )

            if (bytes.isEmpty()) {

                Log.e(
                    "MEDIA_DEBUG",
                    "Сервер вернул пустой файл"
                )

                null

            } else {

                BitmapFactory
                    .decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                "MEDIA_DEBUG",
                "Ошибка загрузки MEDIA",
                e
            )

            null
        }
    }

    // ============================================================
    // FULLSCREEN MEDIA
    // ============================================================

    private fun openMediaFullscreen(
        messageId: String
    ) {

        val dialog =
            Dialog(this)

        val imageView =
            ImageView(this)

        imageView.setBackgroundColor(
            Color.BLACK
        )

        imageView.scaleType =
            ImageView.ScaleType.FIT_CENTER

        dialog.setContentView(
            imageView
        )

        dialog.setOnShowListener {

            dialog.window?.setBackgroundDrawable(
                ColorDrawable(
                    Color.BLACK
                )
            )

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        scope.launch {

            val bitmap =
                loadMediaBitmap(
                    messageId
                )

            if (
                bitmap != null &&
                !isFinishing &&
                !isDestroyed
            ) {

                imageView.setImageBitmap(
                    bitmap
                )
            }
        }
    }

    // ============================================================
    // LOCATION BUBBLE
    // ============================================================

    private fun addLocationBubbleContent(
        bubble: LinearLayout,
        message: MessageResponse
    ) {

        val location =
            message.static_location
                ?: return

        val locationContainer =
            LinearLayout(this)

        locationContainer.orientation =
            LinearLayout.VERTICAL

        locationContainer.gravity =
            Gravity.CENTER_HORIZONTAL

        val icon =
            TextView(this)

        icon.text =
            "⌖"

        icon.textSize =
            38f

        icon.gravity =
            Gravity.CENTER

        icon.setTextColor(
            Color.WHITE
        )

        val iconBackground =
            GradientDrawable()

        iconBackground.setColor(
            Color.rgb(
                45,
                130,
                220
            )
        )

        iconBackground.shape =
            GradientDrawable.OVAL

        icon.background =
            iconBackground

        val iconParams =
            LinearLayout.LayoutParams(
                dp(68),
                dp(68)
            )

        locationContainer.addView(
            icon,
            iconParams
        )

        val title =
            TextView(this)

        title.text =
            "Местоположение"

        title.textSize =
            15f

        title.typeface =
            Typeface.DEFAULT_BOLD

        title.setTextColor(
            Color.rgb(
                35,
                35,
                35
            )
        )

        title.gravity =
            Gravity.CENTER

        val titleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        titleParams.topMargin =
            dp(8)

        locationContainer.addView(
            title,
            titleParams
        )

        val coordinates =
            TextView(this)

        coordinates.text =
            String.format(
                Locale.US,
                "%.6f, %.6f",
                location.latitude,
                location.longitude
            )

        coordinates.textSize =
            12f

        coordinates.setTextColor(
            Color.rgb(
                110,
                120,
                130
            )
        )

        coordinates.gravity =
            Gravity.CENTER

        locationContainer.addView(
            coordinates
        )

        bubble.addView(
            locationContainer
        )

        locationContainer.setOnClickListener {

            val uri =
                Uri.parse(
                    "geo:${location.latitude},${location.longitude}" +
                            "?q=${location.latitude},${location.longitude}"
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )

            try {

                startActivity(
                    intent
                )

            } catch (
                _: Exception
            ) {

                Toast.makeText(
                    this,
                    "На устройстве нет приложения карт",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ============================================================
// LIVE LOCATION BUBBLE
// ============================================================

    private fun addLiveLocationBubbleContent(
        bubble: LinearLayout,
        message: MessageResponse
    ) {

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.gravity =
            Gravity.CENTER_HORIZONTAL

        container.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        // ========================================================
        // ИКОНКА
        // ========================================================

        val icon =
            TextView(this)

        icon.text =
            "📡"

        icon.textSize =
            34f

        icon.gravity =
            Gravity.CENTER

        icon.setTextColor(
            Color.WHITE
        )

        val iconBackground =
            GradientDrawable()

        iconBackground.setColor(
            Color.rgb(
                45,
                130,
                220
            )
        )

        iconBackground.shape =
            GradientDrawable.OVAL

        icon.background =
            iconBackground

        container.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(68),
                dp(68)
            )
        )

        // ========================================================
        // TITLE
        // ========================================================

        val title =
            TextView(this)

        title.text =
            "Передача геопозиции"

        title.textSize =
            15f

        title.typeface =
            Typeface.DEFAULT_BOLD

        title.setTextColor(
            Color.rgb(
                35,
                35,
                35
            )
        )

        title.gravity =
            Gravity.CENTER

        val titleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        titleParams.topMargin =
            dp(8)

        container.addView(
            title,
            titleParams
        )

        // ========================================================
        // STATUS
        // ========================================================

        val status =
            TextView(this)

        status.text =
            if (isLiveLocationActive) {
                "● Геопозиция передаётся"
            } else {
                "Передача завершена"
            }

        status.textSize =
            12f

        status.setTextColor(
            if (isLiveLocationActive) {
                Color.rgb(
                    45,
                    150,
                    90
                )
            } else {
                Color.rgb(
                    110,
                    120,
                    130
                )
            }
        )

        status.gravity =
            Gravity.CENTER

        val statusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        statusParams.topMargin =
            dp(4)

        container.addView(
            status,
            statusParams
        )

        // ========================================================
        // DESCRIPTION
        // ========================================================

        val description =
            TextView(this)

        description.text =
            "Передача в течение 15 минут"

        description.textSize =
            11f

        description.setTextColor(
            Color.rgb(
                125,
                130,
                140
            )
        )

        description.gravity =
            Gravity.CENTER

        val descriptionParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        descriptionParams.topMargin =
            dp(2)

        container.addView(
            description,
            descriptionParams
        )

        bubble.addView(
            container
        )
    }

    // ============================================================
    // SEND TEXT
    // ============================================================

    private fun sendCurrentMessage() {

        val text =
            messageInput.text
                .toString()
                .trim()

        if (text.isEmpty()) {
            return
        }

        if (isSending) {
            return
        }

        isSending =
            true

        sendButton.isEnabled =
            false

        attachButton.isEnabled =
            false

        messageInput.isEnabled =
            false

        scope.launch {

            try {

                repository.sendMessage(
                    text
                )

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@launch
                }

                messageInput.text.clear()

                val messages =
                    withContext(
                        Dispatchers.IO
                    ) {

                        repository
                            .getMessages(null)
                    }

                renderMessages(
                    messages,
                    true
                )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    Toast.makeText(
                        this@ChatActivity,
                        getErrorMessage(e),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                isSending =
                    false

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    sendButton.isEnabled =
                        true

                    attachButton.isEnabled =
                        true

                    messageInput.isEnabled =
                        true
                }
            }
        }
    }

    // ============================================================
    // DATE PARSER
    // ============================================================

    private fun parseServerDate(
        value: String?
    ): ZonedDateTime? {

        if (value.isNullOrBlank()) {
            return null
        }

        val clean =
            value.trim()

        try {

            return Instant.parse(
                clean
            ).atZone(
                ZoneId.systemDefault()
            )

        } catch (
            _: Exception
        ) {
        }

        try {

            return OffsetDateTime
                .parse(clean)
                .atZoneSameInstant(
                    ZoneId.systemDefault()
                )

        } catch (
            _: Exception
        ) {
        }

        try {

            val normalized =
                clean.replace(
                    " ",
                    "T"
                )

            return OffsetDateTime
                .parse(normalized)
                .atZoneSameInstant(
                    ZoneId.systemDefault()
                )

        } catch (
            _: Exception
        ) {
        }

        val localFormats =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSSSSS",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss"
            )

        for (pattern in localFormats) {

            try {

                val formatter =
                    DateTimeFormatter
                        .ofPattern(
                            pattern,
                            Locale.US
                        )

                val localDateTime =
                    java.time.LocalDateTime
                        .parse(
                            clean,
                            formatter
                        )

                return localDateTime
                    .atZone(
                        ZoneId.of("UTC")
                    )
                    .withZoneSameInstant(
                        ZoneId.systemDefault()
                    )

            } catch (
                _: DateTimeParseException
            ) {
            }
        }

        try {

            val date =
                LocalDate.parse(
                    clean
                )

            return date.atStartOfDay(
                ZoneId.systemDefault()
            )

        } catch (
            _: Exception
        ) {
        }

        return null
    }

    // ============================================================
    // TIME
    // ============================================================

    private fun formatTime(
        date: ZonedDateTime
    ): String {

        return date.format(
            DateTimeFormatter.ofPattern(
                "HH:mm",
                Locale.getDefault()
            )
        )
    }

    // ============================================================
    // DATE
    // ============================================================

    private fun formatDate(
        date: ZonedDateTime
    ): String {

        val today =
            LocalDate.now(
                ZoneId.systemDefault()
            )

        val messageDate =
            date.toLocalDate()

        return when {

            messageDate == today ->
                "Сегодня"

            messageDate ==
                    today.minusDays(1) ->
                "Вчера"

            else ->
                date.format(
                    DateTimeFormatter.ofPattern(
                        "d MMMM yyyy",
                        Locale(
                            "ru",
                            "RU"
                        )
                    )
                )
        }
    }

    // ============================================================
    // DATE KEY
    // ============================================================

    private fun getDateKey(
        value: String?
    ): String? {

        val date =
            parseServerDate(
                value
            ) ?: return null

        return date
            .toLocalDate()
            .toString()
    }

    // ============================================================
    // ERROR
    // ============================================================

    private fun getErrorMessage(
        throwable: Throwable
    ): String {

        return throwable.message
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Ошибка соединения с сервером"
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }

    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        attachmentPopup?.dismiss()

        attachmentPopup =
            null

        pollingJob?.cancel()

        liveLocationJob?.cancel()

        isLiveLocationActive =
            false

        try {

            locationListener?.let {

                locationManager
                    .removeUpdates(it)
            }

        } catch (
            _: Exception
        ) {
        }

        locationListener =
            null

        handler.removeCallbacksAndMessages(
            null
        )
        locationListener?.let {

            try {

                locationManager.removeUpdates(
                    it
                )

            } catch (_: Exception) {
            }
        }

        locationListener = null
        scope.cancel()

        super.onDestroy()
    }
}

