package cn.itcast.tanhua.sso.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends BasePojo{

    private  Long id;

    private String mobile;

    @JsonIgnore
    private String password;
}
