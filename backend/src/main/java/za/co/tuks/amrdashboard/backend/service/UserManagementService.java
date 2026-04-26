package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import za.co.tuks.amrdashboard.backend.dto.CognitoUserDTO;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    @Value("${cognito.admin-group}")
    private String adminGroup;

    public List<CognitoUserDTO> listUsers() {
        Set<String> adminUsernames = listAdminUsernames();

        return cognitoClient.listUsers(ListUsersRequest.builder()
                        .userPoolId(userPoolId)
                        .build())
                .users()
                .stream()
                .map(u -> toDTO(u, adminUsernames))
                .toList();
    }

    public void addToAdmins(String username) {
        cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .groupName(adminGroup)
                .build());
    }

    public void removeFromAdmins(String username) {
        cognitoClient.adminRemoveUserFromGroup(AdminRemoveUserFromGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .groupName(adminGroup)
                .build());
    }

    private Set<String> listAdminUsernames() {
        return cognitoClient.listUsersInGroup(ListUsersInGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .groupName(adminGroup)
                        .build())
                .users()
                .stream()
                .map(UserType::username)
                .collect(Collectors.toSet());
    }

    private CognitoUserDTO toDTO(UserType user, Set<String> adminUsernames) {
        String email = user.attributes().stream()
                .filter(a -> a.name().equals("email"))
                .map(AttributeType::value)
                .findFirst()
                .orElse("");
        return new CognitoUserDTO(user.username(), email, adminUsernames.contains(user.username()));
    }
}
