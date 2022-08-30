package com.mce.wirelesstransfer.contacts;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.gson.Gson;
import com.mce.wirelesstransfer.ui.MainActivity;

import java.util.ArrayList;

public class PhoneBookManager {

    private static PhoneBookManager instance = null;

    public static PhoneBookManager getInstance()
    {
        if(instance==null)
            instance = new PhoneBookManager();
        return instance;
    }



    @SuppressLint("Range")
    private PhoneBook getReadContacts(MainActivity activity) {

        PhoneBook phoneBook = new PhoneBook();
        ContentResolver cr = activity.getContentResolver();
        Cursor mainCursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (mainCursor != null) {
            while (mainCursor.moveToNext()) {
                Contact contactItem = new Contact();
                @SuppressLint("Range") String id = mainCursor.getString(mainCursor.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String displayName = mainCursor.getString(mainCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
                Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);

                //ADD NAME AND CONTACT PHOTO DATA...
                contactItem.setDisplayName(displayName);
                contactItem.setPhotoUrl(displayPhotoUri.toString());

                if (Integer.parseInt(mainCursor.getString(mainCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    //ADD PHONE DATA...
                    ArrayList < ContactPhone > arrayListPhone = new ArrayList < > ();
                    Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] {
                            id
                    }, null);
                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            ContactPhone phoneContact = new ContactPhone();
                            String phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            phoneContact.setPhone(phone);
                            arrayListPhone.add(phoneContact);
                        }
                    }
                    if (phoneCursor != null) {
                        phoneCursor.close();
                    }
                    contactItem.setArrayListPhone(arrayListPhone);


                    //ADD E-MAIL DATA...
                    ArrayList < ContactEmail > arrayListEmail = new ArrayList <> ();
                    Cursor emailCursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[] {
                            id
                    }, null);
                    if (emailCursor != null) {
                        while (emailCursor.moveToNext()) {
                            ContactEmail emailContact = new ContactEmail();
                            String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            emailContact.setEmail(email);
                            arrayListEmail.add(emailContact);
                        }
                    }
                    if (emailCursor != null) {
                        emailCursor.close();
                    }
                    contactItem.setArrayListEmail(arrayListEmail);

                }
                phoneBook.getContacts().add(contactItem);
            }
        }
        if (mainCursor != null) {
            mainCursor.close();
        }
        return phoneBook;
    }


    public String getJsonPhoneBook(MainActivity activity)
    {
        return new Gson().toJson(getReadContacts(activity));
    }


    public void writePhoneBook(MainActivity activity, PhoneBook phoneBook)
    {
        ArrayList<Contact> contacts = phoneBook.getContacts();
        for(Contact contact : contacts)
        {
            String name = contact.getDisplayName()==null ? "" : contact.getDisplayName();
            String mobile = contact.getArrayListPhone().size()==0 ? "" : contact.getArrayListPhone().get(0).getPhone();
            String email = contact.getArrayListEmail().size()==0 ? "" : contact.getArrayListEmail().get(0).getEmail();

            addContact(activity,name,mobile,email);
        }

        return;

    }


    private void addContact(MainActivity activity, String name, String mobile, String email) {
        ArrayList<ContentProviderOperation> contact = new ArrayList<ContentProviderOperation>();
        contact.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // first and last names
        contact.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
                .build());

        // Contact No Mobile
        contact.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobile)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());


        // Email    `
        contact.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build());

        try {
            ContentProviderResult[] results = activity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, contact);
        } catch (Exception e) {
            Log.d(MainActivity.TAG,"Exception saveing contact "+e.getMessage());
        }
    }


}
