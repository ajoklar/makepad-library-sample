package dev.makepad.android

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

// MARK: Converted this file to Kotlin in order to use composable functions
internal class MakepadSurface(context: Context?) : SurfaceView(context),
    View.OnTouchListener, View.OnKeyListener, ViewTreeObserver.OnGlobalLayoutListener,
    SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        setOnTouchListener(this)
        setOnKeyListener(this)
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i("SAPP", "surfaceCreated")
        val surface = holder.surface
        MakepadNative.surfaceOnSurfaceCreated(surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i("SAPP", "surfaceDestroyed")
        val surface = holder.surface
        MakepadNative.surfaceOnSurfaceDestroyed(surface)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        Log.i("SAPP", "surfaceChanged")
        val surface = holder.surface
        MakepadNative.surfaceOnSurfaceChanged(surface, width, height)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        MakepadNative.surfaceOnTouch(event)
        return true
    }

    override fun onGlobalLayout() {
        val insets = this.rootWindowInsets ?: return
        val r = Rect()
        getWindowVisibleDisplayFrame(r)
        val screenHeight = this.rootView.height
        val visibleHeight = r.height()
        val keyboardHeight = screenHeight - visibleHeight
        MakepadNative.surfaceOnResizeTextIME(
            keyboardHeight,
            insets.isVisible(WindowInsets.Type.ime())
        )
    }

    // docs says getCharacters are deprecated
    // but somehow on non-latyn input all keyCode and all the relevant fields in the KeyEvent are zeros
    // and only getCharacters has some usefull data
    @Suppress("deprecation")
    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode != 0) {
            val metaState = event.metaState
            MakepadNative.surfaceOnKeyDown(keyCode, metaState)
        }
        if (event.action == KeyEvent.ACTION_UP && keyCode != 0) {
            val metaState = event.metaState
            MakepadNative.surfaceOnKeyUp(keyCode, metaState)
        }
        if (event.action == KeyEvent.ACTION_UP || event.action == KeyEvent.ACTION_MULTIPLE) {
            var character = event.unicodeChar
            if (character == 0) {
                val characters = event.characters
                if (characters != null && characters.length >= 0) {
                    character = characters[0].code
                }
            }
            if (character != 0) {
                MakepadNative.surfaceOnCharacter(character)
            }
        }
        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            super.onKeyUp(keyCode, event)
        } else true
    }

    // MARK: commented out because it caused a NullPointerException
    // There is an Android bug when screen is in landscape,
    // the keyboard inset height is reported as 0.
    // This code is a workaround which fixes the bug.
    // See https://groups.google.com/g/android-developers/c/50XcWooqk7I
    // For some reason it only works if placed here and not in the parent layout.
//    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
//        val connection = super.onCreateInputConnection(outAttrs)
//        outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
//        return connection
//    }

    val nativeSurface: Surface
        get() = holder.surface
}

internal class ResizingLayout(context: Context?) : LinearLayout(context),
    View.OnApplyWindowInsetsListener {
    init {
        // When viewing in landscape mode with keyboard shown, there are
        // gaps on both sides so we fill the negative space with black.
        setBackgroundColor(Color.BLACK)
        setOnApplyWindowInsetsListener(this)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        val imeInsets = insets.getInsets(WindowInsets.Type.ime())
        v.setPadding(0, 0, 0, imeInsets.bottom)
        return insets
    }
}

@Composable
fun MakepadView() {
    AndroidView(factory = { ctx ->
        val view = MakepadSurface(ctx)
        MakepadActivity.view = view
        view
    })
}

// MARK: Changed the super type from Activity to ComponentActivity
open class MakepadActivity : ComponentActivity(), MidiManager.OnDeviceOpenedListener {
    private var mHandler: Handler? = null

    // video playback
    private var mVideoPlaybackHandler: Handler? = null
    private var mVideoPlayerRunnables: HashMap<Long, VideoPlayerRunnable>? = null

