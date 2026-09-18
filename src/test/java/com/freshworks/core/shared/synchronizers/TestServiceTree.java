package com.freshworks.core.shared.synchronizers;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Disabled
public class TestServiceTree {

    ApplicationContext applicationContext;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @BeforeEach
    public void beforeEach(){
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Autowired
    public TestServiceTree(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testServiceRegistrationWhenServicePathIsNotPresent() throws IllegalAccessException {

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        serviceTree.register("/syncService/traverser");
        int count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(3));


        serviceTree.register("/syncService/traverser/amit/rahul");
        count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(5));
    }

    @Test
    public void testServiceRegistrationWhenChildKeysAreRegistered() throws IllegalAccessException {

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        serviceTree.register("/syncService/traverser");
        int count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(3));


        serviceTree.register("/syncService/traverser/amit");
        count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(4));

        serviceTree.register("/syncService/traverser/amit/aggarwal/freshworks");
        count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(6));
    }

    @Test
    public void testServiceRegistrationDuplicateRegistration() throws IllegalAccessException {

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        serviceTree.register("/syncService/traverser");
        int count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(3));

        serviceTree.register("/syncService/traverser");
        count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(3));

        serviceTree.register("/syncService/traverser/");
        count = serviceTree.getNumberOfRegisteredKeys();
        assertThat(count, is(3));
    }


    @Test
    public void testFindMatchingServiceNodeForGivenServicePathWhenPathIsNotRegistered(){

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);
        ServiceNode root = serviceTree.getRoot();
        List<String> servicePathParts = serviceTree.validateServicePath("/unknown/");
        ServiceNode a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(nullValue()));

        servicePathParts = serviceTree.validateServicePath("/unknown/orphan");
        a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(nullValue()));

        servicePathParts = serviceTree.validateServicePath("/unknown/orphan/");
        a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(nullValue()));
    }


    @Test
    public void testFindMatchingServiceNodeForGivenServicePathWhenPathIsRegistered(){

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        serviceTree.register("/known");
        ServiceNode root = serviceTree.getRoot();
        List<String> servicePathParts = serviceTree.validateServicePath("/known/");
        ServiceNode a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(notNullValue()));


        serviceTree.register("/known/amit");
        servicePathParts = serviceTree.validateServicePath("/known/amit");
        a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(notNullValue()));

        servicePathParts = serviceTree.validateServicePath("/known/amit/");
        a = serviceTree.findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
        assertThat(a, is(notNullValue()));
    }


    @Test
    public void testServiceDeRegistration() throws IllegalAccessException {

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);
        serviceTree.register("/syncService/traverser");
        assertThat(serviceTree.getNumberOfRegisteredKeys(), is(3));

        serviceTree.deRegister("/syncService/traverser");
        serviceTree.deRegister("/syncService");

        assertThat(serviceTree.getNumberOfRegisteredKeys(), is(1));
    }


    @Test
    public void testValidationOfServicePathWhenPathStartsWithForwardSlash(){


        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        List<String> servicePathParts = serviceTree.validateServicePath("/syncService/traverser");

        assertThat(servicePathParts.size(), is(2));
        assertThat(servicePathParts.get(0), is("syncService"));
        assertThat(servicePathParts.get(1), is("traverser"));
    }

    @Test
    public void testValidationOfServicePathWhenPathStartsWithForwardSlashAndEndsWithForwardSlash(){


        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        List<String> servicePathParts = serviceTree.validateServicePath("/syncService/traverser/");

        assertThat(servicePathParts.size(), is(2));
        assertThat(servicePathParts.get(0), is("syncService"));
        assertThat(servicePathParts.get(1), is("traverser"));
    }

    @Test
    public void testValidationOfServicePathWhenPathNotStartsWithForwardSlash(){


        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> serviceTree.validateServicePath("syncService/traverser/"));

        assertThat(exception.getClass(), is(IllegalArgumentException.class));
        assertThat(exception.getMessage(), containsString("service path must start with"));
    }

    @Test
    public void testValidationOfServicePathWhenPathHasMoreThanOneConsecutiveForwardSlash(){


        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("abcde");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(analyticsFactory, AnalyticsFactory.class)
                .add(namespace, NamespaceService.class)
                .build();

        serviceTree.configure(syncServiceContainer);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> serviceTree.validateServicePath("/syncService//traverser/"));

        assertThat(exception.getClass(), is(IllegalArgumentException.class));
        assertThat(exception.getMessage(), containsString("service path can not have consecutive forward slash like //"));
    }

}
