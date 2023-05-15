package ir.mahdiparastesh.mergen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class Model : ViewModel() {
    val res = MutableLiveData<String>()
    val host = MutableLiveData("127.0.0.1")
    val audPort = MutableLiveData(0)
    val tocPort = MutableLiveData(0)
    val visPort = MutableLiveData(0)
    val toggling = MutableLiveData(false)


    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(Model::class.java)) {
                val key = "Model"
                return if (hashMapViewModel.containsKey(key)) getViewModel(key) as T
                else {
                    addViewModel(key, Model())
                    getViewModel(key) as T
                }
            }
            throw IllegalArgumentException("Unknown Model class")
        }

        companion object {
            val hashMapViewModel = HashMap<String, ViewModel>()

            fun addViewModel(key: String, viewModel: ViewModel) =
                hashMapViewModel.put(key, viewModel)

            fun getViewModel(key: String): ViewModel? = hashMapViewModel[key]
        }
    }
}
