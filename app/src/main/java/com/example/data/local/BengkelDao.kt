package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BengkelDao {
    // --- Customer Services ---
    @Query("SELECT * FROM customer_services ORDER BY queueNumber ASC")
    fun getAllCustomerServices(): Flow<List<CustomerService>>

    @Query("SELECT * FROM customer_services WHERE status != 'DIBAYAR' ORDER BY queueNumber ASC")
    fun getActiveCustomerServices(): Flow<List<CustomerService>>

    @Query("SELECT * FROM customer_services WHERE status = 'DIBAYAR' ORDER BY dateEpoch DESC")
    fun getCompletedCustomerServices(): Flow<List<CustomerService>>

    @Query("SELECT * FROM customer_services WHERE id = :id")
    suspend fun getCustomerServiceById(id: Long): CustomerService?

    @Query("SELECT MAX(queueNumber) FROM customer_services")
    suspend fun getMaxQueueNumber(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerService(service: CustomerService): Long

    @Update
    suspend fun updateCustomerService(service: CustomerService)

    @Delete
    suspend fun deleteCustomerService(service: CustomerService)

    // --- Stock Items ---
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun getAllStockItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    fun searchStockItems(query: String): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStockItems(items: List<StockItem>)

    @Update
    suspend fun updateStockItem(item: StockItem)

    @Delete
    suspend fun deleteStockItem(item: StockItem)

    // --- Incoming Stocks (Barang Datang) ---
    @Query("SELECT * FROM incoming_stocks ORDER BY dateEpoch DESC")
    fun getAllIncomingStocks(): Flow<List<IncomingStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomingStock(item: IncomingStock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIncomingStocks(items: List<IncomingStock>)

    @Update
    suspend fun updateIncomingStock(item: IncomingStock)

    @Query("UPDATE incoming_stocks SET status = :status WHERE id = :id")
    suspend fun updateIncomingStockStatus(id: Long, status: ApprovalStatus)

    // --- Reject Items (Pengajuan Barang Reject) ---
    @Query("SELECT * FROM reject_items ORDER BY dateEpoch DESC")
    fun getAllRejectItems(): Flow<List<RejectItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRejectItem(item: RejectItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRejectItems(items: List<RejectItem>)

    @Update
    suspend fun updateRejectItem(item: RejectItem)

    @Query("UPDATE reject_items SET status = :status WHERE id = :id")
    suspend fun updateRejectItemStatus(id: Long, status: ApprovalStatus)

    // --- Expense Items (Belanja / Pengeluaran) ---
    @Query("SELECT * FROM expense_items ORDER BY dateEpoch DESC")
    fun getAllExpenseItems(): Flow<List<ExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseItem(item: ExpenseItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenseItems(items: List<ExpenseItem>)

    @Update
    suspend fun updateExpenseItem(item: ExpenseItem)

    @Query("UPDATE expense_items SET status = :status WHERE id = :id")
    suspend fun updateExpenseItemStatus(id: Long, status: ApprovalStatus)

    @Query("UPDATE expense_items SET status = :status, approvedAtEpoch = :approvedAt WHERE id = :id")
    suspend fun updateExpenseItemApproval(id: Long, status: ApprovalStatus, approvedAt: Long)

    // --- Attendance (Absen) ---
    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString ORDER BY staffName ASC")
    fun getAttendanceByDate(dateString: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC, timeCheckIn DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("DELETE FROM attendance_records WHERE staffName = :staffName OR staffName = UPPER(:staffName)")
    suspend fun deleteAttendanceByStaffName(staffName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(records: List<AttendanceRecord>)

    @Query("UPDATE attendance_records SET status = :status WHERE id = :id")
    suspend fun updateAttendanceStatus(id: Long, status: AttendanceStatus)

    // --- Cash Deposit (Setoran) ---
    @Query("SELECT * FROM cash_deposits ORDER BY dateEpoch DESC")
    fun getAllCashDeposits(): Flow<List<CashDeposit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashDeposit(deposit: CashDeposit): Long

    @Query("UPDATE cash_deposits SET approvalStatus = :status, approvedBy = :approvedBy, approvedAtEpoch = :approvedAt WHERE id = :id")
    suspend fun updateCashDepositApproval(id: Long, status: ApprovalStatus, approvedBy: String, approvedAt: Long)

    // --- Workshop Profile ---
    @Query("SELECT * FROM workshop_profile WHERE id = 1")
    fun getWorkshopProfile(): Flow<WorkshopProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: WorkshopProfile)

    // --- Staff Members ---
    @Query("SELECT * FROM staff_members ORDER BY name ASC")
    fun getAllStaffMembers(): Flow<List<StaffMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffMember(staff: StaffMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStaff(staffList: List<StaffMember>)

    @Delete
    suspend fun deleteStaffMember(staff: StaffMember)

    // --- Barcode Queries ---
    @Query("SELECT * FROM stock_items WHERE barcode = :code OR name LIKE '%' || :code || '%' LIMIT 1")
    suspend fun getStockByBarcode(code: String): StockItem?

    // --- Backup Queries (Direct Snapshot Lists) ---
    @Query("SELECT * FROM customer_services ORDER BY queueNumber ASC")
    suspend fun getAllCustomerServicesList(): List<CustomerService>

    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    suspend fun getAllStockItemsList(): List<StockItem>

    @Query("SELECT * FROM incoming_stocks ORDER BY dateEpoch DESC")
    suspend fun getAllIncomingStocksList(): List<IncomingStock>

    @Query("SELECT * FROM reject_items ORDER BY dateEpoch DESC")
    suspend fun getAllRejectItemsList(): List<RejectItem>

    @Query("SELECT * FROM expense_items ORDER BY dateEpoch DESC")
    suspend fun getAllExpenseItemsList(): List<ExpenseItem>

    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC")
    suspend fun getAllAttendanceRecordsList(): List<AttendanceRecord>

    @Query("SELECT * FROM cash_deposits ORDER BY dateEpoch DESC")
    suspend fun getAllCashDepositsList(): List<CashDeposit>

    @Query("SELECT * FROM workshop_profile WHERE id = 1")
    suspend fun getWorkshopProfileSync(): WorkshopProfile?

    @Query("SELECT * FROM staff_members ORDER BY name ASC")
    suspend fun getAllStaffMembersList(): List<StaffMember>

    // --- Bulk Restore Inserts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomerServices(services: List<CustomerService>)

    // --- Clear Queries for Reset ---
    @Query("DELETE FROM customer_services")
    suspend fun clearCustomerServices()

    @Query("DELETE FROM expense_items")
    suspend fun clearExpenseItems()

    @Query("DELETE FROM cash_deposits")
    suspend fun clearCashDeposits()

    @Query("DELETE FROM stock_items")
    suspend fun clearStockItems()

    @Query("DELETE FROM incoming_stocks")
    suspend fun clearIncomingStocks()

    @Query("DELETE FROM reject_items")
    suspend fun clearRejectItems()

    @Query("DELETE FROM attendance_records")
    suspend fun clearAttendanceRecords()

    @Query("DELETE FROM staff_members")
    suspend fun clearStaffMembers()

    @Query("DELETE FROM workshop_profile")
    suspend fun clearWorkshopProfile()
}