    // networking
    private var mWebSocketsHandler: Handler? = null
    private val mActiveWebsockets = HashMap<Long, MakepadWebSocket>()
    private val mActiveWebsocketsReaders = HashMap<Long, MakepadWebSocketReader>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // MARK: Removed this as view is instantiated inside the composable MakepadView
//        view = new MakepadSurface(this);
//        // Put it inside a parent layout which can resize it using padding
//        ResizingLayout layout = new ResizingLayout(this);
//        layout.addView(view);
//        setContentView(layout);
        MakepadNative.activityOnCreate(this)
        val decoderThreadHandler = HandlerThread("VideoPlayerThread")
        decoderThreadHandler.start() // TODO: only start this if its needed.
        mVideoPlaybackHandler = Handler(decoderThreadHandler.looper)
        mVideoPlayerRunnables = HashMap()
        val webSocketsThreadHandler = HandlerThread("WebSocketsThread")
        webSocketsThreadHandler.start()
        mWebSocketsHandler = Handler(webSocketsThreadHandler.looper)
        val cache_path = this.cacheDir.absolutePath
        val density = resources.displayMetrics.density
        MakepadNative.onAndroidParams(cache_path, density)

        // Set volume keys to control music stream, we might want make this flexible for app devs
        volumeControlStream = AudioManager.STREAM_MUSIC

