package com.example;

import akka.Done;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.model.headers.LinkParams.type;
import akka.actor.typed.javadsl.Behaviors;
import akka.dispatch.OnSuccess;
import akka.actor.typed.ActorSystem;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.RejectionHandler;

import com.example.*;

//------------------------------------------------

//#main-class
public class QuickstartApp extends AllDirectives{

    private static Connection conn = null;  
    private static Statement stmt = null;

    public static void main(String[] args) throws Exception {
        // boot up server using the route as defined below
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "routes");
    
        final Http http = Http.get(system);
    
        //In order to access all directives we need an instance where the routes are define.
        QuickstartApp app = new QuickstartApp();
    
        final CompletionStage<ServerBinding> binding =
          http.newServerAt("localhost", 8080)
              .bind(app.Routes());
              
              
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        connect();
        System.in.read(); // let it run until user presses return
    
        binding
            .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
            .thenAccept(unbound -> {
                if (conn != null) {  
                    try {
                        stmt = conn.createStatement();
                        stmt.executeUpdate("DROP TABLE Persons");
                        stmt.executeUpdate("DROP TABLE Tasks");
                        stmt.executeUpdate("DROP TABLE Chore");
                        stmt.executeUpdate("DROP TABLE HomeWork");
                        stmt.close();
                        conn.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }  
                } 
                system.terminate();}); // and shutdown when done
    }


    private Route Routes() {
        final RejectionHandler defaultHandler = RejectionHandler.defaultHandler();
        final ExceptionHandler exceptionHandler = ExceptionHandler.newBuilder()
            .match(RuntimeException.class, x ->
                complete(StatusCodes.INTERNAL_SERVER_ERROR, x.getMessage()))
            .build();
        return concat(
            pathPrefix("people", () ->
                    concat(
                        pathEnd(() -> 
                            concat(
                                // Add a new person.
                                post(() -> 
                                    entity(
                                        Jackson.unmarshaller(Person.class), 
                                        person -> {
                                            System.out.println("THE PERSON: " + person);
                                            CompletionStage<Done> futureSaved = addPerson(person);
                                            return onComplete(futureSaved, done -> complete("Person created successfully"))
                                            .seal(defaultHandler, exceptionHandler);
                                        }
                                    )
                                ),
                                // Get a list of all the people.
                                get(() -> {
                                    CompletionStage<List<Person>> personsResultSet = getPeopleList();
                                    return onSuccess(personsResultSet, maybeList -> {
                                        return complete(StatusCodes.OK, maybeList, Jackson.marshaller());
                                    });
                                })
                            )
                        ),
                        path(PathMatchers.segment(), (String id) -> 
                            concat(
                                get(() -> {
                                    System.out.println("id is: " + id);
                                    CompletionStage<Optional<Person>> person = getPersonById(id);
                                    return onSuccess(person, per -> 
                                        per.map(per1 -> completeOK(per1, Jackson.marshaller()))
                                            .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Person not found"))
                                    );
                                }),
                                patch(() -> 
                                    entity(
                                        Jackson.unmarshaller(Person.class),
                                        person -> {
                                            CompletionStage<Done> donePersonUpdated = updatePersonById(person, id);
                                            return onSuccess(donePersonUpdated, done -> complete("Person Updated"));
                                        }
                                    )
                                ),
                                delete(() -> {
                                    CompletionStage<Done> deletedPerson = deletePersonById(id);
                                    return onSuccess(deletedPerson, done -> complete("Person Deleted"));
                                })
                            )
                        ),
                        path(PathMatchers.segment().slash("tasks"), (String id) ->
                            concat(
                                post(() -> 
                                    entity(
                                        Jackson.unmarshaller(GenericTask.class), 
                                        task -> {
                                            System.out.println(task.getType());
                                            Task tsk = task.getType().equals("Chore") 
                                                ? new Chore(task.getId(), id, task.getStatus(), task.getDescription(), task.getSize()) 
                                                : new HomeWork(task.getId(), id, task.getStatus(), task.getCourse(), task.getDueDate(), task.getDetails());
                                            CompletionStage<Done> updatedTask = updateTask(tsk, task.getType()); //TODO
                                            return onSuccess(updatedTask, done -> complete("Task Added"));
                                        }
                                    )
                                )
                            )
                        )
                    )
            ),
            pathPrefix("tasks", () -> 
                concat(
                    path(PathMatchers.segment(), (String id) ->
                        delete(() -> {
                                CompletionStage<Done> deletedTask = deleteTaskById(id);
                                return onSuccess(deletedTask, done -> complete("Task Deleted"));
                            }
                        )
                    ),
                    path(PathMatchers.segment().slash("status"), (String id) -> 
                        concat(
                            get(() -> {
                                CompletionStage<Optional<Status>> task = getStatusTaskById(id);
                                return onSuccess(task, t -> 
                                    t.map(t1 -> completeOK(t1, Jackson.marshaller()))
                                        .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Task not found")));
                                }   
                            ),
                            put(() -> 
                                entity(
                                    Jackson.unmarshaller(String.class),
                                    status -> {
                                        CompletionStage<Done> doneTaskUpdated = updateTaskById(status, id);
                                        return onSuccess(doneTaskUpdated, done -> complete("Task Updated"));
                                    }
                                )  
                            )
                        )
                    ),
                    path(PathMatchers.segment().slash("owner"), (String id) -> 
                        concat(
                            get(() -> {
                                CompletionStage<Optional<String>> task = getOwnerIdById(id);
                                return onSuccess(task, t -> 
                                    t.map(t1 -> completeOK(t1, Jackson.marshaller()))
                                        .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Task not found")));
                                }   
                            ),
                            put(() -> 
                                entity(
                                    Jackson.unmarshaller(String.class),
                                    ownerId -> {
                                        CompletionStage<Done> doneTaskUpdated = updateOwnerIdById(ownerId, id);
                                        return onSuccess(doneTaskUpdated, done -> complete("OwnerID Updated"));
                                    }
                                )  
                            )
                        )
                    )
                )
            )
        );
    }


    private CompletionStage<Done> deleteTaskById(String id) {
        String sqlQuery = "DELETE FROM Tasks WHERE id = " + Integer.valueOf(id);
        try{
            stmt = conn.createStatement();
            stmt.executeUpdate(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private CompletionStage<Done> updateOwnerIdById(String ownerId, String id) {
        String sqlQuery = "UPDATE Tasks SET ownerId = '" + ownerId + "' WHERE id = " + Integer.valueOf(id); 
        try {
            stmt = conn.createStatement();
            stmt.execute(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private CompletionStage<Optional<String>> getOwnerIdById(String id) {
        String ownerId = null;
        try {
            stmt = conn.createStatement();
            System.out.println("THE ID: " + id);
            ResultSet task = stmt.executeQuery("SELECT * FROM Tasks WHERE id = " + Integer.valueOf(id));
            while (task.next()) {
                ownerId = task.getString("ownerId");
            }
            stmt.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Optional.of(ownerId));
    }


    private CompletionStage<Done> deletePersonById(String id) {
        String sqlQuery = "DELETE FROM Persons WHERE id = " + Integer.valueOf(id);
        try{
            stmt = conn.createStatement();
            stmt.executeUpdate(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private CompletionStage<Done> updateTaskById(String status, String id) {
        String sqlQuery = "UPDATE Tasks SET status = '" + status + "' WHERE id = " + Integer.valueOf(id); 
        try {
            stmt = conn.createStatement();
            stmt.execute(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }


    private CompletionStage<Optional<Status>> getStatusTaskById(String id) {
        Status statusToReturn = null;
        try {
            stmt = conn.createStatement();
            System.out.println("THE ID: " + id);
            ResultSet task = stmt.executeQuery("SELECT * FROM Tasks WHERE id = " + Integer.valueOf(id));
            while (task.next()) {
                String status = task.getString("status");
                statusToReturn = Status.valueOf(status);
            }
            stmt.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Optional.of(statusToReturn));
    }

    private CompletionStage<Done> updateTask(Task task, String type) {
        String insertTaskQuery = "INSERT INTO Tasks (ownerId, type, status) VALUES ('" + task.getOwnerId() + "', '" + type + "', '" + task.getStatus() + "')";
        int maximumId = 1;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(insertTaskQuery);
            ResultSet maxId = stmt.executeQuery("SELECT MAX(id) AS max_id FROM Tasks");
            while (maxId.next()) {
                maximumId = maxId.getInt("max_id");
            }
            String sqlQuery = type.equals("Chore")
                        ? "INSERT INTO Chore (taskId, size, description) VALUES (" + maximumId + ", '" + ((Chore) task).getSize() + "', '" + ((Chore) task).getDescription() + "')"
                        : "INSERT INTO HomeWork (taskId, course, dueDate, details) VALUES (" + maximumId + ", '" + ((HomeWork) task).getCourse() + "', '" + ((HomeWork) task).getDueDate() + "', '" + ((HomeWork) task).getDetails() + "')";
            System.out.println(sqlQuery);
            stmt.executeUpdate(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private CompletionStage<Done> updatePersonById(Person person, String id) {
        String sqlQuery = "UPDATE Persons SET name = '" + person.getName() + "', email = '" + person.getEmail() + "', favoriteProgrammingLanguage = '" + person.getFavoriteProgrammingLanguage() + "' WHERE id = " + Integer.valueOf(id); 
        try {
            stmt = conn.createStatement();
            stmt.execute(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private CompletionStage<Optional<Person>> getPersonById(String id) {
        Person personToReturn = null;
        try {
            stmt = conn.createStatement();
            ResultSet person = stmt.executeQuery("SELECT * FROM Persons WHERE id = " + Integer.valueOf(id));
            while (person.next()) {
                String personId = String.valueOf(person.getInt("id"));        
                String name = person.getString("name");
                String email = person.getString("email");
                String favoriteProgrammingLanguage = person.getString("favoriteProgrammingLanguage");
                personToReturn = new Person(personId, name, email, favoriteProgrammingLanguage);
            }
            stmt.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(personToReturn);
        return CompletableFuture.completedFuture(Optional.of(personToReturn));
    }
        
    private CompletionStage<List<Person>> getPeopleList() {
        List<Person> personsList = new LinkedList<>();
        try {
            stmt = conn.createStatement();
            ResultSet persons = stmt.executeQuery("SELECT * FROM Persons");
            while (persons.next()) {
                String id = String.valueOf(persons.getInt("id"));        
                String name = persons.getString("name");
                String email = persons.getString("email");
                String favoriteProgrammingLanguage = persons.getString("favoriteProgrammingLanguage");
                personsList.add(new Person(id, name, email, favoriteProgrammingLanguage));
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(personsList);
    }

    private CompletionStage<Done> addPerson(Person person) {
        if (person.getEmail() == null || person.getName() == null || person.getFavoriteProgrammingLanguage() == null) {
            throw new RuntimeException("Missing data");
        }
        String sqlQuery = "INSERT INTO Persons (name, email, favoriteProgrammingLanguage) " + 
        "VALUES ('" + person.getName() + "', '" + person.getEmail() + "', '" + person.getFavoriteProgrammingLanguage() + "')";
        System.out.println("THE QUERY: " + sqlQuery);
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sqlQuery);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("SQL Error");
        }
        return CompletableFuture.completedFuture(Done.getInstance());
    }


    public static void connect() throws ClassNotFoundException {  
        try {  
            // Class.forName("org.sqlite.JDBC");            // db parameters  
            String url = "jdbc:sqlite:C:/sqlite/chinook.db";  
            // create a connection to the database  
            conn = DriverManager.getConnection(url); 
            System.out.println("Connection to SQLite has been established.");   

            stmt = conn.createStatement();
            String createPeopleTableQuery = 
            "CREATE TABLE Persons " + 
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "email TEXT NOT NULL, " +
            "favoriteProgrammingLanguage TEXT NOT NULL)";
            stmt.executeUpdate(createPeopleTableQuery);
            String createTasksTableQuery = 
            "CREATE TABLE Tasks " + 
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "ownerId TEXT NOT NULL, " + //TODO - to make sure that there is this ID
            "type TEXT CHECK(type IN ('Chore', 'HomeWork')) NOT NULL," + 
            "status TEXT CHECK(status IN ('Done', 'Active')) NOT NULL DEFAULT 'Active'," + //TOSO- not working
            "FOREIGN KEY(ownerId) REFERENCES Persons(id))";
            stmt.executeUpdate(createTasksTableQuery);
            String createChoreTable = 
            "CREATE TABLE Chore " +
            "(taskId INTEGER PRIMARY KEY, " +
            "description TEXT NOT NULL, " +
            "size TEXT CHECK(size IN ('Small', 'Medium', 'Large')), " +
            "FOREIGN KEY(taskId) REFERENCES Tasks(id))";
            stmt.executeUpdate(createChoreTable);
            String createHomeworkTable = 
            "CREATE TABLE HomeWork " +
            "(taskId INTEGER PRIMARY KEY, " +
            "course TEXT NOT NULL, " +
            "dueDate TEXT NOT NULL, " +
            "details TEXT NOT NULL, " +
            "FOREIGN KEY(taskId) REFERENCES Tasks(id))";
            stmt.executeUpdate(createHomeworkTable);
            stmt.close();


            System.out.println("Added People Table"); 
              
        } catch (SQLException e) {  
            System.out.println(e.getMessage()); 
        } 
    //     } finally {  
    //         try {  
    //             if (conn != null) {  
    //                 conn.close();  
    //             }  
    //         } catch (SQLException ex) {  
    //             System.out.println(ex.getMessage());  
    //         }  
    //     }  
    // } 
    } 

}


