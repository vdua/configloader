package com.company.configloader;

import java.util.HashMap;

/**
 * Created by vdua on 7/25/2015.
 */
public class Group {

    private String name;
    private HashMap<String, Object> properties;

    public Group(String name) {
        this.name = name;
        properties = new HashMap<String, Object>();
    }

    public String getName() {
        return this.name;
    }

    public void addProperty(String property, Object value) {
        properties.put(property, value);
    }

    public Object getProperty(String property) {
        return properties.get(property);
    }

    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>(properties);
    }
}
