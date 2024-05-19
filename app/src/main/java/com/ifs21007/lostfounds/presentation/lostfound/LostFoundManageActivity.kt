package com.ifs21007.lostfounds.presentation.lostfound

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.ifs21007.lostfounds.R
import com.ifs21007.lostfounds.data.model.LostFound
import com.ifs21007.lostfounds.data.remote.MyResult
import com.ifs21007.lostfounds.databinding.ActivityLostfoundManageBinding
import com.ifs21007.lostfounds.presentation.ViewModelFactory
import com.ifs21007.lostfounds.helper.Utils.Companion.observeOnce
import com.ifs21007.lostfounds.helper.getImageUri
import com.ifs21007.lostfounds.helper.reduceFileImage
import com.ifs21007.lostfounds.helper.uriToFile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class LostFoundManageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLostfoundManageBinding
    private var currentImageUri:Uri? = null

    private val viewModel by viewModels<LostFoundViewModel> {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostfoundManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        setupAction()
    }

    private fun setupView() {
        showLoading(false)

//        binding.btnLostFoundPicture.setOnClickListener {
//            // Membuat intent untuk memilih gambar dari galeri
//            val intent = Intent(Intent.ACTION_GET_CONTENT)
//            intent.type = "image/*"
//
//            // Memulai activity untuk memilih gambar dari galeri
//            launcher.launch(intent)
//        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri = result.data?.data
            // Lakukan sesuatu dengan URI gambar yang dipilih
            // Misalnya, tampilkan gambar tersebut di ImageView
            binding.ivSelectedImage.setImageURI(selectedImageUri)
        }
    }

    private fun setupAction() {
        val isAddLostFound = intent.getBooleanExtra(KEY_IS_ADD, true)
        if (isAddLostFound) {
            manageAddLostFound()
        } else {
            val delcomTodo = intent.getParcelableExtra<LostFound>(KEY_TODO)
            if (delcomTodo == null) {
                finishAfterTransition()
                return
            }
            manageEditLostFound(delcomTodo)
        }

        binding.appbarLostFoundManage.setNavigationOnClickListener {
            finishAfterTransition()
        }
    }

    private fun manageAddLostFound() {
        binding.apply {
            appbarLostFoundManage.title = "Tambah Lost And Found"

            btnTodoManageCamera.setOnClickListener {
                startCamera()
            }
            btnTodoManageGallery.setOnClickListener {
                startGallery()
            }
            btnLostFoundManageSave.setOnClickListener {
                val title = etLostFoundManageTitle.text.toString()
                val description = etLostFoundManageDesc.text.toString()
                val status = etLostFoundManageStatus.selectedItem.toString()

                if (title.isEmpty() || description.isEmpty()) {
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage("Tidak boleh ada data yang kosong!")
                        setPositiveButton("Oke") { _, _ -> }
                        create()
                        show()
                    }
                    return@setOnClickListener
                }

                if (currentImageUri != null) {
                    val imageFile = uriToFile(currentImageUri!!, this@LostFoundManageActivity).reduceFileImage()
                    val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
                    val cover = MultipartBody.Part.createFormData("cover", imageFile.name, requestImageFile)
                    observePostLostFound(title, description, status, cover)
                } else {
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage("Gambar harus dipilih!")
                        setPositiveButton("Oke") { _, _ -> }
                        create()
                        show()
                    }
                }
            }
        }
    }

    private fun observePostLostFound(title: String, description: String, status: String, cover:MultipartBody.Part) {
        viewModel.postLostFound(title, description, status, cover).observeOnce { result ->
            when (result) {
                is MyResult.Loading -> {
                    showLoading(true)
                }

                is MyResult.Success -> {
                    showLoading(false)
                    val resultIntent = Intent()
                    setResult(RESULT_CODE, resultIntent)
                    finishAfterTransition()
                }


                is MyResult.Error -> {
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage(result.error)
                        setPositiveButton("Oke") { _, _ -> }
                        create()
                        show()
                    }
                    showLoading(false)
                }
            }
        }
    }

    private fun manageEditLostFound(todo: LostFound) {
        binding.apply {
            appbarLostFoundManage.title = "Ubah LostFound"

            etLostFoundManageTitle.setText(todo.title)
            etLostFoundManageDesc.setText(todo.description)
            // Mengatur item yang dipilih di Spinner
            val statusArray = resources.getStringArray(R.array.status)
            val statusIndex = statusArray.indexOf(todo.status)
            etLostFoundManageStatus.setSelection(statusIndex)

            if (todo.cover != null) {
                Glide.with(this@LostFoundManageActivity)
                    .load(todo.cover)
                    .placeholder(R.drawable.ic_image_24)
                    .into(ivSelectedImage)
            }
            btnTodoManageCamera.setOnClickListener{
                startCamera()
            }
            btnTodoManageGallery.setOnClickListener{
                startGallery()
            }

            btnLostFoundManageSave.setOnClickListener {
                val title = etLostFoundManageTitle.text.toString()
                val description = etLostFoundManageDesc.text.toString()
                val status = etLostFoundManageStatus.selectedItem.toString()

                if (title.isEmpty() || description.isEmpty()) {
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage("Tidak boleh ada data yang kosong!")
                        setPositiveButton("Oke") { _, _ -> }
                        create()
                        show()
                    }
                    return@setOnClickListener
                }

                val updatedLostFound = LostFound(todo.id, title,description,status,todo.iscompleted, todo.cover)
                observePutLostFound(todo.id, title, description, status, todo.iscompleted, updatedLostFound)
            }
        }
    }

    private fun startGallery() {
        launcherGallery.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Toast.makeText(
                applicationContext,
                "Tidak ada media yang dipilih!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showImage() {
        currentImageUri?.let {
            binding.ivSelectedImage.setImageURI(it)
        }
    }
    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun observeAddCoverTodo(
        todoId: Int,
        updatedLostFound: LostFound
    ) {
        val imageFile =
            uriToFile(currentImageUri!!, this).reduceFileImage()
        val requestImageFile =
            imageFile.asRequestBody("image/jpeg".toMediaType())
        val reqPhoto =
            MultipartBody.Part.createFormData(
                "cover",
                imageFile.name,
                requestImageFile
            )
        viewModel.addCover(
            todoId,
            reqPhoto
        ).observeOnce { result ->
            when (result) {
                is MyResult.Loading -> {
                    showLoading(true)
                }
                is MyResult.Success -> {
                    showLoading(false)
                    val updatedLostFoundWithCover = updatedLostFound.copy(cover = currentImageUri.toString())
                    val resultIntent = Intent().apply {
                        putExtra("updated_lostfound", updatedLostFoundWithCover)
                    }
                    setResult(RESULT_CODE, resultIntent)
                    finishAfterTransition()
                }
                is MyResult.Error -> {
                    showLoading(false)
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage(result.error)
                        setPositiveButton("Oke") { _, _ ->
                            val resultIntent = Intent()
                            setResult(RESULT_CODE, resultIntent)
                            finishAfterTransition()
                        }
                        setCancelable(false)
                        create()
                        show()
                    }
                }
            }
        }
    }
    private fun observePutLostFound(
        todoId: Int,
        title: String,
        description: String,
        status: String,
        isCompleted: Boolean,
        updatedLostFound: LostFound
    ) {
        viewModel.putLostFound(
            todoId,
            title,
            description,
            status,
            isCompleted
        ).observeOnce { result ->
            when (result) {
                is MyResult.Loading -> {
                    showLoading(true)
                }

                is MyResult.Success -> {
                    if (currentImageUri != null) {
                        observeAddCoverTodo(todoId,updatedLostFound)
                    } else {
                        showLoading(false)
                        val resultIntent = Intent().apply{
                            putExtra("updat_lostfound", updatedLostFound)
                        }
                        setResult(RESULT_CODE, resultIntent)
                        finishAfterTransition()
                    }
                }

                is MyResult.Error -> {
                    AlertDialog.Builder(this@LostFoundManageActivity).apply {
                        setTitle("Oh No!")
                        setMessage(result.error)
                        setPositiveButton("Oke") { _, _ -> }
                        create()
                        show()
                    }
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.pbLostFoundManage.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.btnLostFoundManageSave.isActivated = !isLoading

        binding.btnLostFoundManageSave.text =
            if (isLoading) "" else "Simpan"
    }

    companion object {
        const val KEY_IS_ADD = "is_add"
        const val KEY_TODO = "todo"
        const val RESULT_CODE = 1002
    }
}
