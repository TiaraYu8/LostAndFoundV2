package com.ifs21007.lostfounds.presentation.lostfound

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ifs21007.lostfounds.data.local.entity.DelcomLostFoundEntity
import com.ifs21007.lostfounds.data.remote.MyResult
import com.ifs21007.lostfounds.data.remote.response.DataAddLostFoundResponse
import com.ifs21007.lostfounds.data.remote.response.DelcomLostFoundResponse
import com.ifs21007.lostfounds.data.remote.response.DelcomResponse
import com.ifs21007.lostfounds.data.repository.LocalLostFoundRepository
import com.ifs21007.lostfounds.data.repository.LostFoundRepository
import com.ifs21007.lostfounds.presentation.ViewModelFactory
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class LostFoundViewModel (
    private val lostFoundRepository : LostFoundRepository,
    private val LocalLostFoundRepository: LocalLostFoundRepository
) : ViewModel() {

    fun getLostFound(lostfoundId: Int) : LiveData<MyResult<DelcomLostFoundResponse>> {
        return lostFoundRepository.getDetail(lostfoundId).asLiveData()
    }

    fun postLostFound(
        title: String,
        description: String,
        status: String,
        cover: MultipartBody.Part
    ): LiveData<MyResult<DataAddLostFoundResponse>> {
        val titleRequestBody = title.toRequestBody("text/plain".toMediaType())
        val descriptionRequestBody = description.toRequestBody("text/plain".toMediaType())
        val statusRequestBody = status.toRequestBody("text/plain".toMediaType())

        return lostFoundRepository.postLostFound(titleRequestBody, descriptionRequestBody, statusRequestBody, cover)
            .map { MyResult.Success(it) as MyResult<DataAddLostFoundResponse> }
            .catch { e -> emit(MyResult.Error(e.message ?: "Unknown error")) }
            .asLiveData()
    }

    fun putLostFound(
        lostfoundId: Int,
        title: String,
        description: String,
        status: String,
        isCompleted: Boolean,
    ) : LiveData<MyResult<DelcomResponse>> {
        return lostFoundRepository.putLostFound(
            lostfoundId,
            title,
            description,
            status,
            isCompleted
        ).asLiveData()
    }

    fun delete(lostfoundId: Int) : LiveData<MyResult<DelcomResponse>> {
        return lostFoundRepository.delete(lostfoundId).asLiveData()
    }

    fun getLocalLostFounds(): LiveData<List<DelcomLostFoundEntity>?> {
        return LocalLostFoundRepository.getAllLostFounds()
    }
    fun getLocalLostFound(lostfoundId: Int): LiveData<DelcomLostFoundEntity?> {
        return LocalLostFoundRepository.get(lostfoundId)
    }
    fun insertLocalTodo(todo: DelcomLostFoundEntity) {
        LocalLostFoundRepository.insert(todo)
    }
    fun deleteLocalTodo(todo: DelcomLostFoundEntity) {
        LocalLostFoundRepository.delete(todo)
    }

    fun addCover(
        lostfoundId: Int,
        cover: MultipartBody.Part,
    ): LiveData<MyResult<DelcomResponse>> {
        return lostFoundRepository.addCover(lostfoundId, cover).asLiveData()
    }

    companion object {
        @Volatile
        private var INSTANCE: LostFoundViewModel? = null
        fun getInstance (
            lostFoundRepository: LostFoundRepository,
            LocalLostFoundRepository: LocalLostFoundRepository,
        ) : LostFoundViewModel {
            synchronized(ViewModelFactory::class.java) {
                INSTANCE = LostFoundViewModel(
                    lostFoundRepository,
                    LocalLostFoundRepository
                )
            }
            return INSTANCE as LostFoundViewModel
        }
    }
}