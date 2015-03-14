package org.appbricks.service.auth.config;

import org.appbricks.repository.user.SocialConnectionRepository;
import org.appbricks.service.auth.repository.UsersConnectionRepositoryImpl;
import org.appbricks.service.auth.service.ConnectionSignUpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.*;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.impl.GoogleTemplate;
import org.springframework.social.security.AuthenticationNameUserIdSource;
import org.springframework.social.security.SocialAuthenticationServiceLocator;

import javax.inject.Inject;

/**
 * Spring social context
 */
@Configuration
@EnableSocial
public class SocialContext
    implements SocialConfigurer {

    @Inject
    private SocialConnectionRepository socialConnectionRepository;

    @Inject
    private ConnectionSignUpService connectionSignUpService;

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {

//        GoogleConnectionFactory gcf = new GoogleConnectionFactory(
//            environment.getProperty("social.google.client.id"),
//            environment.getProperty("social.google.client.secret") );
//
//        gcf.setScope(environment.getProperty("social.google.scope"));
//        connectionFactoryConfigurer.addConnectionFactory(gcf);
//
//        LinkedInConnectionFactory lcf = new LinkedInConnectionFactory(
//                environment.getProperty("social.linkedin.api.key"),
//                environment.getProperty("social.linkedin.api.secret") );
//
//        lcf.setScope(environment.getProperty("social.linkedin.scope"));
//        connectionFactoryConfigurer.addConnectionFactory(lcf);
//
        FacebookConnectionFactory fcf = new FacebookConnectionFactory(
                environment.getProperty("social.facebook.app.id"),
                environment.getProperty("social.facebook.app.secret") );

        fcf.setScope(environment.getProperty("social.facebook.scope"));
        connectionFactoryConfigurer.addConnectionFactory(fcf);
    }

    @Override
    public UserIdSource getUserIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        
        UsersConnectionRepositoryImpl repository = 
            new UsersConnectionRepositoryImpl( 
                socialConnectionRepository,
                (SocialAuthenticationServiceLocator) connectionFactoryLocator,
                // TODO: provide a more secure encryptor
                Encryptors.noOpText() );
        
        repository.setConnectionSignUp(this.connectionSignUpService);
        return repository;
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
    public Google google(ConnectionRepository repository) {
        Connection<Google> connection = repository.findPrimaryConnection(Google.class);
        return connection != null ? connection.getApi() : new GoogleTemplate();
    }
}
