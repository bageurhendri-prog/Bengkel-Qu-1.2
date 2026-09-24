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
                        populateCleanUserBase(database.bengkelDao())
                        populateMasterKatalog(database.bengkelDao())
                        populateInitialSampleServices(database.bengkelDao())
                    }
                }
            }
        }

        suspend fun populateCleanUserBase(dao: BengkelDao) {
            // Profil Bengkel Baru yang Bersih (Murni dari Nol)
            dao.insertOrUpdateProfile(
                WorkshopProfile(
                    id = 1,
                    workshopName = "Bengkel Qu",
                    ownerName = "Hendri",
                    email = "bageurhendri@gmail.com",
                    phone = "081234567890",
                    address = "Jl. Raya Otomotif No. 88",
                    subscriptionTier = SubscriptionTier.REGULAR
                )
            )

            // Staff Awal Bersih
            val staff = listOf(
                StaffMember(name = "ADMIN", role = "ADMIN", phone = "081234567890"),
                StaffMember(name = "KASIR", role = "KASIR", phone = "081234567891"),
                StaffMember(name = "MEKANIK DAY", role = "MEKANIK", phone = "081234567892")
            )
            dao.insertAllStaff(staff)
        }

        suspend fun populateMasterKatalog(dao: BengkelDao) {
            val existing = dao.getAllStockItemsList()
            if (existing.isEmpty()) {
                val masterParts = listOf(
                    StockItem(name = "OLI AHM MPX 2 0.8L (MATIC)", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 45000L, sellPrice = 55000L, qty = 15, barcode = "8991001001"),
                    StockItem(name = "OLI YAMALUBE MATIC 0.8L", brand = "YAMAHA", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 44000L, sellPrice = 54000L, qty = 12, barcode = "8991001002"),
                    StockItem(name = "OLI SHELL ADVANCE AX7 0.8L", brand = "SHELL", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 52000L, sellPrice = 65000L, qty = 8, barcode = "8991001003"),
                    StockItem(name = "OLI GARDAN / GEAR MATIC 120ML", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 12000L, sellPrice = 18000L, qty = 20, barcode = "8991001004"),
                    StockItem(name = "KAMPAS REM DEPAN BEAT / VARIO", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 38000L, sellPrice = 50000L, qty = 10, barcode = "8991001005"),
                    StockItem(name = "KAMPAS REM BELAKANG TROMOL", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 35000L, sellPrice = 45000L, qty = 2, barcode = "8991001006"),
                    StockItem(name = "BUSI NGK CPR9EA-9", brand = "NGK", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 18000L, sellPrice = 25000L, qty = 18, barcode = "8991001007"),
                    StockItem(name = "ROLLER SET STANDAR BEAT FI", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 42000L, sellPrice = 55000L, qty = 3, barcode = "8991001008"),
                    StockItem(name = "VANBELT SET BEAT FI", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 110000L, sellPrice = 145000L, qty = 4, barcode = "8991001009"),
                    StockItem(name = "FILTER UDARA BEAT ESP", brand = "AHM", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 38000L, sellPrice = 48000L, qty = 5, barcode = "8991001010"),
                    StockItem(name = "BAN LUAR FDR 80/90-14 TUBELESS", brand = "FDR", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 160000L, sellPrice = 200000L, qty = 4, barcode = "8991001011"),
                    StockItem(name = "AKI KERING GS ASTRA GTZ-5S", brand = "GS ASTRA", quality = "ORIGINAL", status = StockStatus.JUAL_PUTUS, modalPrice = 190000L, sellPrice = 240000L, qty = 2, barcode = "8991001012")
                )
                dao.insertAllStockItems(masterParts)
            }
        }

        suspend fun populateInitialSampleServices(dao: BengkelDao) {
            val now = System.currentTimeMillis()
            val sampleServices = listOf(
                CustomerService(
                    queueNumber = 1,
                    customerName = "Budi Santoso",
                    phoneNumber = "081234567890",
                    plateNumber = "B 3456 TGY",
                    notes = "Ganti oli dan servis rutin",
                    mechanicName = "MEKANIK DAY",
                    status = ServiceStatus.PROSES,
                    items = listOf(
                        ServiceItemDetail(id = "s_1", name = "Service Rutin Ringan", price = 35000L, isPart = false),
                        ServiceItemDetail(id = "s_2", name = "OLI AHM MPX 2 0.8L (MATIC)", price = 55000L, isPart = true)
                    ),
                    totalAmount = 90000L,
                    dateEpoch = now - 1800000L // 30 menit lalu
                ),
                CustomerService(
                    queueNumber = 2,
                    customerName = "Agus Pratama",
                    phoneNumber = "081298765432",
                    plateNumber = "D 4512 ABC",
                    notes = "Rem belakang bunyi decit",
                    mechanicName = "MEKANIK DAY",
                    status = ServiceStatus.ANTRIAN,
                    items = listOf(
                        ServiceItemDetail(id = "s_3", name = "Pemeriksaan Rem Belakang", price = 25000L, isPart = false)
                    ),
                    totalAmount = 25000L,
                    dateEpoch = now - 900000L // 15 menit lalu
                ),
                CustomerService(
                    queueNumber = 3,
                    customerName = "Rina Wijaya",
                    phoneNumber = "081311223344",
                    plateNumber = "B 6789 KLS",
                    notes = "Servis CVT & ganti kampas rem - Siap Kasir",
                    mechanicName = "MEKANIK DAY",
                    status = ServiceStatus.SELESAI,
                    items = listOf(
                        ServiceItemDetail(id = "s_4", name = "Service CVT Lengkap", price = 50000L, isPart = false),
                        ServiceItemDetail(id = "s_5", name = "KAMPAS REM DEPAN BEAT / VARIO", price = 50000L, isPart = true)
                    ),
                    totalAmount = 100000L,
                    dateEpoch = now - 600000L // 10 menit lalu
                ),
                CustomerService(
                    queueNumber = 4,
                    customerName = "Doni Kusuma",
                    phoneNumber = "081755667788",
                    plateNumber = "F 1234 XY",
                    notes = "Ganti aki baru",
                    mechanicName = "MEKANIK DAY",
                    status = ServiceStatus.DIBAYAR,
                    paymentMethod = "CASH",
                    items = listOf(
                        ServiceItemDetail(id = "s_6", name = "Jasa Pasang Aki", price = 10000L, isPart = false),
                        ServiceItemDetail(id = "s_7", name = "AKI KERING GS ASTRA GTZ-5S", price = 240000L, isPart = true)
                    ),
                    totalAmount = 250000L,
                    dateEpoch = now - 3600000L // 1 jam lalu
                )
            )

            for (service in sampleServices) {
                dao.insertCustomerService(service)
            }
        }

        /**
         * 10 Data Uji Simulasi Pelanggan untuk uji coba Marketing WA Blast.
         * Nomor telepon menggunakan format simulasi aman (0812-0000-0001 s/d 0812-0000-0010)
         * dan nama berlabel [SIMULASI] sehingga sistem tidak akan meluncurkan WhatsApp asli.
         */
        suspend fun load10SimulationData(dao: BengkelDao) {
            val names = listOf(
                "Budi Santoso", "Agus Setiawan", "Rian Hidayat", "Denny Pratama", "Eko Prasetyo",
                "Fajar Ramadhan", "Gilang Ramadhan", "Hadi Wijaya", "Indra Gunawan", "Joko Susilo"
            )
            val parts = listOf(
                "Oli Shell AX7 & Service Ringan", "Kampas Rem Depan", "Ganti Busi & Filter Udara",
                "Service CVT & Vanbelt", "Oli MPX2 & Gear Oil", "Ganti Ban Luar Belakang",
                "Tune Up Injeksi & Busi", "Ganti Komstir & Oli Shock", "Aki Kering & Klakson", "Service Karburator & Oli Mesin"
            )

            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 60L * 60L * 1000L

            for (i in 0 until 10) {
                val serviceDaysAgo = 35 + (i * 7) // Antara 35 hingga 98 hari yang lalu (> 1 bulan untuk uji WA blast)
                val serviceDate = now - (serviceDaysAgo * oneDayMs)
                val qNum = 100 + i + 1

                val service = CustomerService(
                    queueNumber = qNum,
                    customerName = "${names[i]} [SIMULASI]",
                    phoneNumber = "08120000000${i + 1}",
                    plateNumber = "D ${1100 + i * 23} SIM",
                    notes = "Servis berkala: ${parts[i]}",
                    mechanicName = if (i % 2 == 0) "MEKANIK 1" else "MEKANIK 2",
                    status = ServiceStatus.DIBAYAR,
                    discount = 0L,
                    items = listOf(
                        ServiceItemDetail(id = "sim_p_$i", name = parts[i], price = 75000L + (i * 15000L), isPart = true),
                        ServiceItemDetail(id = "sim_s_$i", name = "Jasa Servis Berkala", price = 35000L, isPart = false)
                    ),
                    totalAmount = 110000L + (i * 15000L),
                    dateEpoch = serviceDate
                )
                dao.insertCustomerService(service)
            }
        }

        suspend fun populateInitialData(dao: BengkelDao) {
            populateCleanUserBase(dao)
        }
    }
}
