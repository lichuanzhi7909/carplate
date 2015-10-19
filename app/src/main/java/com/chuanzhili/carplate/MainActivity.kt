package com.chuanzhili.carplate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

public class MainActivity : AppCompatActivity() {
    public val CONTEXT_MENU_CAPTURE: Int = 2
    public val CONTEXT_MENU_ALBUM: Int = 3
    var scanImg: ImageView? = null
    var scanText: TextView? = null
    var svmPath: String ? = null
    var annPath: String ? = null
    var rootPath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("imageproc")
        rootPath = AppUtil.getTempFileDir(getApplicationContext());

        setContentView(R.layout.activity_main)
        var btn = findViewById(R.id.scan_btn) as Button
        scanImg = findViewById(R.id.scan_img) as ImageView
        scanText = findViewById(R.id.scan_result_text) as TextView
        btn.setOnClickListener {
            openContextMenu(btn)
        }
        registerForContextMenu(btn)
        svmPath = rootPath + "svm.xml"
        annPath = rootPath + "ann.xml"

        if (!File(svmPath).exists() || !File(annPath).exists())
            CopyTask(this, AppUtil.getTempFileDir(this)).execute()

    }

    class CopyTask(context: Context, rootPath: String) : AsyncTask<Void, Void, Void>() {
        var innerContext: Context? = context
        var innerRootPath: String? = rootPath

        override fun doInBackground(vararg params: Void?): Void? {
            AppUtil.copyAssets(innerContext, innerRootPath)
            return null
        }
    }


    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu?.add(CONTEXT_MENU_CAPTURE, CONTEXT_MENU_CAPTURE, CONTEXT_MENU_CAPTURE, "拍照选取")
        menu?.add(CONTEXT_MENU_ALBUM, CONTEXT_MENU_ALBUM, CONTEXT_MENU_ALBUM, "从相册中选取")
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        when (item?.getGroupId()) {
            CONTEXT_MENU_CAPTURE -> {
                openImageCapture()
            }

            CONTEXT_MENU_ALBUM -> {
                openAlbum()
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun openAlbum() {
        var getImage = Intent(Intent.ACTION_GET_CONTENT)
        getImage.addCategory(Intent.CATEGORY_OPENABLE)
        getImage.setType("image/*")
        startActivityForResult(getImage, CONTEXT_MENU_ALBUM)
    }

    private fun openImageCapture() {
        try {
            val imageFile = File(getCurrentTempFilePath())
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile))
            intent.putExtra("outputFormat", "JPEG")
            startActivityForResult(intent, CONTEXT_MENU_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "抱歉，打开照相机失败", Toast.LENGTH_SHORT).show();
        }

    }

    private fun getCurrentTempFilePath(): String {
        val imagesFolder = File(rootPath)
        imagesFolder.mkdirs()
        return imagesFolder.getPath() + "/temp.jpg"
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        var imgPath: String? = null
        var sampleBitmap: Bitmap? = null
        when (requestCode) {
            CONTEXT_MENU_ALBUM -> {
                imgPath = AppUtil.getRealPathFromURI(this, data?.getData())
                sampleBitmap = AppUtil.decodeSampledBitmapFromFile(imgPath)
            }

            CONTEXT_MENU_CAPTURE -> {
                imgPath = getCurrentTempFilePath()
                sampleBitmap = AppUtil.decodeSampledBitmapFromFile(imgPath)
            }
        }
        scanImg?.setImageBitmap(sampleBitmap)
        Toast.makeText(this, imgPath, Toast.LENGTH_SHORT).show();

//        AppUtil.bitmapToFile(sampleBitmap, getCurrentTempFilePath(), false);

        System.out.println("entering the jni");

        var resultByte = CarPlateDetection.ImageProc(imgPath, svmPath, annPath);
        var result = String(resultByte, "GBK");
        System.out.println(result);
        if ("0".equals(result))
            result = "无法识别，请重新选择图片"
        scanText?.setText(result);
    }
}
