package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZenDao {
    @Query("SELECT * FROM workspaces ORDER BY sortOrder ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaces(workspaces: List<WorkspaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :workspaceId")
    suspend fun deleteWorkspace(workspaceId: String)

    @Query("DELETE FROM workspaces WHERE isPrivate = 1")
    suspend fun deletePrivateWorkspaces()

    @Query("SELECT * FROM tabs ORDER BY sortOrder ASC, lastAccessed DESC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Update
    suspend fun updateTabs(tabs: List<TabEntity>)

    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM tabs WHERE workspaceId = :workspaceId AND isEssential = 0")
    suspend fun deleteTabsForWorkspace(workspaceId: String)

    @Query("DELETE FROM tabs WHERE isEssential = 0 AND isPinned = 0")
    suspend fun clearAllUnpinnedTabs()

    @Query("DELETE FROM tabs WHERE isPrivate = 1")
    suspend fun deleteAllPrivateTabs()

    @Query("UPDATE tabs SET isUnloaded = 1 WHERE url != 'zen://home'")
    suspend fun unloadAllActiveTabs()

    @Query("SELECT * FROM zen_mods ORDER BY category ASC, title ASC")
    fun getAllMods(): Flow<List<ZenModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMods(mods: List<ZenModEntity>)

    @Update
    suspend fun updateMod(mod: ZenModEntity)

    @Query("SELECT * FROM firefox_extensions ORDER BY isInstalled DESC, name ASC")
    fun getAllFirefoxExtensions(): Flow<List<FirefoxExtensionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirefoxExtensions(extensions: List<FirefoxExtensionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirefoxExtension(extension: FirefoxExtensionEntity)

    @Update
    suspend fun updateFirefoxExtension(extension: FirefoxExtensionEntity)

    @Query("DELETE FROM firefox_extensions WHERE id = :extensionId")
    suspend fun deleteFirefoxExtension(extensionId: String)

    @Query("SELECT * FROM custom_search_engines ORDER BY name ASC")
    fun getAllCustomSearchEngines(): Flow<List<CustomSearchEngineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomSearchEngine(engine: CustomSearchEngineEntity)

    @Query("DELETE FROM custom_search_engines WHERE id = :engineId")
    suspend fun deleteCustomSearchEngine(engineId: String)

    @Query("SELECT * FROM home_shortcuts ORDER BY sortOrder ASC")
    fun getAllHomeShortcuts(): Flow<List<HomeShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeShortcuts(shortcuts: List<HomeShortcutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeShortcut(shortcut: HomeShortcutEntity)

    @Update
    suspend fun updateHomeShortcut(shortcut: HomeShortcutEntity)

    @Query("DELETE FROM home_shortcuts WHERE id = :shortcutId")
    suspend fun deleteHomeShortcut(shortcutId: String)

    // History Queries
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC LIMIT 200")
    fun getAllHistory(): Flow<List<HistoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryEntry(id: String)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()

    @Query("DELETE FROM history_entries WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteHistoryOlderThan(cutoffTimestamp: Long)

    // Download Queries
    @Query("SELECT * FROM download_items ORDER BY timestamp DESC LIMIT 100")
    fun getAllDownloads(): Flow<List<DownloadItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadItem(item: DownloadItemEntity)

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun deleteDownloadItem(id: String)

    @Query("DELETE FROM download_items")
    suspend fun clearAllDownloads()

    // Offline Page Queries
    @Query("SELECT * FROM offline_pages ORDER BY timestamp DESC")
    fun getAllOfflinePages(): Flow<List<OfflinePageEntity>>

    @Query("SELECT * FROM offline_pages WHERE id = :id LIMIT 1")
    suspend fun getOfflinePageById(id: String): OfflinePageEntity?

    @Query("SELECT * FROM offline_pages WHERE url = :url LIMIT 1")
    suspend fun getOfflinePageByUrl(url: String): OfflinePageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflinePage(page: OfflinePageEntity)

    @Query("DELETE FROM offline_pages WHERE id = :id")
    suspend fun deleteOfflinePage(id: String)

    @Query("DELETE FROM offline_pages")
    suspend fun clearAllOfflinePages()

    // Password Manager Queries
    @Query("SELECT * FROM passwords ORDER BY domain ASC, username ASC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE lower(domain) = lower(:domain)")
    suspend fun getPasswordsForDomain(domain: String): List<PasswordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePassword(id: String)

    @Query("DELETE FROM passwords")
    suspend fun clearAllPasswords()
}

@Database(
    entities = [
        WorkspaceEntity::class,
        TabEntity::class,
        ZenModEntity::class,
        FirefoxExtensionEntity::class,
        CustomSearchEngineEntity::class,
        HomeShortcutEntity::class,
        HistoryEntryEntity::class,
        DownloadItemEntity::class,
        OfflinePageEntity::class,
        PasswordEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ZenDatabase : RoomDatabase() {
    abstract fun zenDao(): ZenDao
}
