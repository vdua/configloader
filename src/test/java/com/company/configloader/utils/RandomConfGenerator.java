package com.company.configloader.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vdua on 7/25/2015.
 */
public class RandomConfGenerator {
    private static class StringGenerator {
        private char[] alphabets = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        private char getChar() {
            int charsAllowed = alphabets.length;
            int randomCharPosition = (int)(Math.random()*charsAllowed);
            return alphabets[randomCharPosition];
        }

        public List<String> generate(int size) {
            List<String> strings = new ArrayList<String>();
            int stringSizes[] = new int[11];
            for (int i = 0; i < 11; i++) {
                stringSizes[i] = 0;
            }
            for (int i = 0; i < size; i++) {
                String str = "";
                int randomStringSize = 4 + (int)(Math.random() * 7);
                stringSizes[randomStringSize]++;
                for (int j = 0; j < randomStringSize; j++) {
                    str += getChar();
                }
                strings.add(str);
            }
            for (int i = 4; i <= 10; i++) {
                System.out.println(String.format("%d %d%%", i, (stringSizes[i]*100)/size ));
            }
            return strings;
        }
    }

    public static void generateConfFile(String str, int lines, int groupsPercentage) throws IOException {
        File f = new File(str);
        FileWriter fw = new FileWriter(f);
        int group = 0, property = -1;
        StringGenerator stringGenerator = new StringGenerator();
        List<String> propertyNames = stringGenerator.generate(2 * (lines - (lines*groupsPercentage) / 100));
        List<String> groupNames = stringGenerator.generate(2 * lines * groupsPercentage/100);
        for (int i = 0; i < lines; i++) {
            int randomNumber = (int) (Math.random() * 100);
            if ( randomNumber < groupsPercentage) {
                if (property == 0) {
                    i--;
                    continue;
                }
                group++;
                property = 0;
                int randomGroupNumber = (int) (Math.random() * groupNames.size());
                if (groupNames.size() == 0) {
                    System.out.println("groups extinguished");
                    continue;
                }
                String randomGroup = groupNames.remove(randomGroupNumber);
                fw.write(String.format("[%s]%n", randomGroup));
            } else {
                if (group == 0) {
                    i--;
                    continue;
                }
                property++;
                int randomPropertyNumber = (int) (Math.random() * propertyNames.size());
                if (propertyNames.size() == 0) {
                    System.out.println("properties extinguished");
                    continue;
                }
                String randomProperty = propertyNames.remove(randomPropertyNumber);
                fw.write(String.format("%s = %1$s%n", randomProperty));
            }
        }
        System.out.println(String.format("actual group percentage = %f%%", (group * 100.0)/ (lines * 1.0)));
        fw.close();
    }

    public static void main(String a[]) throws IOException {
        RandomConfGenerator.generateConfFile("/users/vdua/Desktop/abc.conf",1000000, 5);
    }
}