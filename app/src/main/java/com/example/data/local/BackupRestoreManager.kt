package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val timestamp: Long,
    val dateFormatted: String,
    val appName: String = "BENGKEL QU",
    val version: String = "1.0",
    val totalStocks: Int,
    val totalServices: Int,
    val totalStaff: Int,
    val totalExpenses: Int,
    val totalDeposits: Int
)

data class BackupPayload(
    val metadata: BackupMetadata,
    val profile: WorkshopProfile?,
    val staffList: List<StaffMember>,
    val stockList: List<StockItem>,
    val serviceList: List<CustomerService>,
    val incomingList: List<IncomingStock>,
    val rejectList: List<RejectItem>,
    val expenseList: List<ExpenseItem>,
    val attendanceList: List<AttendanceRecord>,
    val depositList: List<CashDeposit>
)

object BackupRestoreManager {

    private val serviceItemConverters = ServiceItemConverters()

    suspend fun createBackupJson(dao: BengkelDao): String {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date(now))

        val profile = dao.getWorkshopProfileSync()
        val staff = dao.getAllStaffMembersList()
        val stocks = dao.getAllStockItemsList()
        val services = dao.getAllCustomerServicesList()
        val incoming = dao.getAllIncomingStocksList()
        val rejects = dao.getAllRejectItemsList()
        val expenses = dao.getAllExpenseItemsList()
        val attendances = dao.getAllAttendanceRecordsList()
        val deposits = dao.getAllCashDepositsList()

        val root = JSONObject()

        // Metadata
        val metaObj = JSONObject().apply {
            put("timestamp", now)
            put("dateFormatted", dateStr)
            put("appName", "BENGKEL QU")
            put("version", "1.0")
            put("totalStocks", stocks.size)
            put("totalServices", services.size)
            put("totalStaff", staff.size)
            put("totalExpenses", expenses.size)
            put("totalDeposits", deposits.size)
        }
        root.put("metadata", metaObj)

        // Workshop Profile
        if (profile != null) {
            val profObj = JSONObject().apply {
                put("id", profile.id)
                put("workshopName", profile.workshopName)
                put("ownerName", profile.ownerName)
                put("email", profile.email)
                put("phone", profile.phone)
                put("address", profile.address)
                put("subscriptionTier", profile.subscriptionTier.name)
                put("licenseKey", profile.licenseKey)
                put("validUntilEpoch", profile.validUntilEpoch)
            }
            root.put("profile", profObj)
        }

