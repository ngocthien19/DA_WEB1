package vn.iotstar.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import vn.iotstar.service.UserDetailsImpl;
import vn.iotstar.service.impl.UserDetailsServiceImpl;
import vn.iotstar.util.JwtUtil;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public JwtAuthenticationFilter authenticationJwtTokenFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedPage("/access-denied")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/api/auth/web-login")
                .usernameParameter("email")
                .passwordParameter("matKhau")
                .successHandler((request, response, authentication) -> {
                    // Tạo JWT token cho session
                    String jwt = jwtUtil.generateJwtToken(authentication);
                    request.getSession().setAttribute("jwtToken", jwt);
                    
                    // Redirect based on role
                    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                    String role = userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> r.replace("ROLE_", ""))
                        .orElse("USER");
                    
                    // *** CẬP NHẬT LOGIC CHUYỂN HƯỚNG ***
                    if ("ADMIN".equals(role)) {
                        response.sendRedirect("/admin/dashboard");
                    } else if ("VENDOR".equals(role)) {
                        response.sendRedirect("/vendor/dashboard"); // Chuyển Vendor vào dashboard
                    } else {
                        response.sendRedirect("/user/dashboard"); // USER/CUSTOMER vào user dashboard
                    }
                })
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect("/login?error=true");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "jwtToken")
                .clearAuthentication(true)
                .permitAll()
            )
            .authorizeHttpRequests(authz -> authz
                // Public APIs - không cần authentication
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                
                // Static resources - cho phép truy cập tự do
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/files/**").permitAll()
                
                // Public pages - không cần đăng nhập
                .requestMatchers("/", "/index", "/home").permitAll()
                .requestMatchers("/login", "/register", "/verify-otp").permitAll()
                .requestMatchers("/forgot-password", "/verify-forgot-password-otp", "/reset-password").permitAll()
                .requestMatchers("/web/**").permitAll()
                .requestMatchers("/products", "/product/**").permitAll()
                .requestMatchers("/category/**", "/view/**").permitAll()
                .requestMatchers("/stores", "/store/**").permitAll()
                .requestMatchers("/profile").permitAll()
                .requestMatchers("/profile/api/user/**").permitAll()
                .requestMatchers("/access-denied").permitAll()
                
                .requestMatchers("/cart/**").authenticated()
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                
                // User profile pages - cần đăng nhập
                .requestMatchers("/user/**").authenticated()
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/register-store").authenticated()
                
                // API user - cần đăng nhập
                .requestMatchers("/api/user/**").authenticated()
                
                // Admin pages - cần role ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
             // Vendor pages - cần role VENDOR
                .requestMatchers("/vendor/**").hasRole("VENDOR")
                
             // Shipper pages - cần role SHIPPER
                .requestMatchers("/shipper/**").hasRole("SHIPPER")
                
                // Mặc định - yêu cầu authentication
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}