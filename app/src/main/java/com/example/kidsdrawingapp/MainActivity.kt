package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView?=null
    private var mImageButtonCurrentPaint:ImageButton?=null
    private var customDialog:Dialog?=null

    val openGalleryLauncher:ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        //it.data is the location of the image selected in form of a URI
        //URI is like URL of android devices
        if(it.resultCode== RESULT_OK && it.data!=null)
        {
            val imageBackground:ImageView=findViewById(R.id.iv_background)
            imageBackground.setImageURI(it.data?.data)
        }
    }

   private val requestPermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach{
                val nameOfPermission=it.key
                val isGranted=it.value
                if(isGranted)
                {
                    if(nameOfPermission == Manifest.permission.READ_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this, "Read storage permission granted", Toast.LENGTH_SHORT).show()
                        val pickIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }else if(nameOfPermission == Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this, "Write storage permission granted", Toast.LENGTH_SHORT).show()
                        val pickIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }
                }else{
                    if(nameOfPermission == Manifest.permission.READ_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                    }else if(nameOfPermission == Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this, "Write storage permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val linearLayoutColorPalette:LinearLayout=findViewById(R.id.color_palette)

        mImageButtonCurrentPaint=linearLayoutColorPalette[6] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.palette_pressed)

        )

        drawingView=findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20f)

        val btnBrushSize:ImageButton=findViewById(R.id.btn_brush_size_selector)
        btnBrushSize.setOnClickListener{
            showBrushSizeSelectorDialog()
        }
        val btnGallery:ImageButton=findViewById(R.id.btn_gallery_image_selector)
        btnGallery.setOnClickListener{
            //ask for permission here
            requestStoragePermission()
        }
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener{
                drawingView?.onClickUndo()
//            drawingView?.mPaths.let{
//                it?.removeAt(it!!.size-1)
//                drawingView?.invalidate()
            //}
        }

        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            if(isRReadingStorageAllowed())// reading and writing storage are both allowed simultaneously in latest android versions
            {
                showCustomDialog()
                lifecycleScope.launch{
//                    val flDrawingView=findViewById<FrameLayout>(R.id.frameLayout)
//                    val bitmap:Bitmap=getBitmapFromView(flDrawingView)
//                    saveBitmapFile(bitmap)
                    saveBitmapFile(getBitmapFromView(findViewById<FrameLayout>(R.id.frameLayout)))
                }
            }
        }

    }
    private fun showBrushSizeSelectorDialog()
    {
        val brushDialog= Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Select a Brush Size :")
        val smallBtn:ImageButton=brushDialog.findViewById(R.id.ib_small_size)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(8f)
            brushDialog.dismiss()
        }
        val mediumBtn:ImageButton=brushDialog.findViewById(R.id.ib_medium_size)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(12f)
            brushDialog.dismiss()
        }
        val largeBtn:ImageButton=brushDialog.findViewById(R.id.ib_large_size)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(16f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    fun paintClicked(view :View)
    {
        if(view !==mImageButtonCurrentPaint)
        {
            val imageButton=view as ImageButton
            val colorTag= imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.palette_pressed)

            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.palette_normal)
            )
            mImageButtonCurrentPaint=imageButton

        }
    }

    private fun isRReadingStorageAllowed():Boolean
    {
        val result =ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result== PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission()
    {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            showRationaleDialog("Kids Drawing App", "This application requires access to your memory to get a background image")
        }else
        {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showRationaleDialog(title:String,message:String)
    {
        val builder:AlertDialog.Builder= AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog=builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun getBitmapFromView(view:View):Bitmap
    {
        val returnedBitmap=Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas= Canvas(returnedBitmap)
        val bgDrawable=view.background
        if(bgDrawable!=null)
        {
            bgDrawable.draw(canvas)
        }else
        {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String
    {
        var result=""
        withContext(Dispatchers.IO)
        {
            if(mBitmap!=null){
                try{
                    val bytes= ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val file= File(externalCacheDir?.absoluteFile.toString() +File.separator+
                            "KidsDrawingApp_"+ System.currentTimeMillis()/1000 +".png")
                    val fileOutput=FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()//IMPORTANT
                    result=file.absolutePath

                    runOnUiThread {
                        removeCustomDialog()

                        if(result!="")
                        {
                            Toast.makeText(this@MainActivity, "File saved successfully at $result",
                                Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e:Exception)
                {
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showCustomDialog()
    {
        customDialog= Dialog(this)
        customDialog?.setContentView(R.layout.dialog_custom)
        customDialog?.show()
    }
    private fun removeCustomDialog()
    {
        if(customDialog!=null)
        {
            customDialog?.dismiss()
            customDialog=null
        }

    }
    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null)
        {
            path, uri ->
            val shareIntent=Intent()
            shareIntent.action=Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type ="image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}