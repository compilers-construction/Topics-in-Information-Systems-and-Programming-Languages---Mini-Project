package com.example;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeWork implements Task{

    private String id;
    private String ownerId;
    private String status;
    private String course;
    private String dueDate;
    private String details;

    @JsonCreator
    public HomeWork(@JsonProperty("id") String id,
                @JsonProperty("ownerId") String ownerId,
                @JsonProperty("status") String status,
                @JsonProperty("course") String course,
                @JsonProperty("dueDate") String dueDate,
                @JsonProperty("details") String details) {

        this.id = id;
        this.ownerId = ownerId;
        this.status = status;
        this.course = course;
        this.dueDate = dueDate;
        this.details = details;
    }


    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return id;
    }

    @Override
    public String getOwnerId() {
        // TODO Auto-generated method stub
        return ownerId;
    }

    @Override
    public String getStatus() {
        // TODO Auto-generated method stub
        return status;
    }

    @Override
    public void setOwner(String ownerId) {
        // TODO Auto-generated method stub
        this.ownerId = ownerId;
    }

    @Override
    public void setStatus(String status) {
        // TODO Auto-generated method stub
        this.status = status;
    }

    public String getCourse() {return this.course;}

    public String getDueDate() {return this.dueDate;}

    public String getDetails() {return this.details;}

    public String toString() {
        return "Chore id: " + id + " ownerId: " + ownerId + " status: " + status + " course: " + course + " dueDate: " + dueDate + " details: " + details;
    }
    
}
