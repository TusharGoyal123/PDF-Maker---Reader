package com.TusharGoyal.imgtopdf

import android.app.Dialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.TusharGoyal.imgtopdf.databinding.ActivityPdfpreviewBinding
import com.github.barteksc.pdfviewer.PDFView
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

class PDFPreviewActivity : AppCompatActivity() {



    private val AD_UNIT_ID_REWARDED = "Rewarded_Android"

    private var doubleBackToExitPressesOnce = false

    lateinit var binding: ActivityPdfpreviewBinding
    var customProgressBar: Dialog? = null

    private lateinit var pdfPreview: PDFView
    private lateinit var btnSavePdf: Button
    private lateinit var btnAddImages: Button
    private lateinit var imageUris: ArrayList<Uri>
    private var pdfFile: File? = null
    private var pdfFileReturned: File? = null

    private var pdfName:String=""



    private val rewardedLoadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(adUnitId: String) {
            Log.d("UnityAds", "Rewarded Ad Loaded: $adUnitId")
            //  showRewardedButton.isEnabled = true
        }

        override fun onUnityAdsFailedToLoad(adUnitId: String, error: UnityAds.UnityAdsLoadError, message: String) {
            Log.e("UnityAds", "Rewarded Ad Failed to Load: $adUnitId - ${error.name} - $message")
            //  showRewardedButton.isEnabled = false
        }
    }

    private val rewardedShowListener = object : IUnityAdsShowListener {
        override fun onUnityAdsShowFailure(adUnitId: String, error: UnityAds.UnityAdsShowError, message: String) {
            Log.e("UnityAds", "Rewarded Ad Show Failure: $adUnitId - ${error.name} - $message")
        }

        override fun onUnityAdsShowStart(adUnitId: String) {
            Log.d("UnityAds", "Rewarded Ad Show Start: $adUnitId")
        }

        override fun onUnityAdsShowClick(adUnitId: String) {
            Log.d("UnityAds", "Rewarded Ad Click: $adUnitId")
        }

        override fun onUnityAdsShowComplete(adUnitId: String, finishState: UnityAds.UnityAdsShowCompletionState) {
            Log.d("UnityAds", "Rewarded Ad Show Complete: $adUnitId - $finishState")
            if (finishState == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                // Reward the user for watching the rewarded ad
                Log.d("UnityAds", "User watched the rewarded ad completely. Grant reward!")
                // Implement your reward logic here
                savePdfToMediaStore(pdfFileReturned!!,pdfName)
                startActivity(Intent(this@PDFPreviewActivity,MainActivity::class.java))
            } else {
                Log.d("UnityAds", "User did not watch the rewarded ad completely.")
            }
            loadRewardedAd() // Load another rewarded ad after it's shown
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadRewardedAd()

        binding=ActivityPdfpreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        pdfPreview = binding.pdfPreview
        btnSavePdf = binding.btnSavePdf
        btnAddImages = binding.btnAddMoreImg



        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.materialToolbar.setNavigationOnClickListener {
            onBackPressed()
        }


        imageUris = intent.getParcelableArrayListExtra("imageUris")!!

        if (imageUris.isNotEmpty()) {
            showProgressBar()
            generatingPdfPreviewInBackground()

        } else {
            Toast.makeText(this, "No images selected!", Toast.LENGTH_LONG).show()
            finish()
        }

        btnAddImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        btnSavePdf.setOnClickListener {
            pdfFileReturned?.let { showRenameDialog(it) }
        }
    }

    private fun generatingPdfPreviewInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            pdfFileReturned = generatePdfPreview(imageUris)

            withContext(Dispatchers.Main) {
                cancelProgressbar()
                if (pdfFileReturned != null) {
                    pdfPreview.fromFile(pdfFileReturned)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .load()

                } else {
                    Toast.makeText(
                        this@PDFPreviewActivity,
                        "Failed to generate Pdf!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris.addAll(uris)
            showProgressBar()
            generatingPdfPreviewInBackground()
        }

    }


    private fun generatePdfPreview(uriList: ArrayList<Uri>): File? {
        showProgressBar()
        try {
            val file = File(getExternalFilesDir(null), "preview.pdf")
            val pdfDocument = PdfDocument()

            for ((i,uri) in uriList.withIndex()) {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                val pageInfo = PdfDocument.PageInfo.Builder(1150, 1600, i+1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.color = Color.WHITE


//to get the original image size
                val origWidth = bitmap.width
                val origHeight = bitmap.height

// to get the target pdf page size
                val pageWidth = pageInfo.pageWidth
                val pageHeight = pageInfo.pageHeight

// Calculate the scale to fit while maintaining aspect ratio
                val scale = min(
                    pageWidth.toFloat() / origWidth,
                    pageHeight.toFloat() / origHeight
                )

// after scaling
                val newWidth = (origWidth * scale).toInt()
                val newHeight = (origHeight * scale).toInt()

// for centering the image on the page
                val xOffset = (pageWidth - newWidth) / 2f
                val yOffset = (pageHeight - newHeight) / 2f

// Scaling the bitmap
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

                canvas.drawBitmap(scaledBitmap, xOffset, yOffset, paint)

                pdfDocument.finishPage(page)
                bitmap.recycle()
                scaledBitmap.recycle()
            }

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            pdfFile = file


        } catch (e: Exception) {
            Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return pdfFile
    }

    private fun savePdfToMediaStore(pdfFile: File, pdfName: String) {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                pdfName
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"${Environment.DIRECTORY_DOCUMENTS}/PDF Maker & Reader")
        }

        val contentResolver = this.contentResolver
        val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error creating PDF file", Toast.LENGTH_SHORT).show()
        }
        imageUris.clear()
    }

    private fun showRenameDialog(pdfFile: File) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.rename_dialog)
        val btnDone = dialog.findViewById<Button>(R.id.btn_Done)
        val etRename = dialog.findViewById<EditText>(R.id.et_rename)
        val cancel = dialog.findViewById<Button>(R.id.btn_cancel)
        dialog.setCancelable(false)

        val defaultNameOfPdf = "imageToPdf_${System.currentTimeMillis()}"
        etRename.setText(defaultNameOfPdf)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()

        btnDone.setOnClickListener {
            dialog.dismiss()
            if (etRename.text.isNotEmpty()) {
                pdfName=etRename.text.toString()
                showRewardPopup()
            }
            else {
                Toast.makeText(this, "Please give a name to save the file!", Toast.LENGTH_SHORT)
                    .show()
            }


        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun showProgressBar() {

        if (customProgressBar == null) {

            customProgressBar = Dialog(this)
            customProgressBar?.setContentView(R.layout.progress_bar)
            customProgressBar?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            customProgressBar?.setCancelable(false)
            customProgressBar?.show()

        }
    }

    private fun cancelProgressbar() {
        if (customProgressBar != null) {
            customProgressBar!!.dismiss()
            customProgressBar = null
        }
    }


    fun doubleBackToExit(){
        if(doubleBackToExitPressesOnce){
            super.onBackPressed()
            return
        }

        doubleBackToExitPressesOnce = true

        Toast.makeText(this,"Please click back again to exit!",Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressesOnce = false
        },2000)
    }


    override fun onBackPressed() {
        doubleBackToExit()
    }


    private fun showRewardPopup() {
        val dialog=AlertDialog.Builder(this)
        dialog.setIcon(R.drawable.pdf_logo)
            .setMessage("Watch an add to create the PDF")
            .setPositiveButton("Watch Now") { _, _ ->
              //  showAd()
                showRewardedAd()
            }
            .setNegativeButton("Cancel") { _, _ ->
            }.show()
    }

    private fun loadRewardedAd() {
        UnityAds.load(AD_UNIT_ID_REWARDED, rewardedLoadListener)
    }

    private fun showRewardedAd() {
        UnityAds.show(this, AD_UNIT_ID_REWARDED, UnityAdsShowOptions(), rewardedShowListener)
    }
}