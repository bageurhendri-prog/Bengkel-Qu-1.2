package com.example.data.repository

import com.example.data.local.ApprovalStatus
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceStatus
import com.example.data.local.BengkelDao
import com.example.data.local.BengkelDatabase
import com.example.data.local.CashDeposit
import com.example.data.local.CustomerService
import com.example.data.local.ExpenseItem
import com.example.data.local.IncomingStock
import com.example.data.local.RejectItem
import com.example.data.local.StaffMember
import com.example.data.local.StockItem
import com.example.data.local.WorkshopProfile
import kotlinx.coroutines.flow.Flow

class BengkelRepository(private val dao: BengkelDao) {

    // Customer Services
    val activeServices: Flow<List<CustomerService>> = dao.getActiveCustomerServices()
    val completedServices: Flow<List<CustomerService>> = dao.getCompletedCustomerServices()
    val allServices: Flow<List<CustomerService>> = dao.getAllCustomerServices()

    suspend fun getCustomerServiceById(id: Long) = dao.getCustomerServiceById(id)
    suspend fun insertCustomerService(service: CustomerService) = dao.insertCustomerService(service)
    suspend fun updateCustomerService(service: CustomerService) = dao.updateCustomerService(service)
    suspend fun deleteCustomerService(service: CustomerService) = dao.deleteCustomerService(service)
    suspend fun getNextQueueNumber(): Int {
        val max = dao.getMaxQueueNumber() ?: 0
        return max + 1
    }

    // Stocks
    val allStockItems: Flow<List<StockItem>> = dao.getAllStockItems()
    fun searchStockItems(query: String): Flow<List<StockItem>> = dao.searchStockItems(query)
    suspend fun insertStockItem(item: StockItem) = dao.insertStockItem(item)
    suspend fun updateStockItem(item: StockItem) = dao.updateStockItem(item)
    suspend fun deleteStockItem(item: StockItem) = dao.deleteStockItem(item)

    // Incoming Stocks
    val incomingStocks: Flow<List<IncomingStock>> = dao.getAllIncomingStocks()
    suspend fun insertIncomingStock(item: IncomingStock) = dao.insertIncomingStock(item)
    suspend fun updateIncomingStockStatus(id: Long, status: ApprovalStatus) = dao.updateIncomingStockStatus(id, status)

    // Reject Items
    val rejectItems: Flow<List<RejectItem>> = dao.getAllRejectItems()
    suspend fun insertRejectItem(item: RejectItem) = dao.insertRejectItem(item)
    suspend fun updateRejectItemStatus(id: Long, status: ApprovalStatus) = dao.updateRejectItemStatus(id, status)

    // Expense Items
    val expenseItems: Flow<List<ExpenseItem>> = dao.getAllExpenseItems()
    suspend fun insertExpenseItem(item: ExpenseItem) = dao.insertExpenseItem(item)
    suspend fun updateExpenseItemStatus(id: Long, status: ApprovalStatus) = dao.updateExpenseItemStatus(id, status)

    // Attendance
    fun getAttendanceByDate(dateStr: String): Flow<List<AttendanceRecord>> = dao.getAttendanceByDate(dateStr)
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = dao.getAllAttendanceRecords()
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord) = dao.insertOrUpdateAttendance(record)
    suspend fun updateAttendanceStatus(id: Long, status: AttendanceStatus) = dao.updateAttendanceStatus(id, status)
    suspend fun deleteAttendanceByStaffName(staffName: String) = dao.deleteAttendanceByStaffName(staffName)

    // Cash Deposit
    val cashDeposits: Flow<List<CashDeposit>> = dao.getAllCashDeposits()
    suspend fun insertCashDeposit(deposit: CashDeposit) = dao.insertCashDeposit(deposit)

    // Workshop Profile
    val workshopProfile: Flow<WorkshopProfile?> = dao.getWorkshopProfile()
    suspend fun updateWorkshopProfile(profile: WorkshopProfile) = dao.insertOrUpdateProfile(profile)

    // Staff Members
    val staffMembers: Flow<List<StaffMember>> = dao.getAllStaffMembers()
    suspend fun insertStaffMember(staff: StaffMember) = dao.insertStaffMember(staff)
    suspend fun deleteStaffMember(staff: StaffMember) = dao.deleteStaffMember(staff)

    // Barcode Lookup
    suspend fun findStockByBarcode(barcode: String): StockItem? = dao.getStockByBarcode(barcode)

    // Backup, Restore, and Reset
    suspend fun createBackupJson(): String {
        return com.example.data.local.BackupRestoreManager.createBackupJson(dao)
    }

    suspend fun restoreBackup(payload: com.example.data.local.BackupPayload) {
        com.example.data.local.BackupRestoreManager.restoreDatabase(dao, payload)
    }

    suspend fun resetTransactionsOnly() {
        com.example.data.local.BackupRestoreManager.resetTransactionsOnly(dao)
    }

    suspend fun resetToFactoryDefaults() {
        com.example.data.local.BackupRestoreManager.resetToFactoryDefaults(dao)
    }

    suspend fun resetAllToZero() {
        dao.clearCustomerServices()
        dao.clearStockItems()
        dao.clearIncomingStocks()
        dao.clearRejectItems()
        dao.clearExpenseItems()
        dao.clearAttendanceRecords()
        dao.clearCashDeposits()
    }

    suspend fun syncMasterData() {
        BengkelDatabase.populateInitialData(dao)
    }
}
