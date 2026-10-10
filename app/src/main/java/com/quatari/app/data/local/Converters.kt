package com.quatari.app.data.local

import androidx.room.TypeConverter
import com.quatari.app.data.local.entity.AccountType
import com.quatari.app.data.local.entity.ProcessingStatus

class Converters {
    @TypeConverter
    fun fromProcessingStatus(status: ProcessingStatus?): String? = status?.name

    @TypeConverter
    fun toProcessingStatus(value: String?): ProcessingStatus? {
        return value?.let {
            runCatching { ProcessingStatus.valueOf(it) }.getOrDefault(ProcessingStatus.PENDING)
        }
    }

    @TypeConverter
    fun fromAccountType(type: AccountType?): String? = type?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? {
        return value?.let {
            runCatching { AccountType.valueOf(it) }.getOrDefault(AccountType.ASSET)
        }
    }
}
