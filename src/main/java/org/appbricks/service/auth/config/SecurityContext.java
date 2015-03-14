package org.appbricks.service.auth.config;

import org.appbricks.service.auth.service.UserLoginService;
import org.appbricks.model.user.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.social.security.SpringSocialConfigurer;
import org.thymeleaf.extras.springsecurity3.dialect.SpringSecurityDialect;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.inject.Inject;

/**
 * Spring security configuration context
 */
@Configuration
@EnableWebMvcSecurity
public class SecurityContext
    extends WebSecurityConfigurerAdapter {

    @Inject
    private UserLoginService userLoginService;

    @Override
    public void configure(WebSecurity web) 
        throws Exception {
        
        web
            //Spring Security ignores request to static resources such as CSS or JS files.
            .ignoring()
            .antMatchers(
                "/css/**",
                "/images/**",
                "/js/**");
    }

    @Override
    protected void configure(HttpSecurity http) 
        throws Exception {
        
        http
            //Configures form login
            .formLogin()
            .loginPage("/signin")
            .loginProcessingUrl("/login/authenticate")
            .failureUrl("/login?error=bad_credentials")
            //Configures the logout function
            .and()
                .logout()
                .deleteCookies("JSESSIONID")
                .logoutUrl("/signout")
                .logoutSuccessUrl("/signin")
            //Configures url based authorization
            .and()
                .authorizeRequests()
                    //Anyone can access the urls
                    .antMatchers(
                        "/",
                        "/signin",
                        "/signup/**").permitAll()
                    //The rest of the our application is protected.
                    .antMatchers("/**").hasRole("USER")
            //Adds the SocialAuthenticationFilter to Spring Security's filter chain.
            .and()
                .apply(new SpringSocialConfigurer());
    }

    /**
     * Configures the authentication manager bean 
     * which processes authentication requests.
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) 
        throws Exception {
        
        auth
            .userDetailsService(this.userLoginService)
            .passwordEncoder(User.PASSWORD_ENCODER);
    }
}
