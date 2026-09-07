package app.deference.embcl.core.networking

sealed class RequestState<out T> {
    data object Idle : RequestState<Nothing>()
    data object Loading : RequestState<Nothing>()
    data class Success<out T>(val data: T) : RequestState<T>()
    data class Error(val message: String) : RequestState<Nothing>()

    fun isLoading(): Boolean = this is Loading
    fun isError(): Boolean = this is Error
    fun isSuccess(): Boolean = this is Success

    fun getSuccessData() = (this as Success).data

    fun getSuccessDataOrNull(): T? = (this as? Success)?.data

    fun getErrorMessage() = (this as Error).message

    fun getErrorMessageOrNull(): String? = (this as? Error)?.message
}
