package io.github.davolab.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static String getDeclaredReferenceFieldName(Class parentEntity, Class referenceEntity) {
        for (Field declaredField : parentEntity.getDeclaredFields()) {
            if (declaredField.getType().equals(referenceEntity)) {
                return declaredField.getName();
            }
        }
        return "";
    }
}
