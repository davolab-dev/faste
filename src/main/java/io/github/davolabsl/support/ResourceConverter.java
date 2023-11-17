package io.github.davolabsl.support;

import io.github.davolabsl.advice.exception.FasteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author - Shehara
 */

@Component
public class ResourceConverter {

    @Autowired
    private EntityPredicate entityPredicate;

    public Class<?> convertResourceToActualEntity(String resource) {
        try {
            Optional<? extends Class<?>> aClass = entityPredicate.predicateClass(resource);
            return aClass.get();
        } catch (Exception e) {
            throw new FasteException("can not find managed entity class for the give resource - " + resource, e);
        }
    }


}
