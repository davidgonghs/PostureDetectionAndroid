package com.posturedetection.android.data;

import android.app.Application;
import android.net.Uri;

import com.posturedetection.android.data.model.User;

import java.util.Arrays;

public class LoginUser extends Application {

    private static LoginUser login_user = new LoginUser();

    private static User _user;

    private long id;
    private String username;
    private String email;
    private String phone;
    private String password;

    public Uri getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(Uri imgUrl) {
        this.imgUrl = imgUrl;
    }

    private Uri imgUrl;


//private byte[] portrait;

    private String region;
    private String gender;
    private String birthday;

    public LoginUser(String name, String email) {
        this.username = name;
        this.email = email;
    }

    public LoginUser() {

    }

    public static LoginUser getInstance(){
        return login_user;
    }

    public User getUser(){
        return _user;
    }

    public void update(){
        if(_user.getId() == this.id){
            _user.setUsername(this.username);
            _user.setEmail(this.email);
            _user.setPhone(this.phone);
            _user.setImgUrl(this.imgUrl);
            _user.setGender(this.gender);
            _user.setRegion(this.region);
            _user.setBirthday(this.birthday);
            _user.update(_user.getId());
        }
    }

    //reinit
    public void reinit(){
        //update login_user from _user
        login_user.id = _user.getId();
        login_user.username = _user.getUsername();
        login_user.email = _user.getEmail();
        login_user.phone = _user.getPhone();
        login_user.imgUrl = _user.getImgUrl();
        login_user.gender = _user.getGender();
        login_user.region = _user.getRegion();
        login_user.birthday = _user.getBirthday();
    }

    public static boolean logout(){
        if(login_user.id == -1) return false;
        login_user.id = -1;
        login_user.username = null;
        login_user.email = null;
        login_user.phone = null;
        login_user.imgUrl = null;
        login_user.region = null;
        login_user.gender = null;
        login_user.birthday = null;
        return true;
    }

    public boolean login(User user) {
        _user = user;
        login_user.id = user.getId();
        login_user.username = user.getUsername();
        login_user.email = user.getEmail();
        login_user.phone = user.getPhone();
        login_user.imgUrl = user.getImgUrl();
        login_user.region = user.getRegion();
        login_user.gender = user.getGender();
        login_user.birthday = user.getBirthday();
        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", password='" + password + '\'' +
                ", region='" + region + '\'' +
                ", gender='" + gender + '\'' +
                ", birthday='" + birthday + '\'' +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }


}
