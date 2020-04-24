package com.utopa.canal.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StringUtils {


    /**
     * 判断字符串是不是空或者空串
     * @param charSequence
     * @return
     */
    public static boolean isBlank(CharSequence charSequence){
        int length;
        if(charSequence==null||(length=charSequence.length())==0)return true;
        for (int i = 0; i < length; i++) {
            if(!Character.isWhitespace(charSequence.charAt(i)))return false;
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence charSequence){
      return !isBlank(charSequence);
    }

    /**
     * 字符串是不是等于null
     * @param charSequence
     * @return
     */
    public static boolean isEmpty(CharSequence charSequence){
        int length;
        if(charSequence==null||(length=charSequence.length())==0)return true;
        return false;
    }

    public static boolean isNotEmpty(CharSequence charSequence){
        return !isEmpty(charSequence);
    }

    /**
     * 驼峰到下划线转换
     * @param param 参数
     * @return
     */
    public static String camel4underline(String param) {
        //匹配A-Z之间的一个大大写字母
        Pattern p = Pattern.compile("[A-Z]");
        if (isBlank(param)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(param);
        Matcher mc = p.matcher(param);
        int i = 0;
        while (mc.find()) {
            builder.replace(mc.start() + i, mc.end() + i, "_" + mc.group().toLowerCase());
            i++;
        }

        if ('_' == builder.charAt(0)) {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    /**
     * 下划线转驼峰
     * @param param
     * @return
     */
    public static String underline2camel(String param) {
        //匹配下划线小写字母
        Pattern p = Pattern.compile("_[a-z]");
        if (param == null || param.equals("")) {
            return "";
        }
        StringBuilder builder = new StringBuilder(param);
        Matcher mc = p.matcher(param.toLowerCase());
        int i = 0;
        while (mc.find()) {
            builder.replace(mc.start() - i, mc.end() - i, mc.group(0).substring(1).toUpperCase());
            i++;
        }

        return builder.toString();
    }

}
