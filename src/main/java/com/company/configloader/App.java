package com.company.configloader;

import com.company.configloader.ex.InvalidPropertyException;
import sun.security.krb5.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;

/**
 * Hello world!
 */
public class App {

    public static String time(long start) {
        long end = System.currentTimeMillis();
        sn(start);
        sn(end);
        long nanoSeconds = end - start;
        long seconds = nanoSeconds/(long)Math.pow(10, 3);
        long minutes = seconds / (60);
        long actualSeconds = seconds % 60;
        long hours = minutes / 60;
        long actualMinutes = minutes % 60;
        return hours + ":" + actualMinutes + ":" + actualSeconds + "::" + nanoSeconds;
    }

    public static void sn(Object o) {
        System.out.println(o);
    }

    public static void main(String[] args) throws IOException, InvalidPropertyException, InterruptedException {
        long startTime = System.currentTimeMillis();
        ConfigLoader configLoader = ConfigLoader.load(args[0], new String[]{"staging", "ubuntu"});
        sn(time(startTime));
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
