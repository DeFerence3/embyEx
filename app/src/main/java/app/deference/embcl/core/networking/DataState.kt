package app.deference.embcl.core.networking

sealed class DataState<out T> {
	
	inline fun <T> DataState<T>.onFailure(
		action: (String) -> Unit,
	): DataState<T> {
		if (this is Error) {
			action(message)
		}
		return this
	}
	
	inline fun <T> DataState<T>.onSuccess(
		action: (T) -> Unit,
	): DataState<T> {
		if (this is Success) {
			action(data)
		}
		return this
	}
	
	fun onLoading(function: () -> Unit) {
		if (this is Loading) function()
	}
	
	data object Loading : DataState<Nothing>()
	data class Success<out T>(val data: T) : DataState<T>()
	data class Error(val message: String) : DataState<Nothing>()
}