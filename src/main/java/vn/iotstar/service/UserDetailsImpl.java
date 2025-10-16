package vn.iotstar.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.iotstar.entity.NguoiDung;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String email;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(NguoiDung nguoiDung) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().getMaVaiTro());

        return new UserDetailsImpl(
                nguoiDung.getMaNguoiDung(),
                nguoiDung.getTenNguoiDung(),
                nguoiDung.getEmail(),
                nguoiDung.getMatKhau(),
                Collections.singletonList(authority));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Spring Security sử dụng email làm username
    }

    public String getDisplayName() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Hoạt động".equals(getTrangThai());
    }

    // Helper method để lấy trạng thái từ entity
    private String getTrangThai() {
        // Implement logic để lấy trạng thái từ entity nếu cần
        return "Hoạt động";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}