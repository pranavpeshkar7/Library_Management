package com.pranav.library.service;

import com.pranav.library.exceptions.*;
import com.pranav.library.model.*;
import com.pranav.library.repository.UserRepository;
import com.pranav.library.util.PasswordUtil;

import java.util.UUID;

public class AuthService {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public AuthService(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public User signup(String name, String email, String password, Role role) throws Exception {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException(email);
        }
        String id = UUID.randomUUID().toString();
        String passwordHash = PasswordUtil.hash(password);

        User user;
        switch (role) {
            case STUDENT: user = new Student(id, name, email, passwordHash); break;
            case TEACHER: user = new Teacher(id, name, email, passwordHash); break;
            case LIBRARIAN: user = new Librarian(id, name, email, passwordHash); break;
            default: throw new IllegalArgumentException("Unknown role: " + role);
        }
        return userRepository.save(user);
    }

    /** Returns a session token on success. */
    public String login(String email, String password) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!userRepository.isActiveById(user.getId())) {
            throw new AuthenticationException("This account has been deactivated by the librarian");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return sessionManager.createSession(user.getId());
    }

    public User requireUser(String token) throws Exception {
        String userId = sessionManager.getUserId(token);
        if (userId == null) throw new AuthenticationException("Missing or invalid session token");
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Session refers to a user that no longer exists"));
    }

    public void requireLibrarian(User user) throws AuthorizationException {
        if (!user.canManageLibrary()) {
            throw new AuthorizationException("Only a librarian can perform this action");
        }
    }

    public void logout(String token) {
        sessionManager.invalidate(token);
    }
}
