package com.jetcemetery.twotwothree

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CalendarInfo(
    val id: String,
    val name: String,
    val accountName: String
)

object CalendarUtils {
    /**
     * Seasoned Dev Note: We query 'Instances' instead of 'Events' because it 
     * automatically expands recurring events (daily, weekly, etc.).
     */
    fun getEventDates(context: Context, startDate: LocalDate, endDate: LocalDate, selectedIds: Set<String>): Set<LocalDate> {
        if (selectedIds.isEmpty()) return emptySet()
        
        val eventDates = mutableSetOf<LocalDate>()
        val contentResolver = context.contentResolver
        
        val zoneId = ZoneId.systemDefault()
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID
        )
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        // Dev Fix: Ensure we handle IDs as exact Longs in the SQL selection
        val idList = selectedIds.mapNotNull { it.toLongOrNull() }.joinToString(",")
        if (idList.isEmpty()) return emptySet()
        
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($idList)"

        val cursor = try {
            contentResolver.query(builder.build(), projection, selection, null, null)
        } catch (e: Exception) { null }

        cursor?.use {
            val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
            
            while (it.moveToNext()) {
                val bMillis = it.getLong(beginIndex)
                val eMillis = it.getLong(endIndex)
                
                var current = Instant.ofEpochMilli(bMillis).atZone(zoneId).toLocalDate()
                val last = Instant.ofEpochMilli(eMillis).atZone(zoneId).toLocalDate()
                
                while (!current.isAfter(last) && !current.isAfter(endDate)) {
                    if (!current.isBefore(startDate)) {
                        eventDates.add(current)
                    }
                    current = current.plusDays(1)
                }
            }
        }
        return eventDates
    }

    /**
     * Robust Discovery Engine: Scans all columns and filters out deleted/non-synced items.
     */
    fun getAvailableCalendars(context: Context): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val contentResolver = context.contentResolver
        
        // Seasoned Dev Note: Null projection is safer on some device-specific providers 
        // to ensure we don't miss essential metadata.
        val uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = try {
            contentResolver.query(uri, null, null, null, null)
        } catch (e: Exception) { null }

        cursor?.use {
            // Safe index detection for various Android OEM versions
            val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
            val dispNameIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accNameIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val nameIdx = it.getColumnIndex(CalendarContract.Calendars.NAME)
            val ownerIdx = it.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
            val syncIdx = it.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)
            
            while (it.moveToNext()) {
                // Skip calendars that are marked as non-syncing
                if (syncIdx != -1 && it.getInt(syncIdx) == 0) continue

                val id = it.getLong(idIdx).toString()
                
                // Tiered naming logic for Google/Exchange/Local accounts
                val name = it.getString(dispNameIdx) 
                    ?: (if (nameIdx != -1) it.getString(nameIdx) else null)
                    ?: (if (ownerIdx != -1) it.getString(ownerIdx) else null)
                    ?: "Calendar #$id"
                
                val account = (if (accNameIdx != -1) it.getString(accNameIdx) else null)
                    ?: (if (ownerIdx != -1) it.getString(ownerIdx) else null)
                    ?: "Local Device"

                calendars.add(CalendarInfo(id, name, account))
            }
        }
        return calendars.distinctBy { it.id } // Final safety check
    }
}