        // Staff
        val staffArray = JSONArray()
        for (s in staff) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("role", s.role)
                put("phone", s.phone)
            }
            staffArray.put(sObj)
        }
        root.put("staff", staffArray)

        // Stocks
        val stockArray = JSONArray()
        for (st in stocks) {
            val stObj = JSONObject().apply {
                put("id", st.id)
                put("name", st.name)
                put("brand", st.brand)
                put("quality", st.quality)
                put("status", st.status.name)
                put("modalPrice", st.modalPrice)
                put("sellPrice", st.sellPrice)
                put("qty", st.qty)
                put("barcode", st.barcode)
            }
            stockArray.put(stObj)
        }
        root.put("stocks", stockArray)

        // Services
        val serviceArray = JSONArray()
        for (srv in services) {
            val srvObj = JSONObject().apply {
                put("id", srv.id)
                put("queueNumber", srv.queueNumber)
                put("customerName", srv.customerName)
                put("phoneNumber", srv.phoneNumber)
                put("plateNumber", srv.plateNumber)
                put("notes", srv.notes)
                put("mechanicName", srv.mechanicName)
                put("status", srv.status.name)
                put("discount", srv.discount)
                put("itemsJson", serviceItemConverters.fromServiceItemList(srv.items))
                put("totalAmount", srv.totalAmount)
                put("paymentMethod", srv.paymentMethod)
                put("dateEpoch", srv.dateEpoch)
            }
            serviceArray.put(srvObj)
        }
        root.put("services", serviceArray)

        // Incoming Stocks
        val incomingArray = JSONArray()
        for (inc in incoming) {
            val incObj = JSONObject().apply {
                put("id", inc.id)
                put("itemName", inc.itemName)
                put("qty", inc.qty)
                put("status", inc.status.name)
                put("dateEpoch", inc.dateEpoch)
            }
            incomingArray.put(incObj)
        }
        root.put("incoming", incomingArray)

        // Rejects
        val rejectArray = JSONArray()
        for (rej in rejects) {
            val rejObj = JSONObject().apply {
                put("id", rej.id)
                put("itemName", rej.itemName)
                put("reason", rej.reason)
                put("qty", rej.qty)
                put("status", rej.status.name)
                put("dateEpoch", rej.dateEpoch)
            }
            rejectArray.put(rejObj)
        }
        root.put("rejects", rejectArray)

        // Expenses
        val expenseArray = JSONArray()
        for (exp in expenses) {
            val expObj = JSONObject().apply {
                put("id", exp.id)
                put("name", exp.name)
                put("amount", exp.amount)
                put("status", exp.status.name)
                put("dateEpoch", exp.dateEpoch)
            }
            expenseArray.put(expObj)
        }
        root.put("expenses", expenseArray)

        // Attendance
        val attArray = JSONArray()
        for (att in attendances) {
            val attObj = JSONObject().apply {
                put("id", att.id)
                put("staffName", att.staffName)
                put("dateString", att.dateString)
                put("status", att.status.name)
                put("timeCheckIn", att.timeCheckIn)
                put("notes", att.notes)
            }
            attArray.put(attObj)
        }
        root.put("attendance", attArray)

        // Cash Deposits
        val depArray = JSONArray()
        for (dep in deposits) {
            val depObj = JSONObject().apply {
                put("id", dep.id)
                put("count100k", dep.count100k)
                put("count50k", dep.count50k)
                put("count20k", dep.count20k)
                put("count10k", dep.count10k)
                put("count5k", dep.count5k)
                put("count2k", dep.count2k)
                put("count1k", dep.count1k)
                put("count500", dep.count500)
                put("totalAmount", dep.totalAmount)
                put("dateEpoch", dep.dateEpoch)
            }
            depArray.put(depObj)
        }
        root.put("deposits", depArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)

        val metaObj = root.optJSONObject("metadata")
        val timestamp = metaObj?.optLong("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
        val dateFormatted = metaObj?.optString("dateFormatted", "-") ?: "-"
        val appName = metaObj?.optString("appName", "BENGKEL QU") ?: "BENGKEL QU"
        val version = metaObj?.optString("version", "1.0") ?: "1.0"

        // Profile
        var profile: WorkshopProfile? = null
        val profObj = root.optJSONObject("profile")
        if (profObj != null) {
            val tierStr = profObj.optString("subscriptionTier", "REGULAR")
            val tier = try {
                SubscriptionTier.valueOf(tierStr)
            } catch (e: Exception) {
                SubscriptionTier.REGULAR
            }
            profile = WorkshopProfile(
                id = profObj.optInt("id", 1),
                workshopName = profObj.optString("workshopName", "BENGKEL QU"),
                ownerName = profObj.optString("ownerName", "Hendri"),
                email = profObj.optString("email", "bageurhendri@gmail.com"),
                phone = profObj.optString("phone", "085714216556"),
                address = profObj.optString("address", "Jl. Otomotif No. 88, Bandung"),
                subscriptionTier = tier,
                licenseKey = profObj.optString("licenseKey", ""),
                validUntilEpoch = profObj.optLong("validUntilEpoch", 0L)
            )
        }

        // Staff
        val staffList = mutableListOf<StaffMember>()
        val staffArray = root.optJSONArray("staff")
        if (staffArray != null) {
            for (i in 0 until staffArray.length()) {
                val obj = staffArray.getJSONObject(i)
                staffList.add(
                    StaffMember(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", ""),
                        role = obj.optString("role", "MEKANIK"),
                        phone = obj.optString("phone", "")
                    )
                )
            }
        }

        // Stocks
        val stockList = mutableListOf<StockItem>()
        val stockArray = root.optJSONArray("stocks")
        if (stockArray != null) {
            for (i in 0 until stockArray.length()) {
                val obj = stockArray.getJSONObject(i)
                val statusStr = obj.optString("status", "JUAL_PUTUS")
                val statusEnum = try {
                    StockStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    StockStatus.JUAL_PUTUS
                }
                stockList.add(
                    StockItem(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", ""),
                        brand = obj.optString("brand", ""),
                        quality = obj.optString("quality", "Original"),
                        status = statusEnum,
                        modalPrice = obj.optLong("modalPrice", 0L),
                        sellPrice = obj.optLong("sellPrice", 0L),
                        qty = obj.optInt("qty", 0),
                        barcode = obj.optString("barcode", "")
                    )
                )
            }
        }

        // Services
        val serviceList = mutableListOf<CustomerService>()
        val serviceArray = root.optJSONArray("services")
        if (serviceArray != null) {
            for (i in 0 until serviceArray.length()) {
                val obj = serviceArray.getJSONObject(i)
                val statusStr = obj.optString("status", "ANTRIAN")
                val statusEnum = try {
                    ServiceStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    ServiceStatus.ANTRIAN
                }
                val itemsJson = obj.optString("itemsJson", "[]")
                val items = serviceItemConverters.toServiceItemList(itemsJson)

                serviceList.add(
                    CustomerService(
                        id = obj.optLong("id", 0),
                        queueNumber = obj.optInt("queueNumber", i + 1),
                        customerName = obj.optString("customerName", "Pelanggan"),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        plateNumber = obj.optString("plateNumber", ""),
                        notes = obj.optString("notes", ""),
                        mechanicName = obj.optString("mechanicName", "DAY"),
                        status = statusEnum,
                        discount = obj.optLong("discount", 0L),
                        items = items,
                        totalAmount = obj.optLong("totalAmount", 0L),
                        paymentMethod = obj.optString("paymentMethod", "CASH"),
                        dateEpoch = obj.optLong("dateEpoch", System.currentTimeMillis())
                    )
                )
            }
        }

        // Incoming
        val incomingList = mutableListOf<IncomingStock>()
        val incomingArray = root.optJSONArray("incoming")
        if (incomingArray != null) {
            for (i in 0 until incomingArray.length()) {
                val obj = incomingArray.getJSONObject(i)
                val statusStr = obj.optString("status", "PENDING")
                val statusEnum = try { ApprovalStatus.valueOf(statusStr) } catch (e: Exception) { ApprovalStatus.PENDING }
                incomingList.add(
                    IncomingStock(
                        id = obj.optLong("id", 0),
                        itemName = obj.optString("itemName", ""),
                        qty = obj.optInt("qty", 1),
                        status = statusEnum,
                        dateEpoch = obj.optLong("dateEpoch", System.currentTimeMillis())
                    )
                )
            }
        }

        // Rejects
        val rejectList = mutableListOf<RejectItem>()
        val rejectArray = root.optJSONArray("rejects")
        if (rejectArray != null) {
            for (i in 0 until rejectArray.length()) {
                val obj = rejectArray.getJSONObject(i)
                val statusStr = obj.optString("status", "PENDING")
                val statusEnum = try { ApprovalStatus.valueOf(statusStr) } catch (e: Exception) { ApprovalStatus.PENDING }
                rejectList.add(
                    RejectItem(
                        id = obj.optLong("id", 0),
                        itemName = obj.optString("itemName", ""),
                        reason = obj.optString("reason", "Cacat"),
                        qty = obj.optInt("qty", 1),
                        status = statusEnum,
                        dateEpoch = obj.optLong("dateEpoch", System.currentTimeMillis())
                    )
                )
            }
        }

        // Expenses
        val expenseList = mutableListOf<ExpenseItem>()
        val expenseArray = root.optJSONArray("expenses")
        if (expenseArray != null) {
            for (i in 0 until expenseArray.length()) {
                val obj = expenseArray.getJSONObject(i)
                val statusStr = obj.optString("status", "PENDING")
                val statusEnum = try { ApprovalStatus.valueOf(statusStr) } catch (e: Exception) { ApprovalStatus.PENDING }
                expenseList.add(
                    ExpenseItem(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", ""),
                        amount = obj.optLong("amount", 0L),
                        status = statusEnum,
                        dateEpoch = obj.optLong("dateEpoch", System.currentTimeMillis())
                    )
                )
            }
        }

        // Attendance
        val attendanceList = mutableListOf<AttendanceRecord>()
        val attArray = root.optJSONArray("attendance")
        if (attArray != null) {
            for (i in 0 until attArray.length()) {
                val obj = attArray.getJSONObject(i)
                val statusStr = obj.optString("status", "MASUK")
                val statusEnum = try { AttendanceStatus.valueOf(statusStr) } catch (e: Exception) { AttendanceStatus.MASUK }
                attendanceList.add(
                    AttendanceRecord(
                        id = obj.optLong("id", 0),
                        staffName = obj.optString("staffName", ""),
                        dateString = obj.optString("dateString", ""),
                        status = statusEnum,
                        timeCheckIn = obj.optString("timeCheckIn", "08:00"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        // Deposits
        val depositList = mutableListOf<CashDeposit>()
        val depArray = root.optJSONArray("deposits")
        if (depArray != null) {
            for (i in 0 until depArray.length()) {
                val obj = depArray.getJSONObject(i)
                depositList.add(
                    CashDeposit(
                        id = obj.optLong("id", 0),
                        count100k = obj.optInt("count100k", 0),
                        count50k = obj.optInt("count50k", 0),
                        count20k = obj.optInt("count20k", 0),
                        count10k = obj.optInt("count10k", 0),
                        count5k = obj.optInt("count5k", 0),
                        count2k = obj.optInt("count2k", 0),
                        count1k = obj.optInt("count1k", 0),
                        count500 = obj.optInt("count500", 0),
                        totalAmount = obj.optLong("totalAmount", 0L),
                        dateEpoch = obj.optLong("dateEpoch", System.currentTimeMillis())
                    )
                )
            }
        }

        val metadata = BackupMetadata(
            timestamp = timestamp,
            dateFormatted = dateFormatted,
            appName = appName,
            version = version,
            totalStocks = stockList.size,
            totalServices = serviceList.size,
            totalStaff = staffList.size,
            totalExpenses = expenseList.size,
            totalDeposits = depositList.size
        )

        return BackupPayload(
            metadata = metadata,
            profile = profile,
            staffList = staffList,
            stockList = stockList,
            serviceList = serviceList,
            incomingList = incomingList,
            rejectList = rejectList,
            expenseList = expenseList,
            attendanceList = attendanceList,
            depositList = depositList
        )
    }

    suspend fun restoreDatabase(dao: BengkelDao, payload: BackupPayload) {
        // Clear all current tables first to cleanly restore
        dao.clearCustomerServices()
        dao.clearStockItems()
        dao.clearIncomingStocks()
        dao.clearRejectItems()
        dao.clearExpenseItems()
        dao.clearAttendanceRecords()
        dao.clearCashDeposits()
        dao.clearStaffMembers()
        dao.clearWorkshopProfile()

        // Insert restored items
        payload.profile?.let { dao.insertOrUpdateProfile(it) }
        if (payload.staffList.isNotEmpty()) dao.insertAllStaff(payload.staffList)
        if (payload.stockList.isNotEmpty()) dao.insertAllStockItems(payload.stockList)
        if (payload.serviceList.isNotEmpty()) dao.insertAllCustomerServices(payload.serviceList)
        if (payload.incomingList.isNotEmpty()) dao.insertAllIncomingStocks(payload.incomingList)
        if (payload.rejectList.isNotEmpty()) dao.insertAllRejectItems(payload.rejectList)
        if (payload.expenseList.isNotEmpty()) dao.insertAllExpenseItems(payload.expenseList)
        if (payload.attendanceList.isNotEmpty()) dao.insertAllAttendance(payload.attendanceList)
        for (dep in payload.depositList) {
            dao.insertCashDeposit(dep)
        }
    }

    suspend fun resetTransactionsOnly(dao: BengkelDao) {
        dao.clearCustomerServices()
        dao.clearExpenseItems()
        dao.clearCashDeposits()
    }

    suspend fun resetToFactoryDefaults(dao: BengkelDao) {
        dao.clearCustomerServices()
        dao.clearStockItems()
        dao.clearIncomingStocks()
        dao.clearRejectItems()
        dao.clearExpenseItems()
        dao.clearAttendanceRecords()
        dao.clearCashDeposits()
        dao.clearStaffMembers()
        dao.clearWorkshopProfile()

        BengkelDatabase.populateInitialData(dao)
    }
}
