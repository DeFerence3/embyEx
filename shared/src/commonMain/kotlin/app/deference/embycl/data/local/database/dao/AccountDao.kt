package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AccountDao {
	
	@Upsert
	suspend fun saveAccount(row: AccountEntity)
	
	@Query("SELECT * FROM offline_accounts ORDER BY serverName, userName, serverId, userId")
	fun observeAccounts(): Flow<List<AccountEntity>>
}
