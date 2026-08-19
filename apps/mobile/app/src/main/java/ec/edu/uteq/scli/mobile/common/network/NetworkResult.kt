package ec.edu.uteq.scli.mobile.common.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>
    data class Failure(val statusCode: Int?, val message: String) : NetworkResult<Nothing>
}
