package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ServiceStatus {
    ANTRIAN,
    PROSES,
    SELESAI,
    DIBAYAR
}

enum class AttendanceStatus {
    MASUK,
    IZIN,
    SAKIT,
    ALPA
}

enum class StockStatus {
    JUAL_PUTUS,
    KONSINYASI
}

enum class ApprovalStatus {
    PENDING,
    DISETUJUI,
    KOREKSI
}

enum class SubscriptionTier {
    REGULAR,
    PRO
}

data class ServiceItemDetail(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Long,
    val qty: Int = 1,
    val isPart: Boolean = true // true = sparepart/barang, false = jasa service
)

class ServiceItemConverters {
    @TypeConverter
    fun fromServiceItemList(value: List<ServiceItemDetail>): String {
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("price", item.price)
            obj.put("qty", item.qty)
            obj.put("isPart", item.isPart)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toServiceItemList(value: String): List<ServiceItemDetail> {
        val list = mutableListOf<ServiceItemDetail>()
        if (value.isBlank()) return list
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ServiceItemDetail(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        name = obj.optString("name", ""),
                        price = obj.optLong("price", 0L),
                        qty = obj.optInt("qty", 1),
                        isPart = obj.optBoolean("isPart", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun fromSubscriptionTier(tier: SubscriptionTier?): String {
        return tier?.name ?: SubscriptionTier.REGULAR.name
    }

    @TypeConverter
    fun toSubscriptionTier(value: String?): SubscriptionTier {
        return try {
            if (value != null) SubscriptionTier.valueOf(value) else SubscriptionTier.REGULAR
        } catch (e: Exception) {
            SubscriptionTier.REGULAR
        }
    }
}

@Entity(tableName = "customer_services")
@TypeConverters(ServiceItemConverters::class)
data class CustomerService(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val queueNumber: Int,
    val customerName: String,
    val phoneNumber: String,
    val plateNumber: String,
    val notes: String = "",
    val mechanicName: String = "DAY",
    val status: ServiceStatus = ServiceStatus.ANTRIAN,
    val discount: Long = 0L,
    val items: List<ServiceItemDetail> = emptyList(),
    val totalAmount: Long = 0L,
    val paymentMethod: String = "CASH",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val quality: String, // Original, OEM, Aftermarket
    val status: StockStatus = StockStatus.JUAL_PUTUS, // JUAL PUTUS / KONSINYASI
    val modalPrice: Long,
    val sellPrice: Long,
    val qty: Int,
    val barcode: String = ""
)

@Entity(tableName = "incoming_stocks")
data class IncomingStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val qty: Int,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "reject_items")
data class RejectItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val reason: String, // Bocor, Patah, Rusak Pabrik
    val qty: Int = 1,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "expense_items")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. AIR GALON, BENSIN
    val amount: Long,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffName: String,
    val dateString: String, // YYYY-MM-DD
    val status: AttendanceStatus = AttendanceStatus.MASUK,
    val timeCheckIn: String = "08:00",
    val notes: String = ""
)

@Entity(tableName = "cash_deposits")
data class CashDeposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val count100k: Int = 0,
    val count50k: Int = 0,
    val count20k: Int = 0,
    val count10k: Int = 0,
    val count5k: Int = 0,
    val count2k: Int = 0,
    val count1k: Int = 0,
    val count500: Int = 0,
    val totalAmount: Long = 0L,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "workshop_profile")
data class WorkshopProfile(
    @PrimaryKey val id: Int = 1,
    val workshopName: String = "BENGKEL QU",
    val ownerName: String = "Hendri",
    val email: String = "bageurhendri@gmail.com",
    val phone: String = "081234567890",
    val address: String = "Jl. Otomotif No. 88, Bandung",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.REGULAR,
    val licenseKey: String = "",
    val validUntilEpoch: Long = 0L // 0 = seumur hidup (lifetime)
)

@Entity(tableName = "staff_members")
data class StaffMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String, // ADMIN, KASIR, MEKANIK
    val phone: String = ""
)
