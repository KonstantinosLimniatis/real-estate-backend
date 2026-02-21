package gr.hua.dit.ds.ds_2025.services;

import gr.hua.dit.ds.ds_2025.entities.Role;
import gr.hua.dit.ds.ds_2025.entities.User;
import gr.hua.dit.ds.ds_2025.repositories.RoleRepository;
import gr.hua.dit.ds.ds_2025.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {


    private UserRepository userRepository; // Ένα αντικείμενο τύπου UserRepository

    private RoleRepository roleRepository; // Ένα αντικείμενο τύπου RoleRepository

    private BCryptPasswordEncoder passwordEncoder; // Ένα αντικείμενο τύπου BCryptPasswordEncoder για την κρυπτογράφηση του password

    private EmailService emailService; // Υπηρεσία αποστολής email

    public UserService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, EmailService emailService) { // Constructor της κλάσης
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public Integer saveUser(User user) { // Μέθοδος για την αποθήκευση ενός χρήστη
        String passwd= user.getPassword();
        String encodedPassword = passwordEncoder.encode(passwd);
        user.setPassword(encodedPassword);

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        user = userRepository.save(user);

        // Στείλε email επιτυχούς εγγραφής (best-effort)
        try {
            emailService.sendRegistrationSuccessEmail(user);
        } catch (Exception e) {
            System.out.println("Registration email failed (ignored): " + e.getMessage());
        }

        return user.getId();
    }

    @Transactional
    public Integer updateUser(User user) { // Μέθοδος που ενημερώνει τα στοιχεία του χρήστη
        user = userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // Μέθοδος που φορτώνει έναν χρήστη με βάση το username του
        Optional<User> opt = userRepository.findByUsernameOrEmail(username, username);

        if(opt.isEmpty())
            throw new UsernameNotFoundException("User with email: " +username +" not found !");
        else {
            User user = opt.get();
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    user.getRoles()
                            .stream()
                            .map(role-> new SimpleGrantedAuthority(role.toString()))
                            .collect(Collectors.toSet())
            );
        }
    }

    @Transactional
    public Object getUsers() { // Μέθοδος που επιστρέφει όλους τους χρήστες
        return userRepository.findAll();
    }

    public User getUser(Integer userId) { // Μέθοδος που επιστρέφει έναν χρήστη με βάση το id
        return userRepository.findById(userId).get();
    }

    @Transactional
    public void updateOrInsertRole(Role role) { // Μέθοδος που ενημερώνει έναν ρόλο
        roleRepository.updateOrInsert(role);
    }

    @Transactional
    public Integer getCurrentUserId() { // Μέθοδος που επιστρέφει το id του συνδεδεμένου χρήστη
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()  || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User is not authenticated.");
        }
        User user = (User) userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + authentication.getName()));
        return user.getId();
    }
}
