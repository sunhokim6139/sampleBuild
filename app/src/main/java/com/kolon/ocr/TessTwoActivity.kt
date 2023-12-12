package com.kolon.ocr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.googlecode.tesseract.android.TessBaseAPI
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class TessTwoActivity : AppCompatActivity() {
    var image //사용되는 이미지
            : Bitmap? = null
    private var mTess //Tess API reference
            : TessBaseAPI? = null
    var datapath = "" //언어데이터가 있는 경로
    var uri : Uri? = null // 갤러리에서 가져온 이미지에 대한 Uri

    var btn_picture //사진 찍는 버튼
            : Button? = null
    var btn_ocr //텍스트 추출 버튼
            : Button? = null
    var btn_mlkit //텍스트 추출 버튼
            : Button? = null

    private var imageFilePath //이미지 파일 경로
            : String? = null
    private var p_Uri: Uri? = null
    var bitmap : Bitmap? = null // 갤러리에서 가져온 이미지를 담을 비트맵
    var imageView : ImageView? = null  // 갤러리에서 가져온 이미지를 보여줄 뷰

    val REQUEST_IMAGE_CAPTURE = 672
    val REQUEST_CODE = 232
    var strategy: DefaultVideoStrategy? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tesstwo)
        btn_picture = findViewById(R.id.takePicture)
        btn_ocr = findViewById(R.id.ocrButton)
        imageView = findViewById(R.id.imageView)
        btn_mlkit = findViewById(R.id.mlkit_btn)

        strategy = DefaultVideoStrategy.exact(1080,720).build()


        //언어파일 경로
        datapath = "$filesDir/tesseract/"

        //트레이닝데이터가 카피되어 있는지 체크
        checkFile(File(datapath + "tessdata/"), "kor");
        checkFile(File(datapath + "tessdata/"), "eng");
        /**
         * Tesseract API
         * 한글 + 영어(함께 추출)
         * 한글만 추출하거나 영어만 추출하고 싶다면
         * String lang = "eng"와 같이 작성해도 무관
         */
        val lang = "kor+eng"

        mTess = TessBaseAPI()
        mTess!!.init(datapath, lang)
        btn_picture!!.setOnClickListener {
//            sendTakePhotoIntent()
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(intent, REQUEST_CODE)
        }
        btn_ocr!!.setOnClickListener {
            // 가져와진 사진을 bitmap으로 추출

            var OCRresult: String? = null
            mTess!!.setImage(bitmap)

            //텍스트 추출

            //텍스트 추출
            OCRresult = mTess!!.utF8Text
            val OCRTextView = findViewById<View>(R.id.OCRTextView) as TextView
            OCRTextView.text = OCRresult
        }
        btn_mlkit!!.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun exifOrientationToDegrees(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun sendTakePhotoIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                p_Uri = FileProvider.getUriForFile(this, packageName, photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, p_Uri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            // 갤러리에서 선택한 사진에 대한 uri를 가져온다.
            uri = data?.data

            uri?.let { setImage(it) }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            var exif: ExifInterface? = null
            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            try {
                exif = ExifInterface(imageFilePath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val exifOrientation: Int
            val exifDegree: Int
            if (exif != null) {
                exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifDegree = exifOrientationToDegrees(exifOrientation)
            } else {
                exifDegree = 0
            }
            (findViewById<View>(R.id.imageView) as ImageView).setImageBitmap(
                rotate(
                    bitmap,
                    exifDegree.toFloat()
                )
            )
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "TEST_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        imageFilePath = image.absolutePath
        return image
    }

    //장치에 파일 복사
    private fun copyFiles(lang: String) {
        try {
            //파일이 있을 위치
            val filepath = "$datapath/tessdata/$lang.traineddata"

            //AssetManager에 액세스
            val assetManager = assets

            //읽기/쓰기를 위한 열린 바이트 스트림
            val instream = assetManager.open("tessdata/$lang.traineddata")
            val outstream: OutputStream = FileOutputStream(filepath)

            //filepath에 의해 지정된 위치에 파일 복사
            val buffer = ByteArray(1024)
            var read: Int
            while (instream.read(buffer).also { read = it } != -1) {
                outstream.write(buffer, 0, read)
            }
            outstream.flush()
            outstream.close()
            instream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //check file on the device
    private fun checkFile(dir: File, lang: String) {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(lang)
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if (dir.exists()) {
            val datafilepath = "$datapath/tessdata/$lang.traineddata"
            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles(lang)
            }
        }
    }

    // uri를 비트맵으로 변환시킨후 이미지뷰에 띄워주고 InputImage를 생성하는 메서드
    private fun setImage(uri: Uri) {
        try {
            val inputS: InputStream? = contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputS)
            imageView!!.setImageBitmap(bitmap)
//            image = InputImage.fromBitmap(bitmap!!, 0)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

}