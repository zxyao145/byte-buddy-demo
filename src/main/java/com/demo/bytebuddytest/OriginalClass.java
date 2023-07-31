package com.demo.bytebuddytest;


import javax.annotation.Resource;

@Intercept
public class OriginalClass {

    public String getString(String val) {
        return "foo " + val;
    }
}
