package com.example;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericTask {
    
    private String id;
    private String ownerId;
    private String status;
    private String type;
    private String description;
    private String size;
    private String course;
    private String dueDate;
    private String details;

    @JsonCreator
    public GenericTask(@JsonProperty("id") String id,
                        @JsonProperty("ownerId") String ownerId,
                        @JsonProperty("status") String status,
                        @JsonProperty("type") String type,
                        @JsonProperty("description") String description,
                        @JsonProperty("size") String size,
                        @JsonProperty("course") String course,
                        @JsonProperty("dueDate") String dueDate,
                        @JsonProperty("details") String details) {
                            
        this.id = id;
        this.ownerId = ownerId;
        this.status = status;
        this.type = type;
        this.description = description;
        this.size = size;
        this.course = course;
        this.dueDate = dueDate;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {return this.description;}

    public String getSize() {return this.size;}

    public String getType() {return this.type;}


    public String getCourse() {return this.course;}

    public String getDueDate() {return this.dueDate;}

    public String getDetails() {return this.details;}
}
