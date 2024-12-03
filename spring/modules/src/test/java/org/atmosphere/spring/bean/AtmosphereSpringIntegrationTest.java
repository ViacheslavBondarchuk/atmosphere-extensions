package org.atmosphere.spring.bean;

import jakarta.servlet.ServletConfig;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;

import static org.atmosphere.cpr.BroadcasterLifeCyclePolicy.ATMOSPHERE_RESOURCE_POLICY.IDLE_EMPTY_DESTROY;

/**
 * author: vbondarchuk
 * date: 11/15/2024
 * time: 9:41 PM
 **/


@ExtendWith(SpringExtension.class)
public class AtmosphereSpringIntegrationTest {
    @Autowired
    private AtmosphereFramework framework;
    @Autowired
    private BroadcasterFactory broadcasterFactory;
    @Autowired
    private AtmosphereSpringContext atmosphereSpringContext;

    /**
     * This test shows that a bean of BroadcasterFactory is not initialized till invoke of
     * {@link  AtmosphereSpringServlet#init(ServletConfig)}
     * So, the BroadcasterFactory was initialized and configured once during instantiation, and it will not be reconfigured
     */
    @Test
    public void testUnconfigurableAtmosphereFrameworkBeforeServletInitialization() throws Exception {
        Field broadcasterLifeCyclePolicyField =
                getDeclaredField(AtmosphereFramework.class, "broadcasterLifeCyclePolicy");
        String broadcastLifeCyclePolicy = (String) broadcasterLifeCyclePolicyField.get(framework);

        Assertions.assertFalse(framework.initialized());
        Assertions.assertEquals(atmosphereSpringContext.getConfig()
                .get(ApplicationConfig.BROADCASTER_LIFECYCLE_POLICY), broadcastLifeCyclePolicy);
    }

    /**
     * This test shows that {@link AtmosphereSpringContext#getInitParameterNames()} has incorrect implementation of this method.
     * This method should return a keySet but not values
     */
    @Test
    public void testGetInitParametersNamesOfAtmosphereSpringContext() {
        Enumeration<String> names = atmosphereSpringContext.getInitParameterNames();
        Assertions.assertEquals(ApplicationConfig.BROADCASTER_LIFECYCLE_POLICY, names.nextElement());
    }


    private static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    @TestConfiguration
    public static class AtmosphereTestConfiguration {

        @Bean
        public ServletRegistrationBean<AtmosphereSpringServlet> servletRegistrationBean() {
            AtmosphereSpringServlet servlet = new AtmosphereSpringServlet();
            ServletRegistrationBean<AtmosphereSpringServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/atmosphere/*");
            registrationBean.setLoadOnStartup(0);
            registrationBean.setBeanName("AtmosphereServlet");
            registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registrationBean;
        }

        @Bean
        public AtmosphereSpringContext atmosphereSpringContext() {
            AtmosphereSpringContext context = new AtmosphereSpringContext();
            HashMap<String, String> config = new HashMap<>();
            config.put(ApplicationConfig.BROADCASTER_LIFECYCLE_POLICY, IDLE_EMPTY_DESTROY.name());
            context.setConfig(config);
            return context;
        }

        @Bean
        public AtmosphereFramework atmosphereFramework() {
            return new AtmosphereFramework(false, false);
        }

        @Bean
        public BroadcasterFactory broadcasterFactory(AtmosphereFramework framework) {
            return framework.getBroadcasterFactory();
        }


    }
}
