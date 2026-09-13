package ec.edu.uteq.scli.mobile.features.profile.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

data class ProfileDto(
    val id: String,
    val identificacion: String,
    val nombres: String,
    val apellidos: String,
    val emailInstitucional: String,
    val emailPersonal: String?,
    val telefono: String?,
    val direccion: String?,
    val fechaNacimiento: String?,
    val fotoUrl: String?,
    val activo: Boolean,
)

data class UpdateOwnProfileRequest(
    val emailPersonal: String?,
    val telefono: String?,
    val direccion: String?,
    val fotoUrl: String?,
)

interface ProfileApi {
    @GET("api/v1/perfiles/me")
    suspend fun getOwnProfile(): ProfileDto

    @PATCH("api/v1/perfiles/me")
    suspend fun updateOwnProfile(@Body request: UpdateOwnProfileRequest): ProfileDto
}

class ProfileRepository(private val api: ProfileApi) {
    suspend fun getOwnProfile(): ProfileDto = api.getOwnProfile()
    suspend fun updateOwnProfile(request: UpdateOwnProfileRequest): ProfileDto =
        api.updateOwnProfile(request)
}
