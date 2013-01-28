package org.springframework.data.simpledb.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.simpledb.repository.support.entityinformation.SimpleDbEntityInformation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.simpledb.annotation.MetadataParser;
import org.springframework.data.simpledb.repository.support.entityinformation.SimpleDbEntityInformation;
import org.springframework.data.simpledb.util.SimpleDBAttributeConverter;
import org.springframework.util.Assert;

public class SimpleDbEntity <T, ID extends Serializable> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDbEntity.class);
	
    private SimpleDbEntityInformation<T, ?> entityInformation;
    private T item;

    public SimpleDbEntity(SimpleDbEntityInformation<T, ?> entityInformation, T item){
        this.entityInformation = entityInformation;
        this.item = item;
    }

    public SimpleDbEntity(SimpleDbEntityInformation<T, ?> entityInformation){
        this.entityInformation = entityInformation;
        try {
            this.item = entityInformation.getJavaType().newInstance();
        } catch (InstantiationException |IllegalAccessException e) {
            throw new MappingException("Could not instantiate object", e);
        }

    }

    public String getDomain(){
        return entityInformation.getDomain();
    }

    public String getItemName(){
        return entityInformation.getItemName(item);
    }

    public Map<String, String> getAttributes(){
        return entityInformation.getAttributes(item);
    }

    public T getItem(){
        return item;
    }

    public void generateIdIfNotSet() {
        if(entityInformation.getItemName(item)==null){
            setId(UUID.randomUUID().toString());
        }
    }

    public void setId(String itemName)  {
        final Field idField;
        try {
            idField = item.getClass().getDeclaredField(entityInformation.getItemNameFieldName(item));
            idField.setAccessible(Boolean.TRUE);
            idField.set(item, itemName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new MappingException("Could not set id field", e);
        }
    }

    public void setAttributes(Map<String, List<String>> attributes) {
    	for(final String key: attributes.keySet()) {
    		/* we only support simple primitives (list should be of size = 1) */
    		final List<String> values = attributes.get(key);
    		Assert.notNull(values);
    		Assert.isTrue(values.size() == 1);
    		
    		try {
				final Field attributesField = item.getClass().getDeclaredField(key);
				
	            attributesField.setAccessible(true);
	            attributesField.set(item, SimpleDBAttributeConverter.toDomainFieldPrimitive(values.get(0), attributesField.getType()));
			} catch (NoSuchFieldException | SecurityException e) {
				throw new MappingException("Could not set attribute field " + key, e);
			} catch (IllegalArgumentException e) {
				LOGGER.error("Illegal argument exception parsing field {}. Exception: {}", key, e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Illegal access exception parsing field {}. Exception: {}", key, e);
			} catch (ParseException e) {
				LOGGER.error("Parsing exception parsing field {}. Exception: {}", key, e);
			}
    	}
    	
//        try {
//            final Field attributesField = item.getClass().getDeclaredField(entityInformation.getAttributesFieldName(item));
//
//            attributesField.setAccessible(true);
//            attributesField.set(item, attributes);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new MappingException("Could not set attribute field", e);
//        }
    }
    
    /**
     * @return a map of serialized field name with the corresponding list of values (if the field is a collection of primitives)
     */
    public Map<String, List<String>> getSerializedPrimitiveAttributes() {
    	final Map<String, List<String>> result = new HashMap<>();
    	
    	for(final Field itemField: MetadataParser.getPrimitiveFields(item)) {
    		final List<String> fieldValues = new ArrayList<>();
    		
    		try {
    			itemField.setAccessible(Boolean.TRUE);
				fieldValues.add(SimpleDBAttributeConverter.toSimpleDBAttributeValue(itemField.get(item)));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("Could not retrieve field value {}. Exception: {} ", itemField.getName(), e);
			}
    		
    		result.put(itemField.getName(), fieldValues);
    	}
    	
    	return result;
    }
}
