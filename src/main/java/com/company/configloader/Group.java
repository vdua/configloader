package com.company.configloader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by vdua on 7/25/2015.
 */
public class Group {

    private final String name;
    private final HashMap<String, Object> properties;
    private String file;
    private volatile boolean isWriting = false;

    public Group(String name) {
        this.name = name;
        properties =  new HashMap<String, Object>();
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

    private class GroupWriter implements Runnable {

        private String directory;
        public GroupWriter(String directory) {
            this.directory = directory;
        }

        @Override
        public void run() {
            Map<String, Object> copiedMap;
            synchronized(Group.this.properties) {
                copiedMap = getProperties();
                properties.clear();
            }
            synchronized (Group.this) {
                File f = new File(directory +"/"+name+".conf");
                FileWriter fw = null;
                try {
                    fw = new FileWriter(f, true);
                    Set<Map.Entry<String, Object>> entries = copiedMap.entrySet();
                    Iterator<Map.Entry<String,Object>> entryIterator = entries.iterator();
                    while(entryIterator.hasNext()) {
                        Map.Entry<String, Object> entry = entryIterator.next();
                        fw.write(entry.getKey() + "=");
                        fw.write(entry.getValue().toString());
                        fw.write("\n");
                    }
                    System.out.println("group written " +name);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e) {

                        }
                    }
                }
            }
        }
    }

    public Runnable getWriter(String directory) {
        return new GroupWriter(directory);
    }
}
