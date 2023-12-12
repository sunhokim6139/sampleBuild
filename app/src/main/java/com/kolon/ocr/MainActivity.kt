package com.kolon.ocr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.FileNotFoundException
import java.io.InputStream
import com.kolon.ocr.R

class MainActivity : AppCompatActivity() {
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

    val REQUEST_CODE = 2

    var imageView : ImageView? = null  // 갤러리에서 가져온 이미지를 보여줄 뷰
    var uri : Uri? = null // 갤러리에서 가져온 이미지에 대한 Uri
    var bitmap : Bitmap? = null // 갤러리에서 가져온 이미지를 담을 비트맵
    var image : InputImage? = null // ML 모델이 인식할 인풋 이미지
    var text_info : TextView? = null // ML 모델이 인식한 텍스트를 보여줄 뷰
    var btn_get_image: Button? = null // 이미지 가져오기 버튼
    var btn_detection_image :Button? = null // 이미지 인식 버튼
    var btn_tesstwo_go :Button? = null // 이미지 인식 버튼
    var recognizer : TextRecognizer? = null //텍스트 인식에 사용될 모델

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        text_info = findViewById(R.id.text_info)
        recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())//텍스트 인식에 사용될 모델


        // GET IMAGE 버튼

        // GET IMAGE 버튼
        btn_get_image = findViewById(R.id.btn_get_image)

        btn_get_image!!.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(intent, REQUEST_CODE)
        }

        // IMAGE DETECTION 버튼

        // IMAGE DETECTION 버튼
        btn_detection_image = findViewById(R.id.btn_detection_image)
        btn_detection_image!!.setOnClickListener {
            TextRecognition(recognizer!!)
        }

        btn_tesstwo_go = findViewById(R.id.btn_tess_two_go)
        btn_tesstwo_go!!.setOnClickListener {
            val intent = Intent(this, TessTwoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            // 갤러리에서 선택한 사진에 대한 uri를 가져온다.
            uri = data?.data

            uri?.let { setImage(it) }
        }
    }

    // uri를 비트맵으로 변환시킨후 이미지뷰에 띄워주고 InputImage를 생성하는 메서드
    private fun setImage(uri: Uri) {
        try {
            val inputS: InputStream? = contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputS)
            imageView!!.setImageBitmap(bitmap)
            image = InputImage.fromBitmap(bitmap!!, 0)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun TextRecognition(recognizer: TextRecognizer) {
        val result: Task<Text> = recognizer.process(image!!) // 이미지 인식에 성공하면 실행되는 리스너
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                val resultText = visionText.text
                text_info!!.text = resultText // 인식한 텍스트를 TextView에 세팅
            } // 이미지 인식에 실패하면 실행되는 리스너
            .addOnFailureListener { e ->
                text_info!!.text = e.message
            }
    }


/* private fun showCaptureImage(img: Uri) {
        try {
            val image = InputImage.fromFilePath(this, img)
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            recognizer.process(image)
                .addOnSuccessListener {
                    binding.txt.text = it.text
                }
                .addOnFailureListener {
                    binding.txt.text = it.message
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun showCaptureImage() {
        try {
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            recognizer.process(image)
                .addOnSuccessListener {
                    binding.txt.text = it.text
                }
                .addOnFailureListener {
                    binding.txt.text = it.message
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }*/
    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(cameraId: String, activity: Activity, isFrontFacing: Boolean): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // Get the device's sensor orientation.
        val cameraManager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360
        }
        return rotationCompensation
    }

}