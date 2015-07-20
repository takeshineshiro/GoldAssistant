package com.parttime.utils;

import android.text.TextUtils;

import java.util.List;

/**
 * Created by wyw on 2015/7/19.
 */
public class CheckUtils {
    public static boolean isEmpty(Object[] data) {
        if (data == null || data.length == 0) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(List data) {
        if (data == null || data.size() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String charSequence) {
        if (charSequence == null) {
            return true;
        } else if (TextUtils.isEmpty(charSequence.trim())) {
            return true;
        }

        return false;
    }
}