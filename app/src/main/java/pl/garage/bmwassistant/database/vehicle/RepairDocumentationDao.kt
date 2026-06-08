package pl.garage.bmwassistant.database.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RepairDocumentationDao {
    @Query("SELECT * FROM repair_documentation ORDER BY updatedAtEpochMillis DESC")
    suspend fun getAllDocumentation(): List<RepairDocumentationEntity>

    @Query("SELECT * FROM repair_documentation WHERE repairId = :repairId LIMIT 1")
    suspend fun getDocumentationForRepair(repairId: String): RepairDocumentationEntity?

    @Query("DELETE FROM repair_documentation")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentation(documentation: RepairDocumentationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedShoppingItems(items: List<DocumentationArchivedShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTisLinks(items: List<TisDocumentationLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYoutubeVideos(items: List<YoutubeVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalItems(items: List<PersonalDocumentationItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorqueTables(items: List<TorqueSpecTableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorqueSpecs(items: List<TorqueSpecEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorqueAssignments(items: List<TorqueDiagramAssignmentEntity>)

    @Query("DELETE FROM documentation_archived_shopping_items WHERE documentationId = :documentationId")
    suspend fun deleteArchivedShoppingItems(documentationId: String)

    @Query("DELETE FROM tis_documentation_links WHERE documentationId = :documentationId")
    suspend fun deleteTisLinks(documentationId: String)

    @Query("DELETE FROM youtube_videos WHERE documentationId = :documentationId")
    suspend fun deleteYoutubeVideos(documentationId: String)

    @Query("DELETE FROM personal_documentation_items WHERE documentationId = :documentationId")
    suspend fun deletePersonalItems(documentationId: String)

    @Query("DELETE FROM torque_diagram_assignments WHERE tableId IN (SELECT tableId FROM torque_spec_tables WHERE documentationId = :documentationId)")
    suspend fun deleteTorqueAssignments(documentationId: String)

    @Query("DELETE FROM torque_specs WHERE tableId IN (SELECT tableId FROM torque_spec_tables WHERE documentationId = :documentationId)")
    suspend fun deleteTorqueSpecs(documentationId: String)

    @Query("DELETE FROM torque_spec_tables WHERE documentationId = :documentationId")
    suspend fun deleteTorqueTables(documentationId: String)

    @Query("SELECT * FROM documentation_archived_shopping_items WHERE documentationId = :documentationId ORDER BY sortOrder ASC")
    suspend fun getArchivedShoppingItems(documentationId: String): List<DocumentationArchivedShoppingItemEntity>

    @Query("SELECT * FROM tis_documentation_links WHERE documentationId = :documentationId ORDER BY sortOrder ASC")
    suspend fun getTisLinks(documentationId: String): List<TisDocumentationLinkEntity>

    @Query("SELECT * FROM youtube_videos WHERE documentationId = :documentationId ORDER BY sortOrder ASC")
    suspend fun getYoutubeVideos(documentationId: String): List<YoutubeVideoEntity>

    @Query("SELECT * FROM personal_documentation_items WHERE documentationId = :documentationId ORDER BY sortOrder ASC")
    suspend fun getPersonalItems(documentationId: String): List<PersonalDocumentationItemEntity>

    @Query("SELECT * FROM torque_spec_tables WHERE documentationId = :documentationId ORDER BY sortOrder ASC")
    suspend fun getTorqueTables(documentationId: String): List<TorqueSpecTableEntity>

    @Query("SELECT * FROM torque_specs WHERE tableId = :tableId ORDER BY sortOrder ASC")
    suspend fun getTorqueSpecs(tableId: String): List<TorqueSpecEntity>

    @Query("SELECT * FROM torque_diagram_assignments WHERE tableId = :tableId ORDER BY sortOrder ASC")
    suspend fun getTorqueAssignments(tableId: String): List<TorqueDiagramAssignmentEntity>

    @Transaction
    suspend fun replaceDocumentationBundle(
        documentation: RepairDocumentationEntity,
        archivedShoppingItems: List<DocumentationArchivedShoppingItemEntity>,
        tisLinks: List<TisDocumentationLinkEntity>,
        youtubeVideos: List<YoutubeVideoEntity>,
        personalItems: List<PersonalDocumentationItemEntity>,
        torqueTables: List<TorqueSpecTableEntity>,
        torqueSpecs: List<TorqueSpecEntity>,
        torqueAssignments: List<TorqueDiagramAssignmentEntity>,
    ) {
        insertDocumentation(documentation)
        deleteArchivedShoppingItems(documentation.documentationId)
        deleteTisLinks(documentation.documentationId)
        deleteYoutubeVideos(documentation.documentationId)
        deletePersonalItems(documentation.documentationId)
        deleteTorqueAssignments(documentation.documentationId)
        deleteTorqueSpecs(documentation.documentationId)
        deleteTorqueTables(documentation.documentationId)

        if (archivedShoppingItems.isNotEmpty()) insertArchivedShoppingItems(archivedShoppingItems)
        if (tisLinks.isNotEmpty()) insertTisLinks(tisLinks)
        if (youtubeVideos.isNotEmpty()) insertYoutubeVideos(youtubeVideos)
        if (personalItems.isNotEmpty()) insertPersonalItems(personalItems)
        if (torqueTables.isNotEmpty()) insertTorqueTables(torqueTables)
        if (torqueSpecs.isNotEmpty()) insertTorqueSpecs(torqueSpecs)
        if (torqueAssignments.isNotEmpty()) insertTorqueAssignments(torqueAssignments)
    }
}
