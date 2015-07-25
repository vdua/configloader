package com.company.configloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vdua on 7/25/2015.
 */
public class Parser {

    private String groupRegex = "^\\[([a-z]+)\\]$";
    private String propertyRegex = "^([a-z]+)(?:<([a-z]+)>)?\\s*=\\s*(.+)$";
    private Pattern propertyPattern;
    private Pattern groupPattern;
    private List<String> params;

    public Parser(String[] params) {
        propertyPattern = Pattern.compile(propertyRegex);
        groupPattern = Pattern.compile(groupRegex);
        if (params != null) {
            this.params = Arrays.asList(params);
        }
    }

    public Object parseLine(String line){
        line = line.trim();
        if (line.length() == 0 || line.charAt(0) == ';') {
            return null;
        }
        Matcher propertyMatch = propertyPattern.matcher(line);
        if (propertyMatch.matches()) {
            int grpCount = propertyMatch.groupCount();
            String propertyName =propertyMatch.group(1);
            String configName = propertyMatch.group(2);
            String value = propertyMatch.group(3);
            if (configName == null || (params != null && params.contains(configName))) {
                return new String[]{propertyName, value};
            }
        }
        Matcher groupMatcher = groupPattern.matcher(line);
        if (groupMatcher.matches()) {
            return groupMatcher.group(1);
        }
        return null;
    }
}
