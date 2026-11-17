package com.college.library.model;

import java.time.LocalDate;

public class Member {
    private final String id;
    private String name;
    private String email;
    private MemberType type;
    private final LocalDate joinedOn;

    public Member(String id, String name, String email, MemberType type, LocalDate joinedOn) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.type = type;
        this.joinedOn = joinedOn;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public MemberType getType() { return type; }
    public LocalDate getJoinedOn() { return joinedOn; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setType(MemberType type) { this.type = type; }
}
