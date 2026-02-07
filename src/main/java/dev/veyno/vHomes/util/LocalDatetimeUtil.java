package dev.veyno.vHomes.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDatetimeUtil {

    public static String fromLocalDateTime(LocalDateTime l){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return l.format(formatter);
    }

    public static LocalDateTime fromString(String s){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(s, formatter);
    }

}
