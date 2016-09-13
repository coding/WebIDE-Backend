/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

/**
 * Created by vangie on 16/2/2.
 */
public abstract class ProjectUtil {

    public static String randomIcon(){
        int min = 1, max = 24;

        return String.format("https://coding.net/static/project_icon/scenery-%d.png",
                min + (int) (Math.random() * ((max - min) + 1)));

    }

}
