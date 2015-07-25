package com.company.configloader.ex;

/**
 * Created by vdua on 7/25/2015.
 */
public class InvalidPropertyException extends Exception {

    public InvalidPropertyException(String str) {
        super("Property Name " + str + " at wrong location");
    }
}
