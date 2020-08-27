package kr.co.bomapp.querydsl.domain.user;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class UserDTO {

    private String name;
    private int age;

    public UserDTO() {
    }

    @QueryProjection
    public UserDTO(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
