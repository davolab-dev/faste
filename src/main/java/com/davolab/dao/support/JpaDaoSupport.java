package com.davolab.dao.support;

import com.davolab.util.JsonConverter;
import com.davolab.util.ReflectionUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.util.ProxyUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Jpa dao support.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 * @param <K>  the type parameter
 * @param <V>  the type parameter
 */
public class JpaDaoSupport<T, ID, K, V> extends JpaDaoSupportHandler<T> {

    private static String getCompleteOrderPieces(String[] orders) {
        StringBuilder finalOrder = new StringBuilder();

        for (String order : orders) {
            String[] splitOrder = order.split(":");
            String orderField = "t." + splitOrder[0] + " ";
            String orderType = splitOrder[1];
            String orderPiece = orderField + orderType;

            // Append the orderPiece to the finalOrder with a comma if it's not the first piece
            if (finalOrder.length() > 0) {
                finalOrder.append(", ");
            }
            finalOrder.append(orderPiece);
        }

        // The final order string will contain all the concatenated order pieces
        return " ORDER BY " + finalOrder;
    }

    /**
     * Save entity t.
     *
     * @param entity the entity
     * @return the t
     */
    public T saveEntity(T entity) {
        Assert.notNull(entity, "Entity must not be null.");
        if (getJpaEntityInformation(getJavaType(getDomainClass(entity))).isNew(entity)) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }

    /**
     * Delete.
     *
     * @param entity the entity
     */
    public void deleteEntity(T entity) {
        Assert.notNull(entity, "Entity must not be null!");
        if (!getJpaEntityInformation(getDomainClass(entity)).isNew(entity)) {
            Class<?> type = ProxyUtils.getUserClass(entity);
            T existing = (T) em.find(type, getJpaEntityInformation(getDomainClass(entity)).getId(entity));
            if (existing != null) {
                this.em.remove(this.em.contains(entity) ? entity : this.em.merge(entity));
            } else {
                throw new EmptyResultDataAccessException(String.format("No %s entity exists!", getJavaType(getDomainClass(entity)).getSimpleName()), 1);
            }
        }
    }

