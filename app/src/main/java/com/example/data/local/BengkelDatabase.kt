package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        CustomerService::class,
        StockItem::class,
        IncomingStock::class,
        RejectItem::class,
        ExpenseItem::class,
        AttendanceRecord::class,
        CashDeposit::class,
        WorkshopProfile::class,
        StaffMember::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ServiceItemConverters::class)
abstract class BengkelDatabase : RoomDatabase() {
    abstract fun bengkelDao(): BengkelDao

    companion object {
        @Volatile
        private var INSTANCE: BengkelDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BengkelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BengkelDatabase::class.java,
                    "bengkel_qu_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(BengkelDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class BengkelDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.bengkelDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: BengkelDao) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Seed Workshop Profile
            dao.insertOrUpdateProfile(
                WorkshopProfile(
                    id = 1,
                    workshopName = "BENGKEL QU",
                    ownerName = "Hendri",
                    email = "bageurhendri@gmail.com",
                    phone = "085714216556",
                    address = "Jl. Otomotif No. 88, Bandung"
                )
            )

            // Seed Staff
            val staff = listOf(
                StaffMember(name = "Hendri", role = "ADMIN", phone = "085714216556"),
                StaffMember(name = "Pray", role = "KASIR", phone = "081299887766"),
                StaffMember(name = "Day", role = "MEKANIK", phone = "081277665544"),
                StaffMember(name = "Bul", role = "MEKANIK", phone = "081255443322"),
                StaffMember(name = "Man", role = "MEKANIK", phone = "081244332211"),
                StaffMember(name = "Yuli", role = "MEKANIK", phone = "081233221100")
            )
            dao.insertAllStaff(staff)

            // Seed Attendance (matching diagram: PRAY MASUK, DAY MASUK, BUL IZIN, MAN SAKIT, YULI MASUK)
            val attendances = listOf(
                AttendanceRecord(staffName = "PRAY", dateString = todayStr, status = AttendanceStatus.MASUK, timeCheckIn = "07:45"),
                AttendanceRecord(staffName = "DAY", dateString = todayStr, status = AttendanceStatus.MASUK, timeCheckIn = "07:50"),
                AttendanceRecord(staffName = "BUL", dateString = todayStr, status = AttendanceStatus.IZIN, timeCheckIn = "-", notes = "Urusan keluarga"),
                AttendanceRecord(staffName = "MAN", dateString = todayStr, status = AttendanceStatus.SAKIT, timeCheckIn = "-", notes = "Flu demam"),
                AttendanceRecord(staffName = "YULI", dateString = todayStr, status = AttendanceStatus.MASUK, timeCheckIn = "08:00")
            )
            dao.insertAllAttendance(attendances)

            // Seed Completed Services (15 completed to match diagram "selesai: 15")
            val completedList = mutableListOf<CustomerService>()
            for (i in 1..15) {
                completedList.add(
                    CustomerService(
                        queueNumber = i,
                        customerName = "Pelanggan $i",
                        phoneNumber = "081234500$i",
                        plateNumber = "D ${1000 + i * 37} BQ",
                        notes = "Servis rutin dan ganti oli",
                        mechanicName = if (i % 2 == 0) "DAY" else "YULI",
                        status = ServiceStatus.DIBAYAR,
                        discount = 0L,
                        items = listOf(
                            ServiceItemDetail(id = "p_$i", name = "Oli Mesin Matic", price = 65000L, isPart = true),
                            ServiceItemDetail(id = "s_$i", name = "Service Rutin", price = 35000L, isPart = false)
                        ),
                        totalAmount = 100000L,
                        dateEpoch = System.currentTimeMillis() - (15 - i) * 1800000L
                    )
                )
            }
            completedList.forEach { dao.insertCustomerService(it) }

            // Seed Active Services (Queue 16, 17, 18 matching diagram!)
            // 16. Budi (matching diagram details exactly!)
            dao.insertCustomerService(
                CustomerService(
                    queueNumber = 16,
                    customerName = "Budi",
                    phoneNumber = "081387654321",
                    plateNumber = "D 2416 BQ",
                    notes = "Ket chek cvt bulan Oktober",
                    mechanicName = "DAY",
                    status = ServiceStatus.PROSES,
                    discount = 0L,
                    items = listOf(
                        ServiceItemDetail(id = "item_1", name = "oli shel", price = 65000L, isPart = true),
                        ServiceItemDetail(id = "item_2", name = "kanvas", price = 20000L, isPart = true),
                        ServiceItemDetail(id = "item_3", name = "service ringan", price = 35000L, isPart = false)
                    ),
                    totalAmount = 120000L
                )
            )

            // 17. Bambang
            dao.insertCustomerService(
                CustomerService(
                    queueNumber = 17,
                    customerName = "Bambang",
                    phoneNumber = "081298761234",
                    plateNumber = "B 5817 KQU",
                    notes = "Ganti vanbelt dan roller cvt",
                    mechanicName = "YULI",
                    status = ServiceStatus.ANTRIAN,
                    discount = 0L,
                    items = listOf(
                        ServiceItemDetail(id = "item_4", name = "vanbelt matic", price = 110000L, isPart = true),
                        ServiceItemDetail(id = "item_5", name = "service cvt", price = 40000L, isPart = false)
                    ),
                    totalAmount = 150000L
                )
            )

            // 18. Amin
            dao.insertCustomerService(
                CustomerService(
                    queueNumber = 18,
                    customerName = "Amin",
                    phoneNumber = "085712345678",
                    plateNumber = "D 4118 AA",
                    notes = "Tune up injeksi & ganti busi",
                    mechanicName = "PRAY",
                    status = ServiceStatus.ANTRIAN,
                    discount = 0L,
                    items = listOf(
                        ServiceItemDetail(id = "item_6", name = "busi ngk", price = 25000L, isPart = true),
                        ServiceItemDetail(id = "item_7", name = "tune up injeksi", price = 45000L, isPart = false)
                    ),
                    totalAmount = 70000L
                )
            )

            // Seed Stock Items (Komstir, Klakson, Oli Shell, Kanvas, etc.)
            val stockItems = listOf(
                StockItem(name = "KOMSTIR", brand = "Aspira", quality = "Original", status = StockStatus.JUAL_PUTUS, modalPrice = 60000L, sellPrice = 85000L, qty = 12, barcode = "899275310012"),
                StockItem(name = "KLAKSON", brand = "Denso", quality = "OEM", status = StockStatus.JUAL_PUTUS, modalPrice = 45000L, sellPrice = 65000L, qty = 6, barcode = "899275310029"),
                StockItem(name = "OLI SHEL", brand = "Shell AX7", quality = "Original", status = StockStatus.JUAL_PUTUS, modalPrice = 50000L, sellPrice = 65000L, qty = 12, barcode = "899100210036"),
                StockItem(name = "KANVAS REM", brand = "AHM", quality = "Original", status = StockStatus.JUAL_PUTUS, modalPrice = 14000L, sellPrice = 20000L, qty = 20, barcode = "899300410043"),
                StockItem(name = "BUSI", brand = "NGK", quality = "Original", status = StockStatus.JUAL_PUTUS, modalPrice = 15000L, sellPrice = 25000L, qty = 25, barcode = "899400510050"),
                StockItem(name = "VANBELT MATIC", brand = "Gates Power", quality = "Konsinyasi", status = StockStatus.KONSINYASI, modalPrice = 90000L, sellPrice = 120000L, qty = 10, barcode = "899500610067")
            )
            dao.insertAllStockItems(stockItems)

            // Seed Incoming Stocks (Barang Datang: Komstir 12, Klakson 6, Oli Shel 12)
            val incoming = listOf(
                IncomingStock(itemName = "KOMSTIR", qty = 12, status = ApprovalStatus.PENDING),
                IncomingStock(itemName = "KLAKSON", qty = 6, status = ApprovalStatus.PENDING),
                IncomingStock(itemName = "OLI SHEL", qty = 12, status = ApprovalStatus.PENDING)
            )
            dao.insertAllIncomingStocks(incoming)

            // Seed Reject Items (Pengajuan Barang Reject: Oli Bocor 1)
            val rejects = listOf(
                RejectItem(itemName = "OLI SHEL", reason = "BOCOR", qty = 1, status = ApprovalStatus.PENDING)
            )
            dao.insertAllRejectItems(rejects)

            // Seed Expense Items (Belanja: Air Galon 30000, Bensin 20000, Total 50000)
            val expenses = listOf(
                ExpenseItem(name = "AIR GALON", amount = 30000L, status = ApprovalStatus.PENDING),
                ExpenseItem(name = "BENSIN", amount = 20000L, status = ApprovalStatus.PENDING)
            )
            dao.insertAllExpenseItems(expenses)

            // Seed Cash Deposit (Setoran: Total 2.000.000)
            dao.insertCashDeposit(
                CashDeposit(
                    count100k = 15, // 1.500.000
                    count50k = 8,   // 400.000
                    count20k = 4,   // 80.000
                    count10k = 1,   // 10.000
                    count5k = 1,    // 5.000
                    count2k = 2,    // 4.000
                    count1k = 1,    // 1.000
                    count500 = 0,
                    totalAmount = 2000000L
                )
            )
        }
    }
}
