package com.smartattendance.app;

public class Course {
    public String id, name, code, teacherName;
    public int present = 0, total = 0; // student ke liye stats

    public Course(String id, String name, String code, String teacherName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.teacherName = teacherName;
    }
}
