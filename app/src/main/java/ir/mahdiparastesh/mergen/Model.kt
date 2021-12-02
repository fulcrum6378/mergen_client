package ir.mahdiparastesh.mergen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class Model : ViewModel() {
    val res: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val host: MutableLiveData<String> by lazy { MutableLiveData<String>("127.0.0.1") }
    val audPort: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }
    val tocPort: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }
    val visPort: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }
    val toggling: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }


    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
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