    /**
     * Delete by id.
     *
     * @param entity the entity
     * @param id     the id
     */
    public void deleteEntityById(T entity, ID id) {
        Assert.notNull(id, "The given id must not be null!");
        deleteEntity(findEntityById(entity, id).orElseThrow(() -> new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", getJavaType(getMetaDomainClass(entity)).getSimpleName(), id), 1)));
    }

    /**
     * Delete all entities.
     *
     * @param entity the entity
     */
    public void deleteAllEntities(T entity) {
        Iterator<T> iterator = this.findAllEntities(entity).iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            this.deleteEntity(element);
        }
    }

    /**
     * Gets reference.
     *
     * @param entity     the entity
     * @param identifier the identifier
     * @return the reference
     */
    public T getReference(T entity, ID identifier) {
        Class<?> domainClass = getJavaType(getDomainClass(entity));
        return (T) em.getReference(domainClass, identifier);
    }

    /**
     * Find all list.
     *
     * @param entityClass the entity class
     * @return the list
     */
    protected List<T> findAllEntities(T entityClass) {
        Class<?> aClass = getJavaType(getMetaDomainClass(entityClass));
        Class domainClass = getJavaType(aClass);
        TypedQuery<?> query = getQuery(null, (Class<T>) domainClass, Sort.unsorted());
        return (List<T>) query.getResultList();
    }

    /**
     * Find all by ids list.
     *
     * @param entityClass the entity class
     * @param ids         the ids
     * @return the list
     */
    protected List<T> findAllEntitiesByIds(T entityClass, Iterable<ID> ids) {

        Assert.notNull(ids, "Ids must not be null!");

        if (!ids.iterator().hasNext()) {
            return Collections.emptyList();
        }

        Class<?> classFromType = getMetaClassFromType(entityClass);
        if (getJpaEntityInformation(classFromType).hasCompositeId()) {

            List<T> results = new ArrayList<T>();

            for (ID id : ids) {
                findEntityById(entityClass, id).ifPresent(results::add);
            }

            return results;
        }

        Class idType = getJpaEntityInformation(classFromType).getIdType();
        if (idType.equals(Long.class)) {
            List<Long> collect = Streamable.of(ids).stream().map(id -> Long.parseLong(String.valueOf(id))).collect(Collectors.toList());

            ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
            TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

            return query.setParameter(specification.parameter, collect).getResultList();
        } else if (idType.equals(Integer.class)) {
            List<Integer> collect = Streamable.of(ids).stream().map(id -> Integer.parseInt(String.valueOf(id))).collect(Collectors.toList());

            ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
            TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

            return query.setParameter(specification.parameter, collect).getResultList();
        } else {
            Collection<ID> idCollection = Streamable.of(ids).toList();

            ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
            TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

            return query.setParameter(specification.parameter, idCollection).getResultList();
        }


    }

    /**
     * Find by id optional.
     *
     * @param entityClass the entity class
     * @param id          the id
     * @return the optional
     */
    protected Optional<T> findEntityById(T entityClass, ID id) {
        Assert.notNull(entityClass, "Entity(T) cannot be null");
        Assert.notNull(id, "Identifier(ID) cannot be null");
        Class<?> domainClass = getJavaType(getMetaDomainClass(entityClass));
        Class idType = getJpaEntityInformation(domainClass).getIdType();
        if (idType.equals(Long.class)) {
            T t = (T) em.find(domainClass, Long.parseLong(String.valueOf(id)));
            return Optional.ofNullable(t);
        } else if (idType.equals(Integer.class)) {
            T t = (T) em.find(domainClass, Integer.parseInt(String.valueOf(id)));
            return Optional.ofNullable(t);
        } else {
            T t = (T) em.find(domainClass, id);
            return Optional.ofNullable(t);
        }
    }

    /**
     * Find reference optional.
     *
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    protected Optional<T> findReference(T referenceEntity, ID id) {
        Assert.notNull(referenceEntity, "Entity(T) cannot be null");
        Assert.notNull(id, "Identifier(ID) cannot be null");
        Class<?> domainClass = getJavaType(getMetaDomainClass(referenceEntity));
        Class idType = getJpaEntityInformation(domainClass).getIdType();

        if (idType.equals(Long.class)) {
            T t = (T) em.getReference(domainClass, Long.parseLong(String.valueOf(id)));
            return Optional.ofNullable(t);
        } else if (idType.equals(Integer.class)) {
            T t = (T) em.getReference(domainClass, Integer.parseInt(String.valueOf(id)));
            return Optional.ofNullable(t);
        } else {
            T t = (T) em.getReference(domainClass, id);
            return Optional.ofNullable(t);
        }
    }

    /**
     * Find by reference id optional.
     *
     * @param mainEntity      the main entity
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    protected Optional<T> findByReferenceId(T mainEntity, T referenceEntity, ID id) {
        Assert.notNull(mainEntity, "table name cannot be null");
        Assert.notNull(referenceEntity, "reference cannot be null");
        Assert.notNull(id, "id cannot be null");
        String declaredReferenceFieldName = ReflectionUtil.getDeclaredReferenceFieldName((Class) mainEntity, (Class) referenceEntity);
        Optional<T> reference = findReference(referenceEntity, id);
        TypedQuery<?> query = null;
        try {
            query = em.createQuery("SELECT t FROM " + ((Class) mainEntity).getSimpleName() + " t WHERE t." + declaredReferenceFieldName + " = :ref", Class.forName(((Class) mainEntity).getName())).setParameter("ref", reference.get());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        List<?> resultList = query.getResultList();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        T results = (T) objectMapper.convertValue(resultList, List.class);
        return Optional.ofNullable(results);
    }

    /**
     * Paginate page.
     *
     * @param entityClass the entity class
     * @param page        the page
     * @param size        the size
     * @return the page
     */
    protected Page<T> paginate(T entityClass, ID page, ID size) {
        Assert.notNull(entityClass, "table name cannot be null");
        Assert.notNull(page, "page number cannot be null");
        Assert.notNull(size, "page size cannot be null");
        TypedQuery<?> query = null;
        try {
            query = em.createQuery("SELECT t FROM " + ((Class) entityClass).getSimpleName() + " t", Class.forName(((Class) entityClass).getName()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        int s = Integer.parseInt(String.valueOf(size));
        int p = Integer.parseInt(String.valueOf(page));
        query.setFirstResult(p * s);
        query.setMaxResults(s);
        Page paginate = new Page();
        paginate.setPage(p);
        paginate.setPerPage(s);
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List<?> resultList = query.getResultList();
        paginate.setResults(objectMapper.convertValue(resultList, List.class));
        TypedQuery<Long> totalQuery = em.createQuery("SELECT count(t) FROM " + ((Class) entityClass).getSimpleName() + " t", Long.class);
        Long count = totalQuery.getSingleResult();
        paginate.setTotal(count);
        Long l = count / s;
        Integer totalPages = l != null ? l.intValue() : 0;
        paginate.setTotalPages(totalPages);
        return paginate;
    }

    /**
     * Find selected entity fields by id map.
     *
     * @param entityClass the entity class
     * @param id          the id
     * @param fields      the fields
     * @return the map
     */
    protected Map<String, Object> findSelectedEntityFieldsById(T entityClass, ID id, String[] fields) {
        Assert.notNull(entityClass, "Entity(T) cannot be null");
        Assert.notNull(id, "Identifier(ID) cannot be null");
        Assert.notEmpty(fields, "Identifier(ID) cannot be empty");
        TypedQuery<?> query = null;
        try {
            List<String> representation = new ArrayList<>();
            List<String> selection = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            for (String field : fields) {
                representation.add(field);
                selection.add("t." + field);
            }
            Class<?> classFromType = getMetaClassFromType(entityClass);
            ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
            String pkAttribute = ((JpaMetamodelEntityInformation) specification.entityInformation).getIdAttribute().getName();
            Class<?> domainClass = getJavaType(getMetaDomainClass(entityClass));
            Class idType = getJpaEntityInformation(domainClass).getIdType();
            query = em.createQuery("SELECT " + String.join(",", selection) + " FROM " + ((Class) entityClass).getSimpleName() + " t WHERE t." + pkAttribute + "=:id", Object[].class);
            Object castedValue = resolveType(String.valueOf(id), idType);
            query.setParameter("id", castedValue);
            Object singleResult = query.getSingleResult();
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Object convertValue = objectMapper.convertValue(singleResult, singleResult.getClass());
            if (convertValue instanceof Object[]) {
                int i = 0;
                for (Object o : ((Object[]) convertValue)) {
                    map.put(representation.get(i), o);
                    i++;
                }
            } else {
                map.put(fields[0], convertValue);
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object findSelectedEntityFieldsByGivenKeyValue(T entityClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orders) {
        Assert.notNull(entityClass, "Entity(T) cannot be null");
        Assert.notEmpty(keys, "Keys cannot be empty");
        Assert.notEmpty(values, "Values cannot be empty");
        try {
            // Initialize a StringBuilder to build the final order string
            String completeOrderPieces = "";
            if (orders != null) {
                completeOrderPieces = getCompleteOrderPieces(orders);
            }
            List<String> representation = new ArrayList<>();
            List<String> selection = new ArrayList<>();
            boolean isSelectAll = false;
            if (fields == null || fields.length == 0) {
                isSelectAll = true;
            } else {
                for (String field : fields) {
                    representation.add(field);
                    selection.add("t." + field);
                }
            }

            List<Object> castedValues = new ArrayList<>();
            List<Object> sqlConditions = new ArrayList<>();
            Class<?> classFromType = getMetaClassFromType(entityClass);
            for (int i = 0; i < keys.length; i++) {
                Object castedValue = extractFieldValueAsItsType(keys[i], values[i], classFromType);
                String sqlCondition = resolveCondition(conditions[i]);
                if (conditions[i].equals("like") || conditions[i].equals("nlike")) {
                    castedValues.add("%" + castedValue + "%");
                } else {
                    castedValues.add(castedValue);
                }
                sqlConditions.add(sqlCondition);
            }
            if (isSelectAll) {
                Map valueParams = new HashMap();
                StringBuilder countQuery = new StringBuilder();
                countQuery.append("SELECT count(t) FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                StringBuilder sqlQuery = new StringBuilder();
                sqlQuery.append("SELECT t FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                for (int i = 0; i < keys.length; i++) {
                    String valueParam = ":value" + i;
                    valueParams.put(valueParam, castedValues.get(i));
                    String conditionSeparator = "";
                    if (conditionSeparators != null && conditionSeparators.length > 0) {
                        if (conditionSeparators.length != i) {
                            conditionSeparator = " " + conditionSeparators[i] + " ";
                        }
                    }
//                    if (sqlConditions.get(i).equals("BETWEEN")) {
//                        sqlQuery.append(keys[i] + " " + sqlConditions.get(i) + " " + valueParam + " AND " + ":value" + i + 1 + conditionSeparator);
//                    } else {
                    sqlQuery.append("t.").append(keys[i]).append(" ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    countQuery.append("t.").append(keys[i]).append(" ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
//                    }
                }
                if (orders != null) {
                    sqlQuery.append(" ").append(completeOrderPieces);
                }
                TypedQuery<?> query;
                query = em.createQuery(sqlQuery.toString(), Object[].class);
                valueParams.forEach((k, v) -> {
                    query.setParameter(String.valueOf(k).split(":")[1], v);
                });
                if (page != null && size != null) {
                    query.setFirstResult(page * size);
                    query.setMaxResults(size);
                    Page paginate = new Page();
                    paginate.setPage(page);
                    paginate.setPerPage(size);
                    List<?> resultList = query.getResultList();
                    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> collect = list.stream().map(map -> JsonConverter.removeHibernateLazyInitializer(map)).collect(Collectors.toList());
                    paginate.setResults(collect);
                    TypedQuery<Long> totalQuery = em.createQuery(countQuery.toString(), Long.class);
                    valueParams.forEach((k, v) -> {
                        totalQuery.setParameter(String.valueOf(k).split(":")[1], v);
                    });
                    Long count = totalQuery.getSingleResult();
                    paginate.setTotal(count);
                    long l = count / size;
                    int totalPages = (int) l;
                    paginate.setTotalPages((count > 0L && totalPages == 0) ? 1 : totalPages);
                    return paginate;
                } else {
                    List<?> resultList = query.getResultList();
                    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> collect = list.stream().map(map -> JsonConverter.removeHibernateLazyInitializer(map)).collect(Collectors.toList());
                    Map returnMap = new HashMap<>();
                    if (resultType == null || resultType.isEmpty()) {
                        returnMap.put("result", collect);
                    } else if (resultType.equalsIgnoreCase("S")) {
                        Map map = collect.get(0);
                        returnMap.put("result", map);
                    } else if (resultType.equalsIgnoreCase("M")) {
                        returnMap.put("result", collect);
                    }
                    return returnMap;
                }
            } else {
                Map valueParams = new HashMap();
                StringBuilder countQuery = new StringBuilder();
                countQuery.append("SELECT count(t) FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                StringBuilder sqlQuery = new StringBuilder();
                sqlQuery.append("SELECT ").append(String.join(",", selection)).append(" FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                for (int i = 0; i < keys.length; i++) {
                    String valueParam = ":value" + i;
                    valueParams.put(valueParam, castedValues.get(i));
                    String conditionSeparator = "";
                    if (conditionSeparators != null && conditionSeparators.length > 0) {
                        if (conditionSeparators.length != i) {
                            conditionSeparator = " " + conditionSeparators[i] + " ";
                        }
                    }
//                    if (sqlConditions.get(i).equals("BETWEEN")) {
//                        sqlQuery.append(keys[i] + " " + sqlConditions.get(i) + " " + valueParam + " AND " + ":value" + i + 1 + conditionSeparator);
//                    } else {
                    sqlQuery.append("t.").append(keys[i]).append(" ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    countQuery.append("t.").append(keys[i]).append(" ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
//                    }
                }
                if (orders != null) {
                    sqlQuery.append(" ").append(completeOrderPieces);
                }
                TypedQuery<?> query;
                query = em.createQuery(sqlQuery.toString(), Object[].class);
                valueParams.forEach((k, v) -> {
                    query.setParameter(String.valueOf(k).split(":")[1], v);
                });
                if (page != null && size != null) {
                    query.setFirstResult(page * size);
                    query.setMaxResults(size);
                    Page paginate = new Page();
                    paginate.setPage(page);
                    paginate.setPerPage(size);
                    List<?> resultList = query.getResultList();
                    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> mapList = new ArrayList<>();
                    for (Object convertValue : list) {
                        Map<String, Object> map = new HashMap<>();
                        int i = 0;
                        if (convertValue instanceof ArrayList) {
                            for (Object o : (ArrayList) convertValue) {
                                map.put(representation.get(i), o);
                                i++;
                            }
                        } else {
                            map.put(representation.get(i), convertValue);
                        }
                        Map convert = JsonConverter.convert(map);
                        mapList.add(convert);
                    }
                    paginate.setResults(mapList);

                    TypedQuery<Long> totalQuery = em.createQuery(countQuery.toString(), Long.class);
                    valueParams.forEach((k, v) -> {
                        totalQuery.setParameter(String.valueOf(k).split(":")[1], v);
                    });
                    Long count = totalQuery.getSingleResult();
                    paginate.setTotal(count);
                    long l = count / size;
                    int totalPages = (int) l;
                    paginate.setTotalPages((count > 0L && totalPages == 0) ? 1 : totalPages);
                    return paginate;
                } else {
                    List<?> resultList = query.getResultList();
                    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> mapList = new ArrayList<>();
                    for (Object convertValue : list) {
                        Map<String, Object> map = new HashMap<>();
                        int i = 0;
                        if (convertValue instanceof ArrayList) {
                            for (Object o : (ArrayList) convertValue) {
                                map.put(representation.get(i), o);
                                i++;
                            }
                        } else {
                            map.put(representation.get(i), convertValue);
                        }
                        Map convert = JsonConverter.convert(map);
                        mapList.add(convert);
                    }
                    Map returnMap = new HashMap<>();
                    if (resultType == null || resultType.isEmpty()) {
                        returnMap.put("result", mapList);
                    } else if (resultType.equalsIgnoreCase("S")) {
                        Map map = mapList.get(0);
                        returnMap.put("result", map);
                    } else if (resultType.equalsIgnoreCase("M")) {
                        returnMap.put("result", mapList);
                    }
                    return returnMap;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Resolves a given condition string to its corresponding symbol.
     *
     * @param condition The condition string to be resolved.
     * @return The symbol corresponding to the given condition string. Returns "=" as default.
     * Supported condition strings: "equal", "nequal", "gt", "lt", "gteq", "lteq", "like", "nlike".
     */
    private String resolveCondition(String condition) {
        try {
            if (condition.equals("equal")) {
                return "=";
            } else if (condition.equals("nequal")) {
                return "<>";
            } else if (condition.equals("gt")) {
                return ">";
            } else if (condition.equals("lt")) {
                return "<";
            } else if (condition.equals("gteq")) {
                return ">=";
            } else if (condition.equals("lteq")) {
                return "<=";
            } else if (condition.equals("like")) {
                return "LIKE";
            } else if (condition.equals("nlike")) {
                return "NOT LIKE";
            } else if (condition.equals("bt")) {
                return "BETWEEN";
            }
            return "=";
        } catch (Exception e) {
            return "=";
        }
    }

    private Object extractFieldValueAsItsType(String key, String value, Class<?> classFromType) {
        Object castedValue = null;
        for (Field declaredField : classFromType.getDeclaredFields()) {
            String k = String.valueOf(key);
            if (k.contains(".")) {
                String[] parts = k.split("\\.");
                if (declaredField.getName().equals(parts[0])) {
                    if (!declaredField.getType().getPackage().getName().startsWith("java.")) {
                        String replace = k.replace(parts[0] + ".", "");
                        Class<?> aClass = resourceConverter.convertResourceToActualEntity(declaredField.getType().getSimpleName().toLowerCase());
                        return extractFieldValueAsItsType(replace, value, aClass);
                    }
                }
            } else {
                if (declaredField.getName().equals(key)) {
                    Class<?> fieldType = declaredField.getType();
                    castedValue = resolveType(value, fieldType);
                    break;
                }
            }
        }
        return castedValue;
    }

    private Object resolveType(String value, Class<?> fieldType) {
        try {
            Object castedValue = null;
            if (fieldType.equals(Long.class)) {
                castedValue = Long.parseLong(String.valueOf(value));
            } else if (fieldType.equals(Integer.class)) {
                castedValue = Integer.parseInt(String.valueOf(value));
            } else if (fieldType.equals(Boolean.class)) {
                castedValue = Boolean.parseBoolean(String.valueOf(value));
            } else if (fieldType.equals(Date.class)) {
                castedValue = new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).setDateFormat(new SimpleDateFormat("yyyy-MM-dd")).convertValue(String.valueOf(value), Date.class);
            } else if (fieldType.equals(java.sql.Date.class)) {
                castedValue = java.sql.Date.valueOf(String.valueOf(value));
            } else if (fieldType.equals(Character.class)) {
                castedValue = String.valueOf(value).charAt(0);
            } else {
                castedValue = fieldType.cast(value);
            }
            return castedValue;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final class ByIdsSpecification<T> implements Specification<T> {

        private static final long serialVersionUID = 1L;

        private final JpaEntityInformation<T, ?> entityInformation;

        /**
         * The Parameter.
         */
        @Nullable
        ParameterExpression<Collection<?>> parameter;

        /**
         * Instantiates a new By ids specification.
         *
         * @param entityInformation the entity information
         */
        ByIdsSpecification(JpaEntityInformation<T, ?> entityInformation) {
            this.entityInformation = entityInformation;
        }

        @Override
        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

            Path<?> path = root.get(entityInformation.getIdAttribute());
            parameter = (ParameterExpression<Collection<?>>) (ParameterExpression) cb.parameter(Collection.class);
            return path.in(parameter);
        }
    }

}
