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

/**
 *  Coded by: Fabio Luis Buitrago Ochoa
 */

class CameraActivity : AppCompatActivity() {

    /**
     * 2. Programe el primer botón para lanzar una nueva actividad que permita cargar una imagen en la pantalla
     * a través de la galería o la cámara de fotos. La imagen obtenida se debe mostrar en el centro de la actividad
     * y con un tamaño fijo sin importar el tamaño de la imagen original. El resultado debe ser igual al de la
     * Figura 2.
     */

    // View binding instance for the camera activity's layout
    private lateinit var binding: ActivityCameraBinding

    // Logger setup
    companion object {
        val TAG: String = CameraActivity::class.java.name
    }
    private val logger = Logger.getLogger(TAG)
    // Logger setup

    // Permission handler using ActivityResultContracts
    private val getSimplePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        updateUI(it)
    }

    // Variables for handling image and video capture
    var pictureImagePath: Uri? = null
    var imageViewContainer: ImageView? = null
    var videoViewContainer: VideoView? = null
    lateinit var switchCompat: SwitchCompat

    /**
     *  -- C A M E R A --
     *  3. (25%) Al hacer clic en el botón TAKE y el TOGGLE DE VIDEO ESTÁ DESACTIVADO se debe solicitar
     *  permisos de uso de la cámara y el usuario puede tomar una FOTO usando la app de cámara que posee
     *  el dispositivo, cuando el usuario tome correctamente la foto esta debe ser mostrada en un ImageView
     *  dentro de la actividad.
     */
    // Create ActivityResultLauncher instances for Camera
    private val cameraActivityResultLauncherCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle camera result by displaying the captured image
            imageViewContainer!!.setImageURI(pictureImagePath)
            imageViewContainer!!.setScaleType(ImageView.ScaleType.FIT_CENTER)
            imageViewContainer!!.setAdjustViewBounds(true)
            // Handle camera result by displaying the captured image
        } else {
            logger.warning("Image capture failed.")
        }
    }

    /**
     *  -- V I D E O --
     *  3. (25%9 Al hacer click en el botón TAKE y el TOGGLE DE VIDEO ESTÁ ACTIVADO se debe solicitar permisos
     *  de uso de la cámara y el usuario puede tomar un VIDEO usando la app de cámara que posee el dispositivo,
     *  cuando el usuario tome correctamente el VIDEO este debe ser mostrada en un VideoView dentro de la actividad.
     */
    // Create ActivityResultLauncher instances for Video
    private val cameraActivityResultLauncherVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle video capture by displaying the captured video
            videoViewContainer!!.setVideoURI(result.data!!.data)
            videoViewContainer!!.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoViewContainer!!.setMediaController(MediaController(this))
            videoViewContainer!!.start()
            videoViewContainer!!.setZOrderOnTop(true)
            // Handle video capture by displaying the captured video
        }
        else {
            logger.warning("Video capture failed.")
        }
    }


    /**
     *  -- G A L L E R Y --
     *  (25%) Al hacer clic en el botón PICK FROM GALLERY y el TOGGLE DE VIDEO ESTÁ DESACTIVADO se deben
     *  solicitar permisos de uso de almacenamiento y el usuario puede seleccionar una FOTO desde la galería,
     *  cuando el usuario seleccione correctamente la FOTO esta debe ser mostrada en un ImageView dentro de la
     *  actividad.
     *
     * Al hacer click en el botón PICK FROM GALLERY y el TOGGLE DE VIDEO ESTÁ ACTIVADO se deben solicitar permisos
     * de uso de almacenamiento y el usuario puede seleccionar un VIDEO desde la galería, cuando el usuario
     * seleccione correctamente el VIDEO este debe ser mostrada en un VideoView dentro de la actividad.
     */

    // Handle gallery result by displaying the selected image or video
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            if (switchCompat.isChecked) {
                // Display selected video
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
                // Display selected image
                val imageUri: Uri? = result.data!!.data
                imageViewContainer!!.setImageURI(imageUri)
                videoViewContainer!!.visibility = View.GONE
                imageViewContainer!!.visibility = View.VISIBLE
                logger.info("Image loaded successfully")
            }
        }
    }


    /**
     * This function is called when the activity is created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
    }

    /**
     * Initialize the activity by setting up UI components and click listeners.
     */
    private fun init(){

        // Initialize UI components and set click listeners
        switchCompat = binding.switchPhotoVideo
        imageViewContainer = binding.imageView
        videoViewContainer = binding.videoView

        // Set a click listener for the "buttonCamera" to request camera permission
        binding.buttonCamera.setOnClickListener {
            // Request camera permission
            verifyPermissions(this, android.Manifest.permission.CAMERA, "El permiso es requerido para tomar una foto y colocarla dentro de la aplicación")
        }

        // Set a click listener for the "buttonGallery" to open the gallery and select images or videos
        binding.buttonGallery.setOnClickListener {
            val pickGallery = Intent(Intent.ACTION_PICK)
            if (switchCompat.isChecked) {
                // Open the gallery for videos if the switch is checked
                pickGallery.type = "video/*"
            } else {
                // Open the gallery for images if the switch is not checked
                pickGallery.type = "image/*"
            }

            // Launch the gallery selection intent
            galleryActivityResultLauncher.launch(pickGallery)
        }
    }

    /**
     * Dispatches an intent to capture a picture using the device's camera.
     */
    private fun dipatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the file where the photo should be stored
        var imageFile: File? = null
        try {
            imageFile = createImageFile()
        } catch (ex: IOException) {
            logger.warning(ex.message)
        }
        // Continue if the file has been created successfully
        if (imageFile != null) {
            // Save the file path for later use
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

    /**
     * Dispatches an intent to capture a video using the device's camera.
     */
    private fun dipatchTakeVideoIntent() {
        // Create an intent to open the video capture feature of the camera
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        // Set the maximum duration of the video in seconds (in this case, 60 seconds)
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)

        // Set the video quality (0: low, 1: high)
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        try {
            // Launch the video capture intent and handle the result using cameraActivityResultLauncherVideo
            cameraActivityResultLauncherVideo.launch(takeVideoIntent)
        } catch (e: ActivityNotFoundException) {
            // Handle the case where a camera app is not found on the device
            logger.warning("Camera app not found.")
        }
    }

    /**
     * Creates a file to store a captured image.
     *
     * @return A File object representing the image file.
     * @throws IOException if there is an error during file creation.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create a timestamp based on the current date
        val timeStamp: String = DateFormat.getDateInstance().format(Date())

        // Create a unique file name for the image using the timestamp
        val imageFileName = "${timeStamp}.jpg"

        // Create a File object that represents the image file in the "Pictures" directory of the app's external files
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),imageFileName)

        return imageFile
    }

    /**
     *  -- P E R M I S S I O N S --
     */

    /**
     * Verify and request a specific permission, displaying a rationale if needed.
     *
     * @param context The context in which the permission is requested.
     * @param permission The permission to be verified and requested.
     * @param rationale A message explaining why the permission is needed.
     */
    private fun verifyPermissions(context: Context, permission: String, rationale: String) {
        when {
            // Check if the permission is already granted
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                // If the permission is already granted, update the UI
                updateUI(true)
            }
            // Check if we should display a rationale for the permission request
            shouldShowRequestPermissionRationale(permission) -> {
                // We display a snackbar with the justification for the permission, and once it disappears, we request it again.
                val snackbar = Snackbar.make(binding.root, rationale, Snackbar.LENGTH_LONG)
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            // Request the permission again after the Snackbar disappears
                            getSimplePermission.launch(permission)
                        }
                    }
                })
                snackbar.show()
            }
            else -> {
                // Request the permission without displaying a rationale
                getSimplePermission.launch(permission)
            }
        }
    }

    /**
     * Update the activity's behavior and actions based on the result of a permission request.
     *
     * @param permission Indicates whether the permission was granted (true) or denied (false).
     */
    private fun updateUI(permission : Boolean) {
        // If the permission is granted, update the UI and perform actions
        if(permission){
            logger.info("Permission granted")
            if (switchCompat.isChecked){
                // If the switch is set to video, initiate video capture
                dipatchTakeVideoIntent()
                videoViewContainer!!.visibility = View.VISIBLE
                imageViewContainer!!.visibility = View.GONE
            }
            else{
                // If the switch is set to photo, initiate image capture
                dipatchTakePictureIntent()
                videoViewContainer!!.visibility = View.GONE
                imageViewContainer!!.visibility = View.VISIBLE
            }

        }else{
            // If the permission is denied, log a warning
            logger.warning("Permission denied")
        }
    }

}