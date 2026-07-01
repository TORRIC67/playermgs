package com.playermgs.model;

/**
 * Base class — Relationship ① INHERITANCE
 * Player and Coach both extend Person.
 */
public abstract class Person {

    private Long   id;
    private String name;
    private int    age;
    private String email;
    private String nationality;

    public Person() {}

    public Person(Long id, String name, int age, String email, String nationality) {
        this.id          = id;
        this.name        = name;
        this.age         = age;
        this.email       = email;
        this.nationality = nationality;
    }

    // ── Getters & Setters ──────────────────────────────────
    public Long   getId()          { return id; }
    public void   setId(Long id)   { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public int  getAge()          { return age; }
    public void setAge(int age)   { this.age = age; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public String getNationality()                    { return nationality; }
    public void   setNationality(String nationality)  { this.nationality = nationality; }

    @Override
    public String toString() {
        return "Person{id=" + id + ", name='" + name + "', age=" + age + "}";
    }
}
