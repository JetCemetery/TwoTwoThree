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

data class CalendarEvent(
    val accountName: String,
    val title: String
)

object CalendarUtils {
    /**
     * Fetches events for specific dates.
     */
    fun getEventsForDate(context: Context, date: LocalDate, selectedIds: Set<String>): List<CalendarEvent> {
        if (selectedIds.isEmpty()) return emptyList()
        
        val events = mutableListOf<CalendarEvent>()
        val contentResolver = context.contentResolver
        val zoneId = ZoneId.systemDefault()
        
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.OWNER_ACCOUNT
        )
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        val idList = selectedIds.mapNotNull { it.toLongOrNull() }.joinToString(",")
        if (idList.isEmpty()) return emptyList()
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($idList)"

        val cursor = try {
            contentResolver.query(builder.build(), projection, selection, null, null)
        } catch (e: Exception) { null }

        cursor?.use {
            val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val ownerIdx = it.getColumnIndex(CalendarContract.Instances.OWNER_ACCOUNT)
            
            while (it.moveToNext()) {
                events.add(
                    CalendarEvent(
                        accountName = it.getString(ownerIdx) ?: "Local",
                        title = it.getString(titleIdx) ?: "No Title"
                    )
                )
            }
        }
        return events
    }

    fun getEventDates(context: Context, startDate: LocalDate, endDate: LocalDate, selectedIds: Set<String>): Set<LocalDate> {
        if (selectedIds.isEmpty()) return emptySet()
        val eventDates = mutableSetOf<LocalDate>()
        val contentResolver = context.contentResolver
        val zoneId = ZoneId.systemDefault()
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

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
                    if (!current.isBefore(startDate)) eventDates.add(current)
                    current = current.plusDays(1)
                }
            }
        }
        return eventDates
    }

    fun getAvailableCalendars(context: Context): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val contentResolver = context.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = try {
            contentResolver.query(uri, null, null, null, null)
        } catch (e: Exception) { null }

        cursor?.use {
            val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
            val dispNameIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accNameIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val nameIdx = it.getColumnIndex(CalendarContract.Calendars.NAME)
            val ownerIdx = it.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
            val syncIdx = it.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)
            
            while (it.moveToNext()) {
                if (syncIdx != -1 && it.getInt(syncIdx) == 0) continue
                val id = it.getLong(idIdx).toString()
                val name = it.getString(dispNameIdx) ?: it.getString(nameIdx) ?: it.getString(ownerIdx) ?: "Calendar #$id"
                val account = it.getString(accNameIdx) ?: it.getString(ownerIdx) ?: "Local Device"
                calendars.add(CalendarInfo(id, name, account))
            }
        }
        return calendars.distinctBy { it.id }
    }
}
