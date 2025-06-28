package com.TusharGoyal.imgtopdf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.TusharGoyal.imgtopdf.Adapters.pdfAdapter
import com.TusharGoyal.imgtopdf.Models.pdfModel
import com.TusharGoyal.imgtopdf.databinding.ActivityMainBinding
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import java.io.File

class MainActivity : AppCompatActivity(), IUnityAdsInitializationListener {


    private val GAME_ID = "XXXXXXX" // Replace with your actual Game ID
    private val TEST_MODE = false // Set to false for production

    private lateinit var pdfList:ArrayList<pdfModel>
    private lateinit var pdfAdapter: pdfAdapter
    lateinit var binding: ActivityMainBinding
    private val selectedImages = ArrayList<Uri>()
    private var doubleBackToExitPressesOnce = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UnityAds.initialize(this, GAME_ID, TEST_MODE, this)



      //  UnityAds.initialize(applicationContext,UNITY_GAME_ID,isTestMode)







        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadPdfList()


        binding.fabCreatePdf.setOnClickListener {
            openImagePicker()
        }




        setSupportActionBar(binding.toolbar)





    }


    // (for using our own menu)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_images, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.imageItemPdf) {
            openPdfPicker()
            return true
        } else
            return super.onOptionsItemSelected(item)
    }
    //(code is till here of using our own menu)


    private fun openPdfPicker() {
        pickPdfLauncher.launch("application/pdf")
    }

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if(uri!=null) {
                val intent = Intent(this, PdfViewActivity::class.java)
                intent.putExtra("pdfUri", uri)
                startActivity(intent)
            }
        }


    // Register activity result for selecting images
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if(uris.isNotEmpty()) {

            selectedImages.addAll(uris)
            val intent = Intent(this, PDFPreviewActivity::class.java)
            intent.putParcelableArrayListExtra("imageUris", selectedImages)
            startActivity(intent)
            selectedImages.clear()
        }

    }


    private fun openImagePicker() {
        pickImagesLauncher.launch("image/*")
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

    private fun loadPdfList(){
        pdfList = ArrayList<pdfModel>()
        pdfAdapter = pdfAdapter(this,pdfList)


        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = pdfAdapter


        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDF Maker & Reader")
        if(folder.exists()){
            val files = folder.listFiles() // it may return null  if folder exist but is empty that's why we have to check
            if (files != null) {
                for (file in files) {
                    val uri = Uri.fromFile(file)
                    val pdfmodel = pdfModel(file, uri)
                    pdfList.add(pdfmodel)
                    pdfAdapter.notifyItemInserted(pdfList.size)
                    binding.tvCreateFirstpdf.visibility = View.GONE
                }
            } else {
                // Optionally handle case where there are no files or an error occurred
                binding.tvCreateFirstpdf.visibility = View.VISIBLE
            }

        }

    }

    override fun onInitializationComplete() {
        Log.d("UnityAds", "Unity Ads initialization complete.")
    }

    override fun onInitializationFailed(p0: UnityAds.UnityAdsInitializationError?, p1: String?) {
        Log.e("UnityAds", "Unity Ads initialization failed: ")
    }





}