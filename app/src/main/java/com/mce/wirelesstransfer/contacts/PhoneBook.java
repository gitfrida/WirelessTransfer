package com.mce.wirelesstransfer.contacts;

import java.util.ArrayList;

public class PhoneBook {

    private ArrayList<Contact> contacts = new ArrayList<>();

    public ArrayList<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(ArrayList<Contact> contacts) {
        this.contacts = contacts;
    }
}
