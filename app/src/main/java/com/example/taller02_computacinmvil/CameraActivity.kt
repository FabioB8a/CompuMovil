package com.example.taller02_computacinmvil

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller02_computacinmvil.databinding.ActivityCameraBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.logging.Logger

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    companion object {
        val TAG: String = CameraActivity::class.java.name
    }
    private val logger = Logger.getLogger(TAG)

    // Permission handler
    private val getSimplePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        updateUI(it)
    }

    var pictureImagePath: Uri? = null
    var imageViewContainer: ImageView? = null
    var videoViewContainer: VideoView? = null
    lateinit var switchCompat: SwitchCompat

    // Create ActivityResultLauncher instances
    private val cameraActivityResultLauncherCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle camera result
            imageViewContainer!!.setImageURI(pictureImagePath)
            imageViewContainer!!.setScaleType(ImageView.ScaleType.FIT_CENTER)
            imageViewContainer!!.setAdjustViewBounds(true)
            logger.info("Image capture successfully.")
        } else {
            logger.warning("Image capture failed.")
        }
    }

    private val cameraActivityResultLauncherVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle camera result
            videoViewContainer!!.setVideoURI(result.data!!.data)
            videoViewContainer!!.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoViewContainer!!.setMediaController(MediaController(this))
            videoViewContainer!!.start()
            videoViewContainer!!.setZOrderOnTop(true)
        }
        else {
            logger.warning("Video capture failed.")
        }
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle gallery result
            if (switchCompat.isChecked) {
                val videoUri: Uri? = result.data!!.data
                videoViewContainer!!.setVideoURI(videoUri)
                videoViewContainer!!.setVideoURI(result.data!!.data)
                videoViewContainer!!.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
                videoViewContainer!!.setMediaController(MediaController(this))
                videoViewContainer!!.start()
                videoViewContainer!!.setZOrderOnTop(true)
                videoViewContainer!!.visibility = View.VISIBLE
                imageViewContainer!!.visibility = View.GONE
                logger.info("Video loaded successfully")
            }
            else {

                val imageUri: Uri? = result.data!!.data
                imageViewContainer!!.setImageURI(imageUri)
                videoViewContainer!!.visibility = View.GONE
                imageViewContainer!!.visibility = View.VISIBLE
                logger.info("Image loaded successfully")
            }
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
    }

    private fun init(){
        switchCompat = binding.switchPhotoVideo
        imageViewContainer = binding.imageView
        videoViewContainer = binding.videoView

        binding.buttonCamera.setOnClickListener {
            verifyPermissions(this, android.Manifest.permission.CAMERA, "El permiso es requerido para tomar una foto y colocarla dentro de la aplicación")
        }

        binding.buttonGallery.setOnClickListener {
            val pickGallery = Intent(Intent.ACTION_PICK)
            if (switchCompat.isChecked) {
                // Open the gallery for videos
                pickGallery.type = "video/*"
            } else {
                // Open the gallery for images
                pickGallery.type = "image/*"
            }

            galleryActivityResultLauncher.launch(pickGallery)
        }


    }

    private fun verifyPermissions(context: Context, permission: String, rationale: String) {
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                Snackbar.make(binding.root, "Ya tengo los permisos 😜", Snackbar.LENGTH_LONG).show()
                updateUI(true)
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // We display a snackbar with the justification for the permission, and once it disappears, we request it again.
                val snackbar = Snackbar.make(binding.root, rationale, Snackbar.LENGTH_LONG)
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            getSimplePermission.launch(permission)
                        }
                    }
                })
                snackbar.show()
            }
            else -> {
                getSimplePermission.launch(permission)
            }
        }
    }

    fun dipatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Crear el archivo donde debería ir la foto
        var imageFile: File? = null
        try {
            imageFile = createImageFile()
        } catch (ex: IOException) {
            logger.warning(ex.message)
        }
        // Continua si el archivo ha sido creado exitosamente
        if (imageFile != null) {
            // Guardar un archivo: Ruta para usar con ACTION_VIEW intents
            pictureImagePath = FileProvider.getUriForFile(this,"com.example.android.fileprovider", imageFile)
            logger.info("Ruta: ${pictureImagePath}")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureImagePath)
            try {
                cameraActivityResultLauncherCamera.launch(takePictureIntent)
            } catch (e: ActivityNotFoundException) {
                logger.warning("Camera app not found.")
            }
        }
    }

    fun dipatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        // Set maximum video duration in seconds
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
        // Set video quality (0: low, 1: high)
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        try {
            cameraActivityResultLauncherVideo.launch(takeVideoIntent)
        } catch (e: ActivityNotFoundException) {
            logger.warning("Camera app not found.")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        //Crear un nombre de archivo de imagen
        val timeStamp: String = DateFormat.getDateInstance().format(Date())
        val imageFileName = "${timeStamp}.jpg"
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),imageFileName)
        return imageFile
    }

    // Update activity behavior and actions according to result of permission request
    fun updateUI(permission : Boolean) {
        if(permission){
            logger.info("Permission granted")
            if (switchCompat.isChecked){
                dipatchTakeVideoIntent()
                videoViewContainer!!.visibility = View.VISIBLE
                imageViewContainer!!.visibility = View.GONE
            }
            else{
                dipatchTakePictureIntent()
                videoViewContainer!!.visibility = View.GONE
                imageViewContainer!!.visibility = View.VISIBLE
            }

        }else{
            logger.warning("Permission denied")
        }
    }

}