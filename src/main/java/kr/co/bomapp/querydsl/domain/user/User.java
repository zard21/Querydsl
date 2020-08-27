package kr.co.bomapp.querydsl.domain.user;

import javax.persistence.*;
import kr.co.bomapp.querydsl.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue
    private Long id;
    private String name;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;


    public User(String name) {
        this(name, 0);
    }

    public User(String name, int age) {
        this(name, age, null);
    }

    public User(String name, int age, Department department) {
        this.name = name;
        this.age = age;
        if (department != null) {
            changeDepartment(department);
        }
    }

    public void changeDepartment(Department department) {
        this.department = department;
        department.getUsers().add(this);
    }
}
