package org.appbricks.service.auth.service;

import org.appbricks.model.person.Email;
import org.appbricks.model.person.Person;
import org.appbricks.model.user.User;
import org.appbricks.repository.user.RoleRepository;
import org.appbricks.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UserProfile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Sign up users who are signed in via their social network account 
 * (google or twitter). This will create a new user in database, and 
 * populate that user's profile data from provider.
 */
@Component
public class ConnectionSignUpService
    implements ConnectionSignUp {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionSignUpService.class);

    @Inject
    private UserRepository userRepository;

    @Inject
    private RoleRepository roleRepository;

    @Value("${user.rbac.user.role}")
    private String defaultUserRole;

    @Override
    public String execute(Connection<?> connection) {
        
        // Create a new un-registered user
        
        ConnectionData data = connection.createData();
        UserProfile profile = connection.fetchUserProfile();

        User user = new User(profile.getUsername(), profile.getEmail());
        
        Person person = new Person();
        person.setFamilyName(profile.getLastName());
        person.setGivenName(profile.getFirstName());
        person.addContact(new Email(profile.getEmail()));
        

//        if (logger.isDebugEnabled()) {
//
//            logger.debug(
//                "Created a new user '" + loggedInUser.getUserId() +
//                "', for " + loggedInUser );
//
//            logger.debug(
//                "connection data is from provider '" + data.getProviderId() +
//                "', providerUserId is '" + data.getProviderUserId() );
//        }

        return profile.getUsername();
    }
}
