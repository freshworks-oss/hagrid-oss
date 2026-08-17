package com.freshworks.core.shared.analytics;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestAnalyticsService {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AnalyticsFactory analyticsFactory;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(AnalyticsService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    public void testConfiguringNamespaceIsMandatory(){

        AnalyticsService analyticsService = new AnalyticsService(null, null);

        try{
            analyticsService.debugLogEvent("SOME_DEBUG_EVENT", "method", "method_name");
            assertThat(true, is(false));
        }
        catch (Exception e){
            assertThat(true, is(true));
        }

        try{
            analyticsService.infoLogEvent("SOME_INFO_EVENT", "method", "method_name");
            assertThat(true, is(false));
        }
        catch (Exception e){
            assertThat(true, is(true));
        }

        try{
            analyticsService.warnLogEvent("SOME_WARN_EVENT", "method", "method_name");
            assertThat(true, is(false));
        }
        catch (Exception e){
            assertThat(true, is(true));
        }

        try{
            analyticsService.errorLogEvent("SOME_ERROR_EVENT", "method", "method_name");
            assertThat(true, is(false));
        }
        catch (Exception e){
            assertThat(true, is(true));
        }

        try{
            analyticsService.meterCounter("SOME_METER_COUNTER", "method", "method_name");
            assertThat(true, is(false));
        }
        catch (Exception e){
            assertThat(true, is(true));
        }
    }



    @Nested
    class TestAnalyticsInitialisationForSameNameSpaceUseCase{

        @Test
        public void testRightNumberOfWarnEventsAreCaptured(){
            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace = UUID.randomUUID().toString();
            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            analyticsService.warnLogEvent("warn_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");

            assertThat(analyticsService.anyWarnEvent(), is(true));
            assertThat(analyticsService.howManyWarnEvent(), is(1.0));
        }

        @Test
        public void testRightNumberOfErrorEventsAreCaptured(){
            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace = UUID.randomUUID().toString();
            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            analyticsService.errorLogEvent("error_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");

            assertThat(analyticsService.anyErrorEvent(), is(true));
            assertThat(analyticsService.howManyErrorEvent(), is(1.0));
        }


    }

    @Nested
    class TestAnalyticsInitialisationForMultipleSpaceUseCase{

        @Test
        public void testRightNumberOfWarnEventsAreCaptured(){

            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace = UUID.randomUUID().toString();
            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            analyticsService.warnLogEvent("warn_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");

            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace2 = UUID.randomUUID().toString();
            AnalyticsService analyticsService2 = analyticsFactory.getAnalyticsService(namespace2);
            analyticsService2.warnLogEvent("warn_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");
            analyticsService2.warnLogEvent("warn_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");

            assertThat(analyticsService.anyWarnEvent(), is(true));
            assertThat(analyticsService.howManyWarnEvent(), is(1.0));

            assertThat(analyticsService2.anyWarnEvent(), is(true));
            assertThat(analyticsService2.howManyWarnEvent(), is(2.0));

            assertThat(analyticsService2.hashCode(), is(not(analyticsService.hashCode())));
        }

        @Test
        public void testRightNumberOfErrorEventsAreCaptured(){
            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace = UUID.randomUUID().toString();
            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            analyticsService.errorLogEvent("error_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");

            analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
            String namespace2 = UUID.randomUUID().toString();
            AnalyticsService analyticsService2 = analyticsFactory.getAnalyticsService(namespace2);
            analyticsService2.errorLogEvent("error_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");
            analyticsService2.errorLogEvent("error_event_name", "method_name", "testRightNumberOfDebugEventsAreCaptured");


            assertThat(analyticsService.anyErrorEvent(), is(true));
            assertThat(analyticsService.howManyErrorEvent(), is(1.0));

            assertThat(analyticsService2.anyErrorEvent(), is(true));
            assertThat(analyticsService2.howManyErrorEvent(), is(2.0));

            assertThat(analyticsService2.hashCode(), is(not(analyticsService.hashCode())));
        }
    }



    @Test
    void testAnalyticsServiceIsRemovedWhenSigTermIsCalled(){

        analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        String namespace = UUID.randomUUID().toString();
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);

        assertThat(analyticsFactory.singletonHashMap.containsKey(namespace), is(true));

        analyticsFactory.destroy(namespace);

        assertThat(analyticsFactory.singletonHashMap.containsKey(namespace), is(false));
    }



    @Test
    void testInfoEvent_IncludesCallerInfo() {
        // Given
        String eventName = "TEST_EVENT {}";
        analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        String namespace = UUID.randomUUID().toString();
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
        analyticsService.infoLogEvent(eventName, "key", "value");

        assertFalse(listAppender.list.isEmpty(), "No log events were captured.");

        ILoggingEvent logEvent = listAppender.list.get(0);
        String loggedMessage = logEvent.getFormattedMessage();

        assertTrue(loggedMessage.contains("caller"), "The log message should include caller information.");
        assertTrue(loggedMessage.contains("TestAnalyticsService.testInfoEvent_IncludesCaller"), "The log message should include caller information.");
    }

}