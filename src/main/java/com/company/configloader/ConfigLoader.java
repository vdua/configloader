package com.company.configloader;

import com.company.configloader.cache.Cache;
import com.company.configloader.cache.impl.LRUCache;
import com.company.configloader.ex.InvalidPropertyException;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class ConfigLoader {

    private Group currentGroup;
    private Map<String, Group> groups;
    private String fileName;
    private String[] params;
    private static Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private int initialRead;
    private int cacheSize;
    private int groupsInMemory;
    private Cache resultCache;

    private ConfigLoader(String file, String[] params) {
        this(file, params, -1, 0, -1);
    }

    private ConfigLoader(String file, String[] params, int initialRead, int cacheSize, int groupsInMemory) {
        this.fileName = file;
        this.params = params;
        this.initialRead = initialRead;
        this.groups = new ConcurrentHashMap<String, Group>();
        this.cacheSize = cacheSize;
        if (cacheSize != 0) {
            resultCache = new LRUCache(cacheSize);
        }
        this.groupsInMemory = groupsInMemory;
    }

    private class ConfigReader implements Runnable {

        private final int start;
        private final int end;
        private volatile int status = 0;

        public int getStatus() {
            return status;
        }

        public ConfigReader() {
            this(0);
        }

        public ConfigReader(int start) {
            this(start, -1);
        }

        public ConfigReader(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
                Parser p = new Parser(params);
                String str;
                int group = -1;
                boolean started = false;
                while ((str = br.readLine()) != null) {
                    try {
                        if (!started && p.isGroup(str)) {
                            group++;
                            if (group == start) {
                                started = true;
                                group = 0;
                            }
                        }
                        if (started) {
                            Object obj = p.parseLine(str);
                            if (obj == null) {
                                //do nothing
                            } else if (obj instanceof Object[]) {
                                Object[] objArray = (Object[]) obj;
                                if (currentGroup == null) {
                                    throw new InvalidPropertyException(str);
                                }
                                currentGroup.addProperty((String) objArray[0], objArray[1]);
                            } else if (obj instanceof String) {
                                if (currentGroup != null) {
                                    groups.put(currentGroup.getName(), currentGroup);
                                }
                                group++;
                                String gpName = (String) obj;
                                currentGroup = groups.get(gpName);
                                if (currentGroup == null) {
                                    if (end != -1 && group > end) {
                                        break;
                                    } else {
                                        currentGroup = new Group(gpName);
                                    }
                                }
                            }
                        }
                    } catch (InvalidPropertyException e) {
                        logger.warning(e.getMessage());
                    }
                }
            } catch (FileNotFoundException e) {
                status = 1;
                logger.severe(e.getMessage());
            } catch (UnsupportedEncodingException e) {
                status = 1;
                logger.severe(e.getMessage());

            } catch (IOException e) {
                status = 1;
                logger.severe(e.getMessage());
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        logger.warning("unable to close file " + fileName + " : " + e.getMessage());
                    }
                }
                if (readSignal != null) {
                    readSignal.countDown();
                }
            }
        }
    }

    private CountDownLatch readSignal;

    private void loadFile() throws IOException, InterruptedException, InvalidPropertyException {
        ConfigReader config;
        if (initialRead == 0) {
            config = new ConfigReader();
            Thread t = new Thread(config);
            t.start();
        } else {
            config = new ConfigReader(0, initialRead);
            readSignal = new CountDownLatch(1);
            Thread t = new Thread(config);
            t.start();
            readSignal.await();
            if (config.getStatus() != 0) {
                logger.severe("Unable to read file. exiting out");
                throw new InvalidPropertyException("Unable to read File");
            }
            if (initialRead != -1) {
                config = new ConfigReader(initialRead + 1);
                new Thread(config).start();
                readSignal = new CountDownLatch(1);
            }
        }
    }

    public static ConfigLoader load(String filePath, String[] params) throws IOException, InterruptedException, InvalidPropertyException {
        int initialRead = 20;
        int cacheSize = 0;
        int groupsInMemory = -1;
        ConfigLoader c = new ConfigLoader(filePath, params, initialRead, cacheSize, groupsInMemory);
        c.loadFile();
        return c;
    }

    public Object get(String property) {
        if (readSignal != null) {
            logger.info("still reading the file");
                try {
                    readSignal.await();
                } catch (InterruptedException e) {
                    logger.severe("Exception while getting the property " + property + " " + e.getMessage());
                    return null;
                } finally {
                    readSignal = null;
                }
        }
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
