package ec.edu.uteq.scli.mobile.common.network

sealed interface NetworkResult<out T> {
    data class Success<T>(
        val value: T,
        val source: DataSource = DataSource.REMOTE,
        val refreshError: String? = null,
    ) : NetworkResult<T>
    data class Failure(val statusCode: Int?, val message: String) : NetworkResult<Nothing>
}

enum class DataSource { REMOTE, CACHE }