        //% MAIN_ACTIVITY_ON_CREATE
    }

    override fun onResume() {
        super.onResume()
        MakepadNative.activityOnResume()

        //% MAIN_ACTIVITY_ON_RESUME
    }

    @Suppress("deprecation")
    override fun onBackPressed() {
        Log.w("SAPP", "onBackPressed")

        // TODO: here is the place to handle request_quit/order_quit/cancel_quit
        super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        MakepadNative.activityOnStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        MakepadNative.activityOnDestroy()
    }

    override fun onPause() {
        super.onPause()
        MakepadNative.activityOnPause()

        //% MAIN_ACTIVITY_ON_PAUSE
    }

    // MARK: commented out because it did nothing and caused warnings
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//        //% MAIN_ACTIVITY_ON_ACTIVITY_RESULT
//    }

    @Suppress("deprecation")
    fun setFullScreen(fullscreen: Boolean) {
        runOnUiThread {
            val decorView = window.decorView
            if (fullscreen) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                if (Build.VERSION.SDK_INT >= 30) {
                    window.setDecorFitsSystemWindows(false)
                } else {
                    val uiOptions =
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    decorView.systemUiVisibility = uiOptions
                }
            } else {
                if (Build.VERSION.SDK_INT >= 30) {
                    window.setDecorFitsSystemWindows(true)
                } else {
                    decorView.systemUiVisibility = 0
                }
            }
        }
    }

    fun showKeyboard(show: Boolean) {
        runOnUiThread {
            if (show) {
                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, 0)
            } else {
                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view!!.windowToken, 0)
            }
        }
    }

    fun requestHttp(
        id: Long,
        metadataId: Long,
        url: String?,
        method: String?,
        headers: String?,
        body: ByteArray?
    ) {
        try {
            val network = MakepadNetwork()
            val future = network.performHttpRequest(url, method, headers, body)
            future.thenAccept { response: HttpResponse ->
                runOnUiThread {
                    MakepadNative.onHttpResponse(
                        id,
                        metadataId,
                        response.statusCode,
                        response.headers,
                        response.body
                    )
                }
            }.exceptionally { ex: Throwable ->
                runOnUiThread {
                    MakepadNative.onHttpRequestError(
                        id,
                        metadataId,
                        ex.toString()
                    )
                }
                null
            }
        } catch (e: Exception) {
            MakepadNative.onHttpRequestError(id, metadataId, e.toString())
        }
    }

    fun openWebSocket(id: Long, url: String?, callback: Long) {
        val webSocket = MakepadWebSocket(id, url, callback)
        mActiveWebsockets[id] = webSocket
        webSocket.connect()
        if (webSocket.isConnected) {
            val reader = MakepadWebSocketReader(this, webSocket)
            mWebSocketsHandler!!.post(reader)
            mActiveWebsocketsReaders[id] = reader
        }
    }

    fun sendWebSocketMessage(id: Long, message: ByteArray?) {
        val webSocket = mActiveWebsockets[id]
        webSocket?.sendMessage(message)
    }

    fun closeWebSocket(id: Long) {
        val reader = mActiveWebsocketsReaders[id]
        if (reader != null) {
            mWebSocketsHandler!!.removeCallbacks(reader)
        }
        mActiveWebsocketsReaders.remove(id)
        mActiveWebsockets.remove(id)
    }

    fun webSocketConnectionDone(id: Long, callback: Long) {
        mActiveWebsockets.remove(id)
        MakepadNative.onWebSocketClosed(callback)
    }

    fun getAudioDevices(flag: Long): Array<String>? {
        return try {
            val am = this.getSystemService(AUDIO_SERVICE) as AudioManager
            var devices: Array<AudioDeviceInfo>? = null
            val out = ArrayList<String>()
            devices = if (flag == 0L) {
                am.getDevices(AudioManager.GET_DEVICES_INPUTS)
            } else {
                am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            }
            for (device in devices) {
                val channel_counts = device.channelCounts
                for (cc in channel_counts) {
                    out.add(
                        String.format(
                            "%d$$%d$$%d$$%s",
                            device.id,
                            device.type,
                            cc,
                            device.productName.toString()
                        )
                    )
                }
            }
            out.toTypedArray<String>()
        } catch (e: Exception) {
            Log.e("Makepad", "exception: " + e.message)
            Log.e("Makepad", "exception: $e")
            null
        }
    }

    @Suppress("deprecation")
    fun openAllMidiDevices(delay: Long) {
        val runnable = Runnable {
            try {
                val bm =
                    this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                val ba = bm.adapter
                val bluetooth_devices =
                    ba.bondedDevices
                val bt_names =
                    ArrayList<String>()
                val mm =
                    this.getSystemService(MIDI_SERVICE) as MidiManager
                for (device in bluetooth_devices) {
                    if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                        val name = device.name
                        bt_names.add(name)
                        mm.openBluetoothDevice(
                            device,
                            this,
                            Handler(Looper.getMainLooper())
                        )
                    }
                }
                // this appears to give you nonworking BLE midi devices. So we skip those by name (not perfect but ok)
                for (info in mm.devices) {
                    val name = info.properties
                        .getCharSequence(MidiDeviceInfo.PROPERTY_NAME)
                        .toString()
                    var found = false
                    for (bt_name in bt_names) {
                        if (bt_name == name) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        mm.openDevice(
                            info,
                            this,
                            Handler(Looper.getMainLooper())
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Makepad", "exception: " + e.message)
                Log.e("Makepad", "exception: $e")
            }
        }
        if (delay != 0L) {
            mHandler!!.postDelayed(runnable, delay)
        } else { // run now
            runnable.run()
        }
    }

    override fun onDeviceOpened(device: MidiDevice) {
        if (device == null) {
            return
        }
        val info = device.info
        if (info != null) {
            val name = info.properties.getCharSequence(MidiDeviceInfo.PROPERTY_NAME).toString()
            MakepadNative.onMidiDeviceOpened(name, device)
        }
    }

    fun prepareVideoPlayback(
        videoId: Long,
        source: Any?,
        externalTextureHandle: Int,
        autoplay: Boolean,
        shouldLoop: Boolean
    ) {
        val VideoPlayer = VideoPlayer(this, videoId)
        VideoPlayer.setSource(source)
        VideoPlayer.setExternalTextureHandle(externalTextureHandle)
        VideoPlayer.setAutoplay(autoplay)
        VideoPlayer.setShouldLoop(shouldLoop)
        val runnable = VideoPlayerRunnable(VideoPlayer)
        mVideoPlayerRunnables!![videoId] = runnable
        mVideoPlaybackHandler!!.post(runnable)
    }

    fun pauseVideoPlayback(videoId: Long) {
        val runnable = mVideoPlayerRunnables!![videoId]
        runnable?.pausePlayback()
    }

    fun resumeVideoPlayback(videoId: Long) {
        val runnable = mVideoPlayerRunnables!![videoId]
        runnable?.resumePlayback()
    }

    fun muteVideoPlayback(videoId: Long) {
        val runnable = mVideoPlayerRunnables!![videoId]
        runnable?.mute()
    }

    fun unmuteVideoPlayback(videoId: Long) {
        val runnable = mVideoPlayerRunnables!![videoId]
        runnable?.unmute()
    }

    fun cleanupVideoPlaybackResources(videoId: Long) {
        val runnable = mVideoPlayerRunnables!!.remove(videoId)
        runnable?.cleanupVideoPlaybackResources()
    }

    companion object {
        // MARK: Made static to be used from the composable MakepadView
        internal var view: MakepadSurface? = null

        init {
            System.loadLibrary("makepadsample")
        }
    }
}
