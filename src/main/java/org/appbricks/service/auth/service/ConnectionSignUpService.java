package org.appbricks.service.auth.service;

import org.appbricks.model.person.Email;
import org.appbricks.model.person.Person;
import org.appbricks.model.user.Account;
import org.appbricks.model.user.IndividualUser;
import org.appbricks.model.user.LoggedInUser;
import org.appbricks.model.user.User;
import org.appbricks.repository.person.PersonRepository;
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
    private PersonRepository personRepository;

    @Inject
    private RoleRepository roleRepository;

    @Value("${user.rbac.user.role}")
    private String defaultUserRole;

    @Override
    public String execute(Connection<?> connection) {
        
        // Create a new un-registered user
        
        ConnectionData data = connection.createData();
        UserProfile profile = connection.fetchUserProfile();

        String name = profile.getName();
        String lastName = profile.getLastName();
        String firstName = profile.getFirstName();
        String email = profile.getEmail();

        if (email == null) {
            throw new ConnectionSignUpException("Email address not found in user profile from auth provider '%s'.", data.getProviderId());
        }

        User user = this.userRepository.findByLoginName(email);
        if (user != null) {
            return user.getLoginName();
        }

        Person owner = new Person();
        if (profile.getLastName() == null && profile.getName() != null) {
            owner.setFullName(name);
        } else {
            owner.setFamilyName(lastName);
        }
        owner.setGivenName(firstName);
        owner.addContact(new Email(email), true);

        IndividualUser newUser = new IndividualUser(email);
        newUser.getAccount().addPrimaryOwner(owner);

        try {
            this.personRepository.save(owner);
            this.userRepository.save(newUser);
        } catch (Throwable t1) {
            // Attempt to delete objects if added
            try { this.personRepository.delete(owner); } catch (Throwable t2) {}
            try { this.userRepository.delete(newUser); } catch (Throwable t2) {}
            throw t1;
        }

        if (logger.isDebugEnabled()) {

            logger.debug(
                "Created a new user with login '" + newUser.getLoginName() +
                "' with registration email " + newUser.getEmail() );

            logger.debug(
                "connection data is from provider '" + data.getProviderId() +
                "', providerUserId is '" + data.getProviderUserId() );
        }

        return email;
    }
}
