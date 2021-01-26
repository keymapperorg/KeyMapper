import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 23/01/21.
 */
class FakeDataStore : DataStore<Preferences> {
    private val dataStateFlow = MutableStateFlow(preferencesOf())
    override val data: Flow<Preferences> = dataStateFlow

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences): Preferences {

        return data.map { data ->
            transform.invoke(data).apply {
                dataStateFlow.value = this
            }
        }.first()
    }
}