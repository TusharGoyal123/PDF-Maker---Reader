package com.TusharGoyal.imgtopdf.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TusharGoyal.imgtopdf.Models.pdfModel
import com.TusharGoyal.imgtopdf.PdfViewActivity
import com.TusharGoyal.imgtopdf.constants
import com.TusharGoyal.imgtopdf.databinding.PdfListRvBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class pdfAdapter(val context : Context, val pdfList : ArrayList<pdfModel>): RecyclerView.Adapter<pdfAdapter.pdfHolder>(){





    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): pdfHolder {
        val binding = PdfListRvBinding.inflate(LayoutInflater.from(context),parent,false)
        return pdfHolder(binding)
    }

    override fun getItemCount(): Int {
       return pdfList.size
    }

    override fun onBindViewHolder(holder: pdfHolder, position: Int) {
       val modelPdf = pdfList[position]
        val name = modelPdf.file.name
        val timeStamp = modelPdf.file.lastModified()
        val formattedDate = constants.formatTimeStamp(timeStamp)

        loadThumbnailFromPdf(modelPdf,holder)
        loadPdfSize(modelPdf,holder)
        holder.binding.tvPdfName.text = name
        holder.binding.tvDate.text = formattedDate

        holder.binding.card.setOnClickListener {
            openPDFFile(modelPdf,holder)
        }


    }

    private fun loadThumbnailFromPdf(modelPdf : pdfModel,holder: pdfHolder){


        try {
            var thumbnailBitamp : Bitmap?=null
            var pageCount=0
            CoroutineScope(Dispatchers.IO).launch {

                val parcelFileDescriptor = ParcelFileDescriptor.open(modelPdf.file,ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(parcelFileDescriptor)
                pageCount = pdfRenderer.pageCount
               val firstPage = pdfRenderer.openPage(0)

                thumbnailBitamp = Bitmap.createBitmap(firstPage.width,firstPage.height,Bitmap.Config.ARGB_8888)

                //for rendering the page onto the bitmap
                firstPage.render(thumbnailBitamp!!,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)


                withContext(Dispatchers.Main){
                    Glide.with(context).load(thumbnailBitamp).into(holder.binding.ivThumbnail)
                    holder.binding.tvPdfPages.text = "$pageCount Pages"
                }


            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }


    }

    private fun loadPdfSize(modelPdf : pdfModel,holder: pdfHolder){
        val bytes:Double = modelPdf.file.length().toDouble()
        val kb = bytes/1024
        val mb =kb/1024

        var size=""
        if(mb>=1){
            size=String.format("%.2f",mb)+" MB"
        }
        else if(kb>=1){
            size=String.format("%.2f",kb)+" KB"
        }
       else{
            size=String.format("%.2f",bytes)+" Bytes"
        }

        holder.binding.tvPdfSize.text = size
    }

    private fun openPDFFile(modelPdf: pdfModel, holder: pdfHolder) {
        val intent = Intent(context,PdfViewActivity::class.java)
        intent.putExtra("pdfUri",modelPdf.uri)
        context.startActivity(intent)

    }

    inner class pdfHolder(val binding: PdfListRvBinding):RecyclerView.ViewHolder(binding.root){}
}