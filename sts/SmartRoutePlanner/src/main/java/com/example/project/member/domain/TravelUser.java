package com.example.project.member.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.project.member.domain.MemberLikeRoute;
import com.example.project.route.domain.Route;   // ← 올바른 Route import!!!
import com.example.project.security.token.Token;
import com.example.project.security.user.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TRAVEL_USER")
public class TravelUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_member_id_gen")
    @SequenceGenerator(name = "seq_member_id_gen", sequenceName = "SEQ_MEMBER_ID", allocationSize = 1)
    @Column(name = "memberid")
    private Integer id;

    @Column(length = 50)
    private String nickname;

    @Column(length = 1)
    private String gender;

    private Integer age;

    @Column(length = 100, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_owner", length = 20)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private LocalDate enrolldate = LocalDate.now();

    @Builder.Default
    @Column(length = 1)
    private String delflag = "N";

    private LocalDate deletedate;

    @Column(length = 1)
    private String regflag;

    @Column(length = 300)
    private String password;

    // --- 연관 관계 매핑 ---
    @OneToMany(mappedBy = "user")
    private List<Token> tokens;

    @OneToMany(mappedBy = "user")
    private List<Route> routes;   // ← 여기 타입이 이제 제대로 Route로 매칭됨!

    @OneToMany(mappedBy = "user")
    private List<MemberLikeRoute> likedRoutes;


    // --- UserDetails 구현 ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return "N".equals(delflag); }
}
