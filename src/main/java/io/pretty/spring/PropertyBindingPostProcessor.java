package io.pretty.spring;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class PropertyBindingPostProcessor implements BeanPostProcessor,
        BeanFactoryAware,InitializingBean {

    private BeanFactory beanFactory;

    private PropertySources propertySources;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.propertySources == null) {
            this.propertySources = deducePropertySources();
        }
    }

    private PropertySources deducePropertySources() {
        PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
        if (null != configurer){
            return new FlatPropertySources(configurer.getAppliedPropertySources());
        }
        return new MutablePropertySources();
    }

    private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
        // Take care not to cause early instantiation of all FactoryBeans
        if (this.beanFactory instanceof ListableBeanFactory) {
            ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
            Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
                    .getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
                            false);
            if (beans.size() == 1) {
                return beans.values().iterator().next();
            }
            if (beans.size() > 1) {
                System.out.println("Multiple PropertySourcesPlaceholderConfigurer "
                        + "beans registered " + beans.keySet()
                        + ", falling back to Environment");
            }
        }
        return null;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        AutoConfigureProperties annotation = AnnotationUtils
                .findAnnotation(bean.getClass(), AutoConfigureProperties.class);
        if (annotation != null) {
            String prefix = annotation.prefix();
            Map<String,String> map = getMapping(prefix);
            DataBinder<Object> dataBinder = new DataBinder<>(bean,prefix, map);
            try {
                dataBinder.doBind();
            } catch (Exception e) {
                throw new BeanInstantiationException(bean.getClass(),e.getMessage());
            }
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private Map<String,String> getMapping(String prefix){
        PropertySources sources = propertySources;
        FlatPropertySources ps = (FlatPropertySources) sources;
        Iterator<PropertySource<?>> iterator = ps.iterator();
        Map<String,String> m = new HashMap<>();
        while (iterator.hasNext()){
            PropertySource<?> next = iterator.next();
            Object source = next.getSource();
            Map<String,String> map;
            if (source instanceof Map){
                map = (Map<String,String>) source;
                for (String s : map.keySet()) {
                    if (s.startsWith(prefix)){
                        m.put(s, map.get(s));
                    }
                }
            }
        }
        return m;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private static class FlatPropertySources implements PropertySources {

        private PropertySources propertySources;

        FlatPropertySources(PropertySources propertySources) {
            this.propertySources = propertySources;
        }

        @Override
        public Iterator<PropertySource<?>> iterator() {
            MutablePropertySources result = getFlattened();
            return result.iterator();
        }

        @Override
        public boolean contains(String name) {
            return get(name) != null;
        }

        @Override
        public PropertySource<?> get(String name) {
            return getFlattened().get(name);
        }

        private MutablePropertySources getFlattened() {
            MutablePropertySources result = new MutablePropertySources();
            for (PropertySource<?> propertySource : this.propertySources) {
                flattenPropertySources(propertySource, result);
            }
            return result;
        }

        private void flattenPropertySources(PropertySource<?> propertySource,
                                            MutablePropertySources result) {
            Object source = propertySource.getSource();
            if (source instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
                for (PropertySource<?> childSource : environment.getPropertySources()) {
                    flattenPropertySources(childSource, result);
                }
            }
            else {
                result.addLast(propertySource);
            }
        }

    }

    class DataBinder<T>{

        private T target;
        private String prefix;
        private Map<String, String> map;

        public DataBinder(T target, String prefix, Map<String, String> map){
            this.target = target;
            this.prefix = prefix;
            this.map = map;
        }


        private void doBind() throws Exception{
            Method[] methods = target.getClass().getDeclaredMethods();
            for (Method method : methods) {
                String name = method.getName();
                if (name.startsWith("set")) {// javabean discriptor
                    String s = name.substring(3, name.length());
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        if (entry.getKey().toLowerCase().contains(s.toLowerCase())) {
                            method.invoke(target,entry.getValue());
                            break;
                        }
                    }
                }
            }
        }
    }
}
