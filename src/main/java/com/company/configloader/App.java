package com.company.configloader;

import sun.security.krb5.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException {
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.load("/users/vdua/desktop/test.conf", null);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        do {
            if ("exit".equals(str)) {
                break;
            } else if (!"".equals(str)) {
                System.out.println(configLoader.get(str));
            }
            System.out.println("Enter string to find. (type 'exit' to exit)");
        } while ((str = br.readLine()) != null);
        br.close();
    }
}
