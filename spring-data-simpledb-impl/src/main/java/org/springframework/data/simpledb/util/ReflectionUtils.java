package org.springframework.data.simpledb.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.util.Assert;

public final class ReflectionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        //utility class
    }

    public static Object callGetter(Object obj, String fieldName) {
        try {
            Method getterMethod = retrieveGetterFrom(obj.getClass(), fieldName);
            return getterMethod.invoke(obj);

        } catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MappingException("Could not call getter for field " + fieldName + " in class:  " + obj.getClass(), e);
        }
    }

    public static void callSetter(Object obj, String fieldName, Object fieldValue) {
        try {
            Method setterMethod = retrieveSetterFrom(obj.getClass(), fieldName);
            setterMethod.invoke(obj, fieldValue);

        } catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MappingException("Could not call getter for field " + fieldName + " in class:  " + obj.getClass(), e);
        }
    }

    /**
     * This method checks if the declared Field is accessible through getters and setters methods
     * Fields which have only setters OR getters and NOT both are discarded from serialization process
     */
    public static <T> boolean hasDeclaredGetterAndSetter(final Field field, Class<T> entityClazz) {
        boolean hasDeclaredAccessorsMutators = true;

        Method getter = retrieveGetterFrom(entityClazz, field.getName());
        Method setter = retrieveSetterFrom(entityClazz, field.getName());

        if(getter == null || setter == null) {
            hasDeclaredAccessorsMutators = false;
        }

        return hasDeclaredAccessorsMutators;
    }

    private static <T> Method retrieveGetterFrom(final Class<T> entityClazz, final String fieldName) {
        Method getterMethod;
        try {
            final PropertyDescriptor descriptor = new PropertyDescriptor(fieldName, entityClazz);
            getterMethod = descriptor.getReadMethod();
        } catch (IntrospectionException e) {
            getterMethod = null;
            LOG.debug("Field {} has not declared getter method", fieldName, e);
        }
        return getterMethod;
    }

    private static <T> Method retrieveSetterFrom(final Class<T> entityClazz, final String fieldName) {
        Method setterMethod;

        try {
            final PropertyDescriptor descriptor = new PropertyDescriptor(fieldName, entityClazz);
            setterMethod = descriptor.getWriteMethod();
        } catch (IntrospectionException e) {
            setterMethod = null;
            LOG.debug("Field {} has not declared setter method", fieldName, e);
        }
        return setterMethod;
    }
}