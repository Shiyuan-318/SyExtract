package com.shiyuan.syextract.util;

public class TimeUtil {

    public static long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return -1;
        }

        timeStr = timeStr.toLowerCase();
        long totalHours = 0;

        StringBuilder number = new StringBuilder();
        for (char c : timeStr.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (Character.isLetter(c)) {
                if (number.length() == 0) {
                    continue;
                }
                int value = Integer.parseInt(number.toString());
                number.setLength(0);

                switch (c) {
                    case 's' -> totalHours += value / 3600.0;
                    case 'm' -> totalHours += value / 60.0;
                    case 'h' -> totalHours += value;
                    case 'd' -> totalHours += value * 24L;
                    case 'w' -> totalHours += value * 24L * 7;
                    case 'M' -> totalHours += value * 24L * 30;
                    case 'y' -> totalHours += value * 24L * 365;
                }
            }
        }

        if (number.length() > 0) {
            totalHours += Integer.parseInt(number.toString());
        }

        return totalHours > 0 ? (long) totalHours : -1;
    }

    public static String formatDuration(long hours) {
        if (hours < 1) {
            return "少于1小时";
        } else if (hours < 24) {
            return hours + "小时";
        } else if (hours < 24 * 7) {
            return (hours / 24) + "天" + (hours % 24 > 0 ? " " + (hours % 24) + "小时" : "");
        } else if (hours < 24 * 30) {
            return (hours / (24 * 7)) + "周";
        } else if (hours < 24 * 365) {
            return (hours / (24 * 30)) + "月";
        } else {
            return (hours / (24 * 365)) + "年";
        }
    }
}
