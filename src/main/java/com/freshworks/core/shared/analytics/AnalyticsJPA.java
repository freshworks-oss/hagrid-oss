package com.freshworks.core.shared.analytics;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalyticsJPA {



//    String dbPath;
//    volatile AtomicBoolean uniqueClient = new AtomicBoolean(false);
//
//    volatile AtomicBoolean uniqueEventListAccess = new AtomicBoolean(false);
//    List<Event> eventList = new ArrayList<>();
//
//    DataSource dataSource;

//    public AnalyticsJPA(@Value("${spring.connector.analytics.store.path}") String dbPath) {

//        try{
//            this.dbPath = dbPath;
//            this.dataSource = initHikariDataSource(this.dbPath, "analytics");
//            Connection connection = dataSource.getConnection();
//
//            // SQL statement to create a table
//            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + "events" + "("
//                    + "id BIGINT NOT NULL AUTO_INCREMENT, "
//                    + "group_id varchar(255) NOT NULL, "
//                    + "namespace varchar(255) NOT NULL, "
//                    + "severity varchar(255) NOT NULL, "
//                    + "event varchar(255) NOT NULL, "
//                    + "tag varchar(255), "
//                    + "item varchar(255), "
//                    + "expiry timestamp NOT NULL, "
//                    + "created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, "
//                    + "updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, "
//                    + "PRIMARY KEY (id))";
//
//
//            Statement statement = connection.createStatement();
//            statement.execute(createTableSQL);
//            uniqueClient.set(false);
//            connection.close();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }

//    }

//    public void configure() {
//        try{
//
//            for(;;) {
//
//                if (uniqueClient.compareAndSet(false, true)) {
//
//                    if (doesClientExists()) {
//                        uniqueClient.set(false);
//                        break;
//                    }
//
//                    break;
//                }
//            } // for loop closed
//        }
//
//        catch(Exception e){
//            log.error("error creating table", e);
//            e.printStackTrace();
//        }
//
//    }


//    public void recordEvent(Event eventObj){
//
//        try{
//            for(;;){
//
//                if(uniqueEventListAccess.compareAndSet(false, true)) {
//                    eventList.add(eventObj);
//                    System.out.println("in the record event, releasing lock");
//                    uniqueEventListAccess.set(false);
//                    break;
//                }
//                else{
//                    System.out.println("in the record event, can not acquire lock");
//                }
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            uniqueEventListAccess.set(false);
//        }
//
//        finally {
//
//        }
//
//    }
//
//
//    private void processEvent(List<Event> eventObjList){
//
//        System.out.println("-----Processing events -------");
//        String insertSql =  "Insert into events" + " (group_id, namespace, severity, event, tag, item, expiry) values (?, ?, ?, ?, ?, ?, ?)";
//        try(Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(insertSql);){
//
//            connection.setAutoCommit(false);
//            System.out.println("Setting auto commit false");
//
//            System.out.println("Object size is " + eventObjList.size());
//            System.out.println(eventObjList.size());
//            for(Event eventObj : eventObjList){
//                String groupId = eventObj.getGroup_id();
//                String event = eventObj.getEvent();
//                String namespace = eventObj.getNamespace();
//                String severity = eventObj.getSeverity();
//                Map<String, Object> tags = eventObj.getTags();
//                Timestamp expiry = eventObj.getExpiry();
//
//                // Set the values for the placeholders
//                preparedStatement.setString(1, groupId);
//                preparedStatement.setString(2, namespace);
//                preparedStatement.setString(3, severity);
//                preparedStatement.setString(4, event);
//                preparedStatement.setTimestamp(7, expiry);
//
//                if(Boolean.FALSE.equals(tags.isEmpty())){
//                    for(Map.Entry<String, Object> entry : tags.entrySet()){
//
//                        System.out.println("Adding to batch ");
//                        if(entry.getValue() == null){
//                            preparedStatement.setString(5, entry.getKey());
//                            preparedStatement.setString(6, "null");
//                        }
//                        else{
//                            preparedStatement.setString(5, entry.getKey());
//                            preparedStatement.setString(6, entry.getValue().toString());
//                        }
//
//                        System.out.println("Adding batch ");
//                        preparedStatement.addBatch();
//                    }
//                }
//                else{
//
//                    preparedStatement.setString(5, null);
//                    preparedStatement.setString(6, null);
//                    System.out.println("Adding batch ");
//                    preparedStatement.addBatch();
//                }
//            }
//
//            System.out.println("executing batch");
//            // Execute the insert statement
//            int[] updateCounts = preparedStatement.executeBatch();
//
//            System.out.println("Executing commit");
//            // Commit the transaction
//            connection.commit();
//            System.out.println("Commit is done");
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        finally {
//            System.out.println("-----Processing events is done");
//        }
//
//    }
//
////    @Scheduled(cron = "* * * * * ?")
//    public void atomicValue(){
//
//        System.out.println( "Value is " + uniqueEventListAccess);
//    }
//
////    @Scheduled(cron = "0 * * * * ?")
//    public void flushEventBatch(){
//
//        try{
//            for(;;){
//                if (uniqueEventListAccess.compareAndSet(false, true)) {
//                    System.out.println("Executing process event");
//
//                    for(int i=0; i<100; i++){
//                        Event eventObj = new Event();
//                        eventObj.setGroup_id(UUID.randomUUID().toString());
//                        eventObj.setNamespace(UUID.randomUUID().toString());
//                        eventObj.setSeverity(UUID.randomUUID().toString());
//                        eventObj.setEvent(UUID.randomUUID().toString());
//                        LocalDateTime now = LocalDateTime.now();
//                        LocalDateTime future = now.plusDays(1);
//                        eventObj.setExpiry(Timestamp.valueOf(future));
//                        eventList.add(eventObj);
//                    }
//
//
//                    processEvent(eventList);
//                    eventList.clear();
//                    System.out.println("releasing lock in flush event");
//                    System.out.println("in the flush event, releasing lock");
//                    uniqueEventListAccess.set(false);
//                    break;
//                }
//                else{
////                    System.out.println("in the flush batch event, can not acquire lock");
//                }
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            uniqueEventListAccess.set(false);
//        }
//
//    }
//
//    @Scheduled(cron = "0 * * * * ?")
//    public void deleteExpiryEvent(){
//
//        String sql = "DELETE FROM events WHERE expiry < ?";
//        try(Connection connection = dataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(sql);){
//
//                LocalDateTime localDateTime = LocalDateTime.now();
//                preparedStatement.setTimestamp(1, Timestamp.valueOf(localDateTime));
//
//                // Execute the insert statement
//                int rowsAffected = preparedStatement.executeUpdate();
//                System.out.println("Rows deleted: " + rowsAffected);
//
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    private DataSource initHikariDataSource(String dbPath, String namespace) {
//
//        String dbString = "jdbc:h2:file:" + dbPath + "/" + namespace + ";TRACE_LEVEL_FILE=2";
//
//        HikariConfig hikariConfig = new HikariConfig();
//        hikariConfig.setJdbcUrl(dbString);
//        hikariConfig.setUsername("");
//        hikariConfig.setPassword("");
//        hikariConfig.setDriverClassName("org.h2.Driver");
//        hikariConfig.setMaximumPoolSize(10);
//        return new HikariDataSource(hikariConfig);
//    }
//
//    public List<Event> executeQuery(String sql) throws SQLException {
//
//        try(Connection connection = dataSource.getConnection();PreparedStatement preparedStatement = connection.prepareStatement(sql);){
//
//            // Execute the insert statement
//            ResultSet resultSet = preparedStatement.executeQuery();
//
//            HashMap<String, Event> eventMap = new HashMap<>();
//
//            while(resultSet.next()){
//                String groupId = resultSet.getString("group_id");
//
//                if(eventMap.containsKey(groupId)){
//                    Event event = eventMap.get(groupId);
//                    String tag = resultSet.getString("tag");
//                    String item = resultSet.getString("item");
//                    event.addTags(tag, item);
//                }
//                else{
//                    Event event = new Event();
//                    event.setGroup_id(groupId);
//
//                    String namespace = resultSet.getString("namespace");
//                    event.setNamespace(namespace);
//
//                    String severity = resultSet.getString("severity");
//                    event.setSeverity(severity);
//
//                    Timestamp expiry = resultSet.getTimestamp("expiry");
//                    event.setExpiry(expiry);
//
//                    Timestamp createdAt = resultSet.getTimestamp("created_at");
//                    event.setCreated_at(createdAt);
//
//                    Timestamp updatedAt = resultSet.getTimestamp("updated_at");
//                    event.setUpdated_at(updatedAt);
//
//                    String tag = resultSet.getString("tag");
//                    String item = resultSet.getString("item");
//                    event.addTags(tag, item);
//
//                    eventMap.put(groupId, event);
//                }
//            }
//
//            return new ArrayList<>(eventMap.values());
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    public boolean doesClientExists(){
//
//        if(dataSource != null) {
//            return true;
//        }
//        else{
//            return false;
//        }
//    }

}
