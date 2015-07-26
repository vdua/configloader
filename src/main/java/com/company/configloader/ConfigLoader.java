package com.company.configloader;

import com.company.configloader.cache.Cache;
import com.company.configloader.cache.impl.LRUCache;
import com.company.configloader.ex.InvalidPropertyException;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ConfigLoader {

    private Group currentGroup;
    private Map<String, Group> groups;
    private String fileName;
    private String[] params;
    private static Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private int initialRead;
    /**
     * N
     */
    private int groupsInMemory;
    private Cache resultCache;
    private ExecutorService threadPool;

    private ConfigLoader(String file, String[] params) {
        this(file, params, -1, 0, -1);
    }

    /**
     *
     * @param file File path of the config file
     * @param params paramaters for the config property
     * @param initialRead Initial groups to read during load, rest of the groups will be read in another thread.
     *                    pass -1 to read all initially
     * @param cacheSize Size of Cache
     * @param groupsInMemory Number of groups to store in the memory. Rest of the groups will be saved in file.
     */
    private ConfigLoader(String file, String[] params, int initialRead, int cacheSize, int groupsInMemory) {
        this.fileName = file;
        this.params = params;
        this.initialRead = initialRead;
        this.groups = new ConcurrentHashMap<String, Group>();
        if (cacheSize > 0) {
            resultCache = new LRUCache(cacheSize);
        }
        this.groupsInMemory = groupsInMemory;
    }

    private class ConfigReader implements Runnable {

        private final int start;
        private final int nGroups;
        private volatile int status = 0;

        public int getStatus() {
            return status;
        }

        /**
         * Default Constructor. Reads the entire file at once
         */
        public ConfigReader() {
            this(0);
        }

        /**
         * Reads the entire file starting from specified group number
         * @param start group number to read the file from
         */
        public ConfigReader(int start) {
            this(start, -1);
        }

        /**
         * Reads the entire file starting from specified group number and number of groups to read
         * @param start group number to read the file from
         * @param nGroups number of groups to read
         */
        public ConfigReader(int start, int nGroups) {
            this.start = start;
            this.nGroups = nGroups;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                if (groupsInMemory != -1) {
                    threadPool = Executors.newFixedThreadPool(10); // create a thread pool only if required
                }
                br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
                Parser p = new Parser(params);
                String str;
                int group = -1;
                boolean started = false;
                File f = new File(fileName);
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
                                    if (groups.get(currentGroup.getName()) == null) {
                                        groups.put(currentGroup.getName(), currentGroup);
                                        group++;
                                    }
                                    if (group > groupsInMemory) {
                                        threadPool.execute(currentGroup.getWriter(f.getParent() + "/tmp"));
                                    }
                                }
                                String gpName = (String) obj;
                                currentGroup = groups.get(gpName);
                                if (currentGroup == null) {
                                    if (nGroups != -1 && group > nGroups) {
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
                if (currentGroup != null) {
                    if (groups.get(currentGroup.getName()) == null) {
                        groups.put(currentGroup.getName(), currentGroup);
                        group++;
                    }
                    if (group > groupsInMemory) {
                        threadPool.execute(currentGroup.getWriter(f.getParent() + "/tmp"));
                    }
                    currentGroup = null;
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
                if (threadPool != null) {
                    threadPool.shutdown();
                }
            }
        }
    }

    private CountDownLatch readSignal;

    private void loadFile() throws IOException, InterruptedException, InvalidPropertyException {
        ConfigReader config;
        if (initialRead == 0) {
            // read the file in a new thread
            config = new ConfigReader();
            readSignal = new CountDownLatch(1);
            Thread t = new Thread(config);
            t.start();
        } else {
            config = new ConfigReader(0, initialRead);
            readSignal = new CountDownLatch(1);
            Thread t = new Thread(config);
            t.start();
            // wait for the read to complete
            readSignal.await();
            if (config.getStatus() != 0) {
                logger.severe("Unable to read file. exiting out");
                throw new InvalidPropertyException("Unable to read File");
            }
            if (initialRead != -1) {
                config = new ConfigReader(initialRead + 1);
                readSignal = new CountDownLatch(1);
                new Thread(config).start();
            }
        }
    }

    public static ConfigLoader load(String filePath, String[] params) throws IOException, InterruptedException, InvalidPropertyException {

        int initialRead = 20;
        int cacheSize = 1000;
        int groupsInMemory = 100;
        return load(filePath, params, initialRead, cacheSize, groupsInMemory);
    }

    /**
     * Create a configLoader from file
     * @param filePath File path of the config file
     * @param params paramaters for the config property
     * @param initialRead Initial groups to read during load, rest of the groups will be read in another thread.
     *                    pass -1 to read all initially
     * @param cacheSize Size of Cache
     * @param groupsInMemory Number of groups to store in the memory. Rest of the groups will be saved in file.
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws InvalidPropertyException
     */
    public static ConfigLoader load(String filePath, String[] params,
                                    int initialRead, int cacheSize, int groupsInMemory)
            throws IOException, InterruptedException, InvalidPropertyException {
        ConfigLoader c = new ConfigLoader(filePath, params, initialRead, cacheSize, groupsInMemory);
        c.loadFile();
        return c;
    }

    /**
     * Returns the value of the requested property;
     * @param property
     * @return
     */
    public Object get(String property) {
        Object result = null;
        if (resultCache != null && resultCache.containsKey(property)) {
            result = resultCache.get(property);
            return result;
        }
        if (readSignal != null) {
            try {
                synchronized (this) {
                    if (readSignal != null) {
                        readSignal.await();
                    }
                }
            } catch (InterruptedException e) {
                logger.severe("Exception while getting the property " + property + " " + e.getMessage());
                return null;
            } finally {
                synchronized (this) {
                    readSignal = null;
                }
            }
        }
        String props[] = property.split("\\.");
        Group gp = groups.get(props[0]);
        if (gp == null) {
            return null;
        }
        if (props.length == 1) {
            result = gp.getProperties();
        }
        if (props.length == 2) {
            result = gp.getProperty(props[1]);
        }
        if (resultCache != null) {
            resultCache.put(property, result);
        }
        return result;
    }

    public void debugInfo() {
        logger.info("Groups loaded " + groups.size());
        logger.info("Thread shutdown " + threadPool.isTerminated());
    }
}
