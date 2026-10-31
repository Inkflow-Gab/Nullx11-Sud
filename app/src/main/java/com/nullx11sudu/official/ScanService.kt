package com.nullx11sudu.official

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.*

class ScanService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private var projection: MediaProjection? = null
    private var bubble: View? = null
    private var guide: BoardView? = null
    private var panel: View? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private val channel = "capture"
    private var board = IntArray(81)
    private var answer: IntArray? = null
    private var selected = -1
    private var infoLabel: TextView? = null
    private var message = "Move the grid over the puzzle, resize it, then scan."
    private var size = 0.82f
    private var centerX = 0.5f
    private var centerY = 0.5f

    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(channel, "Screen scan", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 1, Intent(this, ScanService::class.java).setAction("stop"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channel).setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("Nullx11Sudo is ready").setContentText("Tap bubble to align. Stop to end capture.")
            .setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        if (projection == null) {
            @Suppress("DEPRECATION")
            val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("captureData", Intent::class.java) else intent?.getParcelableExtra("captureData")
            if (data == null) { stopSelf(); return START_NOT_STICKY }
            projection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection((intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED), data)
            projection?.registerCallback(object : MediaProjection.Callback() { override fun onStop() { handler.post { stopSelf() } } }, handler)
            showBubble()
        }
        return START_NOT_STICKY
    }
    private fun params(width: Int, height: Int) = WindowManager.LayoutParams(width, height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        android.graphics.PixelFormat.TRANSLUCENT)
    private fun showBubble() {
        if (bubble != null) return
        val button = TextView(this).apply {
            text = "✦"; textSize = 30f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(0xFF176B66.toInt()); elevation = 10f
            setOnClickListener { hideBubble(); showGuide() }
        }
        val p = params(56.dp, 56.dp).apply { gravity = Gravity.TOP or Gravity.START; x = 12.dp; y = 160.dp }
        button.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0; var startY = 0; var downX = 0f; var downY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { startX = p.x; startY = p.y; downX = event.rawX; downY = event.rawY; return true }
                    MotionEvent.ACTION_MOVE -> { p.x = startX + (event.rawX - downX).toInt(); p.y = startY + (event.rawY - downY).toInt(); wm.updateViewLayout(v, p); return true }
                    MotionEvent.ACTION_UP -> { if (hypot(event.rawX - downX, event.rawY - downY) < 12.dp) v.performClick(); return true }
                }; return false
            }
        })
        bubble = button; wm.addView(button, p)
    }
    private fun hideBubble() { bubble?.let { wm.removeView(it) }; bubble = null }
    private fun showGuide() {
        if (guide != null) return
        val view = BoardView(this)
        guide = view; wm.addView(view, params(-1, -1))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16.dp, 10.dp, 16.dp, 10.dp)
            setBackgroundColor(0xF010191B.toInt())
        }
        val info = TextView(this).apply { text = message; setTextColor(Color.WHITE); textSize = 14f }
        controls.addView(info)
        infoLabel = info
        controls.addView(SeekBar(this).apply {
            max = 75; progress = ((size - 0.25f) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { size = 0.25f + progress / 100f; view.invalidate() }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        val actions = LinearLayout(this)
        fun action(label: String, click: () -> Unit) { actions.addView(Button(this).apply { text = label; textSize = 12f; setOnClickListener { click() } }, LinearLayout.LayoutParams(0, -2, 1f)) }
        action("Scan") { message = "Scanning…"; info.text = message; capture() }
        action("Clear") { board = IntArray(81); answer = null; message = "Cleared. Scan again."; info.text = message; view.invalidate() }
        action("Hide") { closeGuide(); showBubble() }
        controls.addView(actions)
        val digits = LinearLayout(this)
        for (digit in 0..9) {
            digits.addView(TextView(this).apply {
                text = if (digit == 0) "×" else digit.toString()
                textSize = 20f; gravity = Gravity.CENTER; setTextColor(0xFF80CBC4.toInt())
                setOnClickListener {
                    if (selected >= 0) {
                        board[selected] = digit
                        updateSolution()
                        view.invalidate()
                    } else { message = "Tap a cell to select it first."; info.text = message }
                }
            }, LinearLayout.LayoutParams(0, 48.dp, 1f))
        }
        controls.addView(digits)
        panel = controls
        wm.addView(controls, params(-1, -2).apply { gravity = Gravity.BOTTOM })
    }
    private fun closeGuide() { infoLabel = null; panel?.let { wm.removeView(it) }; panel = null; guide?.let { wm.removeView(it) }; guide = null }
    private fun boardRect(w: Int, h: Int): RectF {
        val side = min(w, h) * size
        val cx = centerX * w; val cy = centerY * h
        return RectF(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2)
    }
    private inner class BoardView(context: Context) : View(context) {
        private val paint = Paint(3)
        private var downX = 0f; private var downY = 0f; private var dragging = false
        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(0x44000000)
            val rect = boardRect(width, height); val cell = rect.width() / 9
            paint.style = Paint.Style.FILL; paint.color = 0xDD10191B.toInt(); canvas.drawRect(rect, paint)
            for (i in 0..9) {
                paint.color = if (i % 3 == 0) 0xFF80CBC4.toInt() else 0x667EAEA9
                paint.strokeWidth = if (i % 3 == 0) 2.dp.toFloat() else 1f
                canvas.drawLine(rect.left + i * cell, rect.top, rect.left + i * cell, rect.bottom, paint)
                canvas.drawLine(rect.left, rect.top + i * cell, rect.right, rect.top + i * cell, paint)
            }
            paint.textAlign = Paint.Align.CENTER; paint.textSize = cell * 0.65f
            if (selected >= 0) {
                paint.color = 0x664DB6AC
                canvas.drawRect(rect.left + (selected % 9) * cell, rect.top + (selected / 9) * cell,
                    rect.left + (selected % 9 + 1) * cell, rect.top + (selected / 9 + 1) * cell, paint)
            }
            for (i in 0 until 81) {
                val v = if (board[i] != 0) board[i] else answer?.get(i) ?: 0
                if (v == 0) continue
                paint.color = if (board[i] != 0) Color.WHITE else 0xFF80CBC4.toInt()
                canvas.drawText(v.toString(), rect.left + (i % 9 + 0.5f) * cell, rect.top + (i / 9 + 0.72f) * cell, paint)
            }
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.dp.toFloat(); paint.color = 0xFF80CBC4.toInt()
            canvas.drawCircle(rect.right, rect.bottom, 9.dp.toFloat(), paint); paint.style = Paint.Style.FILL
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; dragging = false; return true }
                MotionEvent.ACTION_MOVE -> {
                    if (hypot(event.x - downX, event.y - downY) < 5.dp) return true
                    dragging = true
                    centerX = (centerX + (event.x - downX) / width).coerceIn(0f, 1f)
                    centerY = (centerY + (event.y - downY) / height).coerceIn(0f, 1f)
                    downX = event.x; downY = event.y; invalidate(); return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        val rect = boardRect(width, height)
                        if (rect.contains(event.x, event.y)) {
                            val col = ((event.x - rect.left) / rect.width() * 9).toInt().coerceIn(0, 8)
                            val row = ((event.y - rect.top) / rect.height() * 9).toInt().coerceIn(0, 8)
                            selected = row * 9 + col; invalidate()
                        }
                    }
                    return true
                }
            }; return true
        }
    }
    private fun capture() {
        val proj = projection ?: return
        val view = guide ?: return
        val rect = boardRect(view.width, view.height)
        val viewWidth = view.width; val viewHeight = view.height
        closeGuide(); hideBubble()
        handler.postDelayed({
            try {
                val metrics = resources.displayMetrics
                val width = metrics.widthPixels; val height = metrics.heightPixels
                val density = metrics.densityDpi
                val imageReader = reader ?: ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { reader = it }
                // Discard any queued frames from the previous scan while the overlay was visible.
                while (true) {
                    val stale = imageReader.acquireLatestImage() ?: break
                    stale.close()
                }
                var received = false
                imageReader.setOnImageAvailableListener({ source ->
                    val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                    received = true
                    source.setOnImageAvailableListener(null, null)
                    val bitmap = try {
                        val plane = image.planes[0]
                        val padded = Bitmap.createBitmap(width + (plane.rowStride - plane.pixelStride * width) / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                        padded.copyPixelsFromBuffer(plane.buffer)
                        Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
                    } finally { image.close() }
                    handler.post { recognize(bitmap, rect, viewWidth, viewHeight) }
                }, handler)
                if (display == null) display = proj.createVirtualDisplay("Sudoku screenshot", width, height, density, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, handler)
                handler.postDelayed({ if (!received && reader === imageReader) { imageReader.setOnImageAvailableListener(null, null); fail("Capture timed out. Try again.") } }, 4000)
            } catch (e: Exception) { fail("Capture unavailable. Restart the session.") }
        }, 350)
    }
    private fun recognize(bitmap: Bitmap, selection: RectF, viewWidth: Int, viewHeight: Int) {
        // OCR only the selected puzzle, not timers, scores, notes, ads or controls nearby.
        // The overlay may have a different viewport from the captured display (status/navigation bars).
        val dx = (bitmap.width - viewWidth) / 2f
        val dy = (bitmap.height - viewHeight) / 2f
        val bounds = RectF(selection).apply { offset(dx, dy) }
        val left = floor(bounds.left).toInt().coerceIn(0, bitmap.width - 1)
        val top = floor(bounds.top).toInt().coerceIn(0, bitmap.height - 1)
        val right = ceil(bounds.right).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = ceil(bounds.bottom).toInt().coerceIn(top + 1, bitmap.height)
        val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        if (crop !== bitmap) bitmap.recycle()
        val scanner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        scanner.process(InputImage.fromBitmap(crop, 0)).addOnSuccessListener { result ->
            val detected = IntArray(81)
            val cellWidth = crop.width / 9f; val cellHeight = crop.height / 9f
            for (block in result.textBlocks) for (line in block.lines) for (element in line.elements) {
                val text = element.text.trim()
                val box = element.boundingBox ?: continue
                if (text.length != 1 || text[0] !in '1'..'9') continue
                val col = (box.exactCenterX() / cellWidth).toInt().coerceIn(0, 8)
                val row = (box.exactCenterY() / cellHeight).toInt().coerceIn(0, 8)
                // Reject OCR spanning a grid line: it is unlikely to be a single clue.
                if (box.width() > cellWidth * 0.85f || box.height() > cellHeight * 0.85f) continue
                detected[row * 9 + col] = text[0].digitToInt()
            }
            board = detected; selected = -1; updateSolution()
            showGuide()
        }.addOnFailureListener { fail("Text recognition failed. Try again.") }
            .addOnCompleteListener { crop.recycle(); scanner.close() }
    }
    private fun updateSolution() {
        val clues = board.count { it != 0 }
        val result = if (clues < 17) Sudoku.Result.Ambiguous else Sudoku.analyze(board)
        answer = (result as? Sudoku.Result.Unique)?.board
        message = when (result) {
            is Sudoku.Result.Unique -> "$clues clues. Unique solution in teal; verify white clues."
            Sudoku.Result.Ambiguous -> "$clues clues: multiple solutions. Correct a clue or rescan."
            Sudoku.Result.Invalid -> "$clues clues: conflicting or unsolvable. Correct a clue or rescan."
        }
        infoLabel?.text = message
    }
    private fun fail(reason: String) { message = reason; showGuide() }
    override fun onDestroy() {
        closeGuide(); hideBubble(); display?.release(); reader?.close(); projection?.stop(); super.onDestroy()
    }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
