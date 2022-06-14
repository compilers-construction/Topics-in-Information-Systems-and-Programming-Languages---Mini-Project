package com.example;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Chore implements Task{

    private String id;
    private String ownerId;
    private String status;
    private String description;
    private String size;

    @JsonCreator
    public Chore(@JsonProperty("id") String id,
                @JsonProperty("ownerId") String ownerId,
                @JsonProperty("status") String status,
                @JsonProperty("description") String description,
                @JsonProperty("size") String size) {

        this.id = id;
        this.ownerId = ownerId;
        this.status = status;
        this.description = description;
        this.size = size;
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

    public String getDescription() {return this.description;}

    public String getSize() {return this.size;}

    public String toString() {
        return "Chore id: " + id + " ownerId: " + ownerId + " status: " + status + " description: " + description + " size: " + size;
    }
    
}
