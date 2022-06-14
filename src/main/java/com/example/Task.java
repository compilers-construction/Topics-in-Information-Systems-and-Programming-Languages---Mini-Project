package com.example;


// public abstract class Task {
    
//     public String id;
//     public String ownerId;
//     public String status;

//     @JsonCreator
//     public Task(@JsonProperty("id") String id, @JsonProperty("ownerId") String ownerId, @JsonProperty("status") String status) {
//         this.id = id;
//         this.ownerId = ownerId;
//         this.status = status;
//     }

//     public String getId() {return this.id;}

//     public String getownerId() {return this.ownerId;}

//     public String getStatus() {return this.status;}


//     public String toString() {
//         return "Person - id:" + id + " ownerId:" + ownerId + " status:" + status;
//     }
// }

public interface Task {

    public String getId();

    public String getOwnerId();

    public String getStatus();

    public void setOwner(String ownerId);

    public void setStatus(String status);
}