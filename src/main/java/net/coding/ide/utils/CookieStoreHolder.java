/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

/**
 * Created by vangie on 14/12/23.
 */
public class CookieStoreHolder {

    private static final ThreadLocal<CookieStore> cookieStoreHolder = new ThreadLocal<CookieStore>();

    public static void clearCookieStore() {
        cookieStoreHolder.remove();
    }

    public static CookieStore getCookieStore(boolean createIfNeed) {
        CookieStore cookieStore =  cookieStoreHolder.get();

        if(createIfNeed && cookieStore == null){
            cookieStore = new BasicCookieStore();
            cookieStoreHolder.set(cookieStore);
        }

        return cookieStore;
    }

    public static void setCookieStore(CookieStore cookieStore) {
        cookieStoreHolder.set(cookieStore);
    }

}
