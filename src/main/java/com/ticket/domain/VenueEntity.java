package com.ticket.domain;

import jakarta.persistence.*;
@Entity
@Table(name = "venues")

public class VenueEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name ="timezone", nullable = false)
    private String timezone;

    public VenueEntity(){}
    public VenueEntity(String id, String name, String address, String timezone){
        this.id=id;
        this.name=name;
        this.address=address;
        this.timezone=timezone;
    }
    public String getId(){return id;}
    public String getName(){return name;}

}

