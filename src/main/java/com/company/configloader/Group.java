package com.company.configloader;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Created by vdua on 7/25/2015.
 */
public class Group {

    private final String name;
    private final HashMap<String, Object> properties;
    private static Logger logger = Logger.getLogger(Group.class.getName());

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
        boolean readFromFile = needsReadingFromFile();
        if (!readFromFile) {
            return properties.get(property);
        } else {
            return getPropertyFromFile(property);
        }
    }

    private Object getPropertyFromFile(String prop) {
        File f = getFile();
        FileInputStream fis = null;
        Map<String, Object> propertiesMap = null;
        Lock readLock = null;
        try {
            if (readWriteLock != null) {
                readLock = readWriteLock.readLock();
                readLock.lock();
                logger.info("written to file " + writtenToFile);
                if (!writtenToFile) {
                    if (prop == null) {
                        return new HashMap<String, Object>(properties);
                    } else {
                        return properties.get(prop);
                    }
                }
            }
            propertiesMap = new HashMap<String, Object>();
            fis = new FileInputStream(f);
            int size = 8000;
            byte[] bytes = new byte[size];
            int bytesRead;
            byte[] strBytes = new byte[size];
            int strByteslength = 0;
            String propertyName = null, propertyValue;
            boolean isPropertyName = true, isPropertyValue = false;
            while ((bytesRead = fis.read(bytes)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte currentByte = bytes[i];
                    if (isPropertyName) {
                        if (currentByte == 61) {
                            propertyName = new String(strBytes, 0, strByteslength, "UTF-8");
                            strByteslength = 0;
                            isPropertyValue = true;
                            isPropertyName = false;
                        } else {
                            strBytes[strByteslength++] = currentByte;
                        }
                    } else if (isPropertyValue) {
                        if (currentByte == 10) {
                            if (prop == null || propertyName.equals(prop)) {
                                propertyValue = new String(strBytes, 0, strByteslength, "UTF-8");
                                if (prop == null) {
                                    propertiesMap.put(propertyName, propertyValue);
                                } else {
                                    return propertyValue;
                                }
                            }
                            isPropertyName = true;
                            strByteslength = 0;
                        } else {
                            strBytes[strByteslength++] = currentByte;
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.severe("unable to open property file " + f.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("problem reading property file " + f.getAbsolutePath());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.severe("problem closing property file " + f.getAbsolutePath());
                }
            }
            if (readLock != null) {
                readLock.unlock();
            }
        }
        if (prop == null) {
            return propertiesMap;
        }
        return null;
    }

    private boolean needsReadingFromFile() {
        return directory != null;
    }

    public Map<String, Object> getProperties() {
        boolean readFromFile = needsReadingFromFile();
        return readFromFile ? (Map<String, Object>) getPropertyFromFile(null) : new HashMap<String, Object>(properties);
    }

    private File getFile() {
        return new File(directory + "/" + name + ".conf");
    }

    private String directory;
    private ReadWriteLock readWriteLock;
    private volatile boolean isWriting = false;
    private volatile boolean writtenToFile = false;

    private class GroupWriter implements Runnable {

        public GroupWriter(String dir) {
            readWriteLock = new ReentrantReadWriteLock();
            directory = dir;
        }

        @Override
        public void run() {
            Lock writeLock = null;
            writeLock = readWriteLock.writeLock();
            writeLock.lock();
            Map<String, Object> copiedMap;
            synchronized (Group.this.properties) {
                copiedMap = new HashMap<String, Object>(properties);
                properties.clear();
            }
            File f = getFile();
            FileWriter fw = null;
            try {
                fw = new FileWriter(f, true);
                Set<Map.Entry<String, Object>> entries = copiedMap.entrySet();
                Iterator<Map.Entry<String, Object>> entryIterator = entries.iterator();
                while (entryIterator.hasNext()) {
                    Map.Entry<String, Object> entry = entryIterator.next();
                    fw.write(entry.getKey() + "=");
                    fw.write(entry.getValue().toString());
                    fw.write("\n");
                }
            } catch (IOException e) {
                logger.warning("unable to write properties in file for the group " + getName());
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        logger.severe("unable to close file" + f.getAbsolutePath() +
                                "while writing properties for the group " + getName());
                    }
                }
                writtenToFile = true;
                writeLock.unlock();
            }
        }
    }

    public Runnable getWriter(String directory) {
        return new GroupWriter(directory);
    }
}
