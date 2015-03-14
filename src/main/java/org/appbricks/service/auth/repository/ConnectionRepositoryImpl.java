package org.appbricks.service.auth.repository;

import org.appbricks.model.user.SocialConnection;
import org.appbricks.repository.user.SocialConnectionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.*;
import org.springframework.social.security.SocialAuthenticationServiceLocator;
import org.springframework.social.security.provider.SocialAuthenticationService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * Implements Spring Social's ConnectionRepository interface 
 * for managing the persistence of a user's connections via
 * the Mongo SocialConnectionRepository.
 */
public class ConnectionRepositoryImpl
    implements ConnectionRepository {

    private final String userId;

    private final SocialConnectionRepository SocialConnectionRepository;

    private final SocialAuthenticationServiceLocator socialAuthenticationServiceLocator;

    private final TextEncryptor textEncryptor;

    public ConnectionRepositoryImpl( String userId, 
        SocialConnectionRepository SocialConnectionRepository,
        SocialAuthenticationServiceLocator socialAuthenticationServiceLocator, 
        TextEncryptor textEncryptor ) {
        
        this.userId = userId;
        this.SocialConnectionRepository = SocialConnectionRepository;
        this.socialAuthenticationServiceLocator = socialAuthenticationServiceLocator;
        this.textEncryptor = textEncryptor;
    }

    public MultiValueMap<String, Connection<?>> findAllConnections() {
        
        List<SocialConnection> SocialConnectionList = this.SocialConnectionRepository.findByUserId(userId);

        MultiValueMap<String, Connection<?>> connections = new LinkedMultiValueMap<String, Connection<?>>();
        Set<String> registeredProviderIds = socialAuthenticationServiceLocator.registeredProviderIds();
        
        for (String registeredProviderId : registeredProviderIds) {
            connections.put(registeredProviderId, Collections.<Connection<?>> emptyList());
        }
        
        for (SocialConnection SocialConnection : SocialConnectionList) {
            
            String providerId = SocialConnection.getProviderId();
            if (connections.get(providerId).size() == 0) {
                connections.put(providerId, new LinkedList<Connection<?>>());
            }
            connections.add(providerId, buildConnection(SocialConnection));
        }
        
        return connections;
    }

    public List<Connection<?>> findConnections(String providerId) {
        
        List<Connection<?>> resultList = new LinkedList<Connection<?>>();
        
        List<SocialConnection> SocialConnectionList = this.SocialConnectionRepository
            .findByUserIdAndProviderId(userId, providerId);

        for (SocialConnection SocialConnection : SocialConnectionList) {
            resultList.add(buildConnection(SocialConnection));
        }
        return resultList;
    }

    @SuppressWarnings("unchecked")
    public <A> List<Connection<A>> findConnections(Class<A> apiType) {
        
        List<?> connections = findConnections(getProviderId(apiType));
        return (List<Connection<A>>) connections;
    }

    public MultiValueMap<String, Connection<?>> findConnectionsToUsers(MultiValueMap<String, String> providerUsers) {
        
        if (providerUsers == null || providerUsers.isEmpty()) {
            throw new IllegalArgumentException("Unable to execute find: no providerUsers provided");
        }

        MultiValueMap<String, Connection<?>> connectionsForUsers = new LinkedMultiValueMap<String, Connection<?>>();

        for (Iterator<Map.Entry<String, List<String>>> it = providerUsers.entrySet().iterator(); it.hasNext();) {
            
            Map.Entry<String, List<String>> entry = it.next();
            
            String providerId = entry.getKey();
            
            List<String> providerUserIds = entry.getValue();
            List<SocialConnection> SocialConnections = this.SocialConnectionRepository
                .findByProviderIdAndProviderUserIdIn(providerId, providerUserIds);

            List<Connection<?>> connections = new ArrayList<Connection<?>>(providerUserIds.size());
            for (int i = 0; i < providerUserIds.size(); i++) {
                connections.add(null);
            }
            connectionsForUsers.put(providerId, connections);

            for (SocialConnection SocialConnection : SocialConnections) {
                
                String providerUserId = SocialConnection.getProviderUserId();
                int connectionIndex = providerUserIds.indexOf(providerUserId);
                connections.set(connectionIndex, buildConnection(SocialConnection));
            }

        }
        return connectionsForUsers;
    }

    public Connection<?> getConnection(ConnectionKey connectionKey) {
        
        SocialConnection SocialConnection = this.SocialConnectionRepository
            .findByUserIdAndProviderIdAndProviderUserId(
                    userId, connectionKey.getProviderId(), connectionKey.getProviderUserId());

        if (SocialConnection != null) {
            return buildConnection(SocialConnection);
        }
        throw new NoSuchConnectionException(connectionKey);
    }

    @SuppressWarnings("unchecked")
    public <A> Connection<A> getConnection(Class<A> apiType, String providerUserId) {
        
        String providerId = getProviderId(apiType);
        return (Connection<A>) getConnection(new ConnectionKey(providerId, providerUserId));
    }

    @SuppressWarnings("unchecked")
    public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
        
        String providerId = getProviderId(apiType);
        Connection<A> connection = (Connection<A>) findPrimaryConnection(providerId);
        
        if (connection == null) {
            throw new NotConnectedException(providerId);
        }
        return connection;
    }

    @SuppressWarnings("unchecked")
    public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
        String providerId = getProviderId(apiType);
        return (Connection<A>) findPrimaryConnection(providerId);
    }

    public void addConnection(Connection<?> connection) {
        
        SocialAuthenticationService<?> socialAuthenticationService = this.socialAuthenticationServiceLocator
            .getAuthenticationService(connection.getKey().getProviderId());

        if ( socialAuthenticationService.getConnectionCardinality() == SocialAuthenticationService.ConnectionCardinality.ONE_TO_ONE ||
            socialAuthenticationService.getConnectionCardinality() == SocialAuthenticationService.ConnectionCardinality.ONE_TO_MANY) {
            
            List<SocialConnection> storedConnections = this.SocialConnectionRepository
                .findByProviderIdAndProviderUserId(
                        connection.getKey().getProviderId(), connection.getKey().getProviderUserId());

            if (storedConnections.size() > 0){
                // not allow one providerId/providerUserId connect to multiple userId
                throw new DuplicateConnectionException(connection.getKey());
            }
        }

        SocialConnection socialConnection = this.SocialConnectionRepository
            .findByUserIdAndProviderIdAndProviderUserId(
                userId, connection.getKey().getProviderId(), connection.getKey().getProviderUserId());

        if (socialConnection == null) {
            
            ConnectionData data = connection.createData();
            
            socialConnection = 
                new SocialConnection(
                    userId,
                    data.getProviderId(),
                    data.getProviderUserId(),
                    data.getDisplayName(),
                    data.getProfileUrl(),
                    data.getImageUrl(),
                    encrypt(data.getAccessToken()),
                    data.getSecret(),
                    encrypt(data.getRefreshToken()),
                    data.getExpireTime() );

            this.SocialConnectionRepository.save(socialConnection);
            
        } else {
            throw new DuplicateConnectionException(connection.getKey());
        }
    }

    public void updateConnection(Connection<?> connection) {
        
        ConnectionData data = connection.createData();
        SocialConnection SocialConnection = this.SocialConnectionRepository
            .findByUserIdAndProviderIdAndProviderUserId(
                    userId, connection.getKey().getProviderId(), connection.getKey().getProviderUserId());

        if (SocialConnection != null) {
            
            SocialConnection.setDisplayName(data.getDisplayName());
            SocialConnection.setProfileUrl(data.getProfileUrl());
            SocialConnection.setImageUrl(data.getImageUrl());
            SocialConnection.setAccessToken(encrypt(data.getAccessToken()));
            SocialConnection.setSecret(encrypt(data.getSecret()));
            SocialConnection.setRefreshToken(encrypt(data.getRefreshToken()));
            SocialConnection.setExpireTime(data.getExpireTime());
            this.SocialConnectionRepository.save(SocialConnection);
        }
    }

    public void removeConnections(String providerId) {
        
        List<SocialConnection> SocialConnectionList = this.SocialConnectionRepository
            .findByUserIdAndProviderId(userId, providerId);

        for (SocialConnection SocialConnection : SocialConnectionList) {
            this.SocialConnectionRepository.delete(SocialConnection);
        }
    }

    public void removeConnection(ConnectionKey connectionKey) {
        
        SocialConnection SocialConnection = this.SocialConnectionRepository
            .findByUserIdAndProviderIdAndProviderUserId(
                    userId, connectionKey.getProviderId(), connectionKey.getProviderUserId());

        this.SocialConnectionRepository.delete(SocialConnection);
    }

    private Sort sortByProviderId() {
        return new Sort(Sort.Direction.ASC, "providerId");
    }

    private Sort sortByCreated() {
        return new Sort(Sort.Direction.DESC, "created");
    }

    // internal helpers

    private Connection<?> buildConnection(SocialConnection SocialConnection) {
        
        ConnectionData connectionData = new ConnectionData(
            SocialConnection.getProviderId(),
            SocialConnection.getProviderUserId(), 
            SocialConnection.getDisplayName(),
            SocialConnection.getProfileUrl(), 
            SocialConnection.getImageUrl(),
            decrypt(SocialConnection.getAccessToken()), 
            decrypt(SocialConnection.getSecret()),
            decrypt(SocialConnection.getRefreshToken()), 
            SocialConnection.getExpireTime() );
        
        ConnectionFactory<?> connectionFactory = this.socialAuthenticationServiceLocator
            .getConnectionFactory(connectionData.getProviderId());

        return connectionFactory.createConnection(connectionData);
    }

    private Connection<?> findPrimaryConnection(String providerId) {
        
        List<SocialConnection> SocialConnectionList = this.SocialConnectionRepository
            .findByUserIdAndProviderId(userId, providerId);

        return buildConnection(SocialConnectionList.get(0));
    }

    private <A> String getProviderId(Class<A> apiType) {
        return socialAuthenticationServiceLocator.getConnectionFactory(apiType).getProviderId();
    }

    private String encrypt(String text) {
        return text != null ? textEncryptor.encrypt(text) : text;
    }

    private String decrypt(String encryptedText) {
        return encryptedText != null ? textEncryptor.decrypt(encryptedText) : encryptedText;
    }
}
