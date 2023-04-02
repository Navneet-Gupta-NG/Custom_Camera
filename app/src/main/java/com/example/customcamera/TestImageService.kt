import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * A Retrofit service interface for uploading test images to a backend API.
 */
interface TestImageService {

    @POST("api/v1/tests")
    fun sendTestImagesToBackend(
        @Header("Authorization") authorizationHeader: String,
        @Body requestBody: RequestBody
    ): Call<TestImageResponse>
}

data class TestImageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("done_date") val doneDate: String,
    @SerializedName("batch_qr_code") val batchQrCode: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("failure") val failure: Boolean,
    @SerializedName("images") val images: List<TestImage>
)

/**
 * A data class representing a test image.
 */
data class TestImage(
    @SerializedName("image_name") val imageName: String,
    @SerializedName("image_data") val imageData: String
)
