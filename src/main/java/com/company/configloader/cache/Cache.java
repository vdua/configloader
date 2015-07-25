package com.company.configloader.cache;

/**
 * Created by vdua on 7/25/2015.
 */
public interface Cache {

    public Object get(String key);

    public void put(String key, Object obj);
}
