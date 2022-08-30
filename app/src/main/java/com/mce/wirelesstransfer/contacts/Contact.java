package com.mce.wirelesstransfer.contacts;

import java.util.ArrayList;

public class Contact {

    private String displayName;
    private String photoUrl;
    private ArrayList<ContactPhone> arrayListPhone = new ArrayList<>();
    private ArrayList<ContactEmail> arrayListEmail = new ArrayList<>();


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public ArrayList<ContactPhone> getArrayListPhone() {
        return arrayListPhone;
    }

    public void setArrayListPhone(ArrayList<ContactPhone> arrayListPhone) {
        this.arrayListPhone = arrayListPhone;
    }

    public ArrayList<ContactEmail> getArrayListEmail() {
        return arrayListEmail;
    }

    public void setArrayListEmail(ArrayList<ContactEmail> arrayListEmail) {
        this.arrayListEmail = arrayListEmail;
    }


}
