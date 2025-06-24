package com.TusharGoyal.imgtopdf
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.TusharGoyal.imgtopdf.databinding.ActivityPdfViewBinding
import com.github.barteksc.pdfviewer.PDFView


class PdfViewActivity : AppCompatActivity()  {
    lateinit var binding : ActivityPdfViewBinding
    private var isUserDragging = false
    private var handler = Handler(Looper.getMainLooper())

    lateinit var pageIndicator :TextView
    lateinit var pdfView : PDFView


    private val hideRunnable = Runnable {
        pageIndicator.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pageIndicator = binding.pageIndicator
        pdfView = binding.pdfView


        val pdfUri : Uri? = intent.getParcelableExtra("pdfUri")







        pdfView.fromUri(pdfUri)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .onPageScroll { page, _ ->
                if (!isUserDragging) {
                    updatePageIndicator(page)
                }
            }
            .load()

        enableIndicatorDrag()
    }
    private fun updatePageIndicator(page: Int) {
        val totalPages = pdfView.pageCount
        pageIndicator.text = "${page + 1}/$totalPages"
        pageIndicator.visibility = View.VISIBLE

        if (!isUserDragging) { // Prevent auto-movement during manual dragging
            val newY = ((page.toFloat() / (totalPages - 1)) * (pdfView.height - 200)).coerceIn(100f, (pdfView.height - 200f))
            pageIndicator.y = newY
        }

        resetAutoHideTimer()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableIndicatorDrag() {
        pageIndicator.setOnTouchListener(object : View.OnTouchListener {
            var dy = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dy = v.y - event.rawY
                        isUserDragging = true
                        handler.removeCallbacks(hideRunnable) // it is used to prevent hiding while dragging
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newY = event.rawY + dy
                        v.y = newY.coerceIn(100f, (pdfView.height - 200f))


                        val totalPages = pdfView.pageCount
                        val targetPage = ((newY / pdfView.height) * totalPages).toInt().coerceIn(0, totalPages - 1)
                        updatePageIndicator(targetPage)
                    }
                    MotionEvent.ACTION_UP -> {
                        isUserDragging = false // Resume auto-positioning after release
                        val totalPages = pdfView.pageCount
                        val targetPage = ((v.y / pdfView.height) * totalPages).toInt().coerceIn(0, totalPages - 1)
                        pdfView.jumpTo(targetPage, true)
                        resetAutoHideTimer() // Start auto-hide timer after user stops dragging
                    }
                }
                return true
            }
        })
    }
    private fun resetAutoHideTimer() {
        handler.removeCallbacks(hideRunnable) // Stop previous hide attempts
        handler.postDelayed(hideRunnable, 2000) // Hide after 2 seconds
    }

    }


