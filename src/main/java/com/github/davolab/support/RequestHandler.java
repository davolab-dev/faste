package com.github.davolab.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.davolab.advice.exception.FasteException;
import com.github.davolab.dao.GenericDao;
import com.github.davolab.dao.support.Page;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The type Request handler.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 */
@Component
public class RequestHandler<T, ID extends Serializable, K, V> {

//    @Autowired
//    private ObjectMapper objectMapper;

    @Autowired
    private GenericDao genericDao;

    /**
     * Save handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T saveHandler(Class<?> aClass, T entity) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object value = objectMapper.convertValue(entity, aClass);
        return (T) genericDao.save(value);
    }

    /**
     * Save all handler t.
     *
     * @param aClass   the a class
     * @param entities the entities
     * @return the t
     */
    public T saveAllHandler(Class<?> aClass, T entities) {
        List<?> objects = collectEntities(entities, aClass);
        return (T) genericDao.saveAll(objects);
    }


    /**
     * Find by id handler t.
     *
     * @param aClass the a class
     * @param id     the id
     * @return the t
     */
    public T findByIdHandler(Class<?> aClass, ID id) {
        Optional<T> optionalData = genericDao.findById(aClass, id);
        if (optionalData.isPresent()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return (T) objectMapper.convertValue(optionalData.get(), optionalData.get().getClass());
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    /**
     * Find by reference id handler t.
     *
     * @param mainEntity      the main entity
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the t
     */
    public T findByReferenceIdHandler(Class<?> mainEntity, Class<?> referenceEntity, ID id) {
        Optional<T> data = genericDao.findByReferenceId(mainEntity, referenceEntity, id);
        if (data.isPresent()) {
            return data.get();
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    /**
     * Find all handler t.
     *
     * @param aClass the a class
     * @return the t
     */
    public T findAllHandler(Class<?> aClass) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List all = genericDao.findAll(aClass);
        if (!all.isEmpty()) {
            List list = objectMapper.convertValue(all, List.class);
            return (T) list;
        } else {
            throw new FasteException("unable to find resources");
        }
    }

    /**
     * Find all by ids handler t.
     *
     * @param aClass the a class
     * @param ids    the ids
     * @return the t
     */
    public T findAllByIdsHandler(Class<?> aClass, ID[] ids) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List<ID> idList = Arrays.stream(ids).collect(Collectors.toList());
        List all = genericDao.findAllByIds(aClass, idList);
        if (!all.isEmpty()) {
            List list = objectMapper.convertValue(all, List.class);
            return (T) list;
        } else {
            throw new FasteException("unable to find resources with given ids - " + ids);
        }
    }

    /**
     * Paginate handler page.
     *
     * @param entityClass the entity class
     * @param page        the page
     * @param size        the size
     * @return the page
     */
    public Page<T> paginateHandler(T entityClass, ID page, ID size) {
        Page paginate = genericDao.paginate(entityClass, page, size);
        return paginate;
    }


    /**
     * Update handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T updateHandler(Class<?> aClass, T entity) {
        try {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Object value = objectMapper.convertValue(entity, aClass);
            T update = (T) genericDao.update(value);
            return (T) objectMapper.convertValue(update, update.getClass());
        } catch (Exception e) {
            throw new FasteException("exception occurred while performing updateHandler", e);
        }
    }

    /**
     * Dynamic Update handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T dynamicUpdateHandler(Class<?> aClass, T entity, ID id) {
        try {
            // Load the existing entity from the database
            Optional existingEntity = genericDao.findById(aClass, id);// Assuming you have a method to fetch the entity by its ID

            if (!existingEntity.isPresent()) {
                throw new NotFoundException("Entity not found"); // Handle this appropriately
            }

            // Use ObjectMapper to map only the non-null properties from entity to existingEntity
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Object updatedValue = objectMapper.updateValue(existingEntity.get(), entity);

            // Update the existingEntity in the database

            return (T) genericDao.update(updatedValue);
        } catch (Exception e) {
            throw new FasteException("Exception occurred while performing updateHandler", e);
        }
    }

    /**
     * Update all handler t.
     *
     * @param aClass   the a class
     * @param entities the entities
     * @return the t
     */
    public T updateAllHandler(Class<?> aClass, T entities) {
        try {
            List<?> objects = collectEntities(entities, aClass);
            T updateAll = (T) genericDao.updateAll(objects);
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return (T) objectMapper.convertValue(updateAll, List.class);
        } catch (Exception e) {
            throw new FasteException("exception occurred while performing updateAllHandler", e);
        }
    }

    /**
     * Delete handler.
     *
     * @param aClass the a class
     * @param entity the entity
     */
    public void deleteHandler(Class<?> aClass, T entity) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object value = objectMapper.convertValue(entity, aClass);
        genericDao.delete(value);
    }

    /**
     * Delete by id handler.
     *
     * @param aClass the a class
     * @param id     the id
     */
    public void deleteByIdHandler(Class<?> aClass, ID id) {
        genericDao.deleteById(aClass, id);
    }

    /**
     * Delete all handler.
     *
     * @param aClass the a class
     */
    public void deleteAllHandler(Class<?> aClass) {
        genericDao.deleteAll(aClass);
    }


    private List<?> collectEntities(T entities, Class<?> aClass) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(((ArrayList) entities).iterator(), Spliterator.ORDERED), true);
        return (List<?>) stream.map(o -> objectMapper.convertValue(o, aClass)).collect(Collectors.toList());
    }

    public Map findSelectedByIdHandler(Class<?> aClass, ID id, String[] fields) {
        Map data = genericDao.findSelectedById(aClass, id, fields);
        if (!data.isEmpty()) {
            return data;
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    public Object findSelectedByGivenKeyValueHandler(Class<?> aClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orderBy) {
        Object data = genericDao.findSelectedByGivenKeyValue(aClass, keys, values, fields, conditions, conditionSeparators, page, size, resultType, orderBy);
        if (data != null) {
            return data;
        } else {
            throw new FasteException("unable to find resource for the given " + keys + " - " + values);
        }
    }
}
