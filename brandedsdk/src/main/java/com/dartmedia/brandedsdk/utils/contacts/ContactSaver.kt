package com.dartmedia.brandedsdk.utils.contacts

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.ContactsContract
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


class ContactSaver(
    private val context: Context
) {

    companion object {
        val TAG = ContactSaver::class.java.simpleName.toString()
        fun instance(context: Context): ContactSaver {
            return ContactSaver(context)
        }
    }

    /**
     * Public function to save contact info.
     */
    suspend fun saveContactInfo(
        displayName: String,
        phoneNumber: String,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        try {
            // Add contact details
            val contactId = saveContact(displayName, phoneNumber)

            // Download and add the profile image if provided
            if (!imageUrl.isNullOrEmpty()) {
                downloadAndSaveProfileImage(contactId, imageUrl)
            }

            // Contact saving success
            Result.success("Contact Saved Successfully")
            Log.d(TAG, "Contact Saved Successfully")

        } catch (e: Exception) {
            // Handle exceptions and return error
            Result.failure<String>(e)
            Log.e(TAG, "Failed to save contact : $e")
        }
    }

    /**
     * Saves contact details like name and phone number.
     */
    private fun saveContact(displayName: String, phoneNumber: String): Long {
        val contentValues = ContentValues()
        contentValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
        contentValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)

        val rawContactUri =
            context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues)
        val contactId = ContentUris.parseId(rawContactUri!!)

        // Add name
        val nameValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, contactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

        // Add phone number
        val phoneValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, contactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
            put(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            )
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)

        return contactId
    }

    /**
     * Download and save the profile image for the contact.
     */
    private suspend fun downloadAndSaveProfileImage(contactId: Long, imageUrl: String) {
        val bitmap = withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .submit()
                .get()
        }

        val photoData = bitmapToByteArray(bitmap)

        val photoValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, contactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
            )
            put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoData)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, photoValues)
    }

    /**
     * Convert Bitmap to ByteArray for saving image.
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}
