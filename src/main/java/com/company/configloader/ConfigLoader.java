package com.company.configloader;

import com.company.configloader.ex.InvalidPropertyException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {

    private Group currentGroup;
    private Map<String, Group> groups = new HashMap<String, Group>();

    public void load(String filePath, String[] params) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            Parser p = new Parser(params);
            String str;
            while ((str = br.readLine()) != null) {
                try {
                    Object obj = p.parseLine(str);
                    if (obj == null) {
                        //do nothing
                    } else if (obj instanceof Object[]) {
                        Object[] objArray = (Object[]) obj;
                        if (currentGroup == null) {
                            throw new InvalidPropertyException(str);
                        }
                        currentGroup.addProperty((String)objArray[0], objArray[1]);
                    } else if (obj instanceof String) {
                        if (currentGroup != null) {
                            groups.put(currentGroup.getName(), currentGroup);
                        }
                        String gpName = (String) obj;
                        currentGroup = groups.get(gpName);
                        if (currentGroup == null) {
                            currentGroup = new Group(gpName);
                        }
                    }
                } catch (InvalidPropertyException e) {

                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    public Object get(String property) {
        String props[] = property.split("\\.");
        Group gp = groups.get(props[0]);
        if (gp == null) {
            return null;
        }
        if (props.length == 1) {
            return gp.getProperties();
        }
        if (props.length == 2) {
            return gp.getProperty(props[1]);
        }
        return null;
    }
}
