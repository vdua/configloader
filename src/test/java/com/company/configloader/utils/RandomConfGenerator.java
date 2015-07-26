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
            int randomCharPosition = (int) (Math.random() * charsAllowed);
            return alphabets[randomCharPosition];
        }

        public List<String> generate(int size) {
            return generate(size, 11);
        }

        public List<String> generate(int size, int maxStringSize) {
            List<String> strings = new ArrayList<String>();
            for (int i = 0; i < size; i++) {
                String str = "";
                int randomStringSize = 4 + (int) (Math.random() * maxStringSize);
                for (int j = 0; j < randomStringSize; j++) {
                    str += getChar();
                }
                strings.add(str);
            }
            return strings;
        }
    }

    public static void generateConfFile(String str, int lines, double groupsPercentage, double commentsPercentage) throws IOException {
        File f = new File(str);
        FileWriter fw = new FileWriter(f);
        int group = 0, property = -1, comment = 0;
        StringGenerator stringGenerator = new StringGenerator();
        int propertyTotal = 0, groupSizeMax = 0, groupSizeMin = lines;
        for (int i = 0; i < lines; i++) {
            double randomNumber = Math.random() * lines;
            if (randomNumber < groupsPercentage * lines / 100) {
                if (property == 0) {
                    i--;
                    continue;
                }
                if (property != -1) {
                    groupSizeMax = Math.max(groupSizeMax, property);
                    groupSizeMin = Math.min(groupSizeMin, property);
                }
                group++;
                property = 0;
                String randomGroup = stringGenerator.generate(1).remove(0);
                fw.write(String.format("[%s]%n", randomGroup));
            } else if (randomNumber < (groupsPercentage + commentsPercentage) * lines / 100) {
                comment++;
                int randomCommentSize = (int) (Math.random() * 80);
                String randomComment = stringGenerator.generate(1, randomCommentSize).remove(0);
                fw.write(String.format(";%s%n",randomComment));
            } else {
                if (group == 0) {
                    i--;
                    continue;
                }
                propertyTotal++;
                property++;
                String randomProperty = stringGenerator.generate(1).remove(0);
                fw.write(String.format("%s = %1$s%n", randomProperty));
            }
        }
        System.out.println(String.format("actual group percentage = %f%%", (group * 100.0) / (lines * 1.0)));
        System.out.println(String.format("actual comment percentage = %f%%", (comment * 100.0) / (lines * 1.0)));
        System.out.println(String.format("actual property percentage = %f%%", (propertyTotal * 100.0) / (lines * 1.0)));
        System.out.println(String.format("groups = %d", group));
        System.out.println(String.format("properties = %d", propertyTotal));
        System.out.println(String.format("avg group size = %d", propertyTotal / group));
        System.out.println(String.format("max group size = %d", groupSizeMax));
        System.out.println(String.format("min group size = %d", groupSizeMin));
        fw.close();
    }

    public static void main(String a[]) throws IOException {
        RandomConfGenerator.generateConfFile("/users/vdua/Desktop/abc.conf", 10000000, 0.001, 80.0);
    }
}
