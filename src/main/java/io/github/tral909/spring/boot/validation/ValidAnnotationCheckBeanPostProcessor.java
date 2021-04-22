package io.github.tral909.spring.boot.validation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidAnnotationCheckBeanPostProcessor implements BeanPostProcessor {

    private final Set<Class<?>> validatedDtoClasses = new HashSet<>();

    @Autowired
    public void setClasses(List<MyValidator> validators) {
        validatedDtoClasses.addAll(
                validators.stream()
                        .map(MyValidator::getRequestBodyClass)
                        .collect(Collectors.toSet()));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = bean.getClass();
        if (type.isAnnotationPresent(RestController.class)) {
            for (Method method : type.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    if (validatedDtoClasses.contains(parameter.getType())
                            && !parameter.isAnnotationPresent(Valid.class)) {
                        throw new RuntimeException("Incoming parameter " + parameter.getType() +
                                " must have @Valid annotation!");
                    }
                }
            }
        }
        return bean;
    }
}
