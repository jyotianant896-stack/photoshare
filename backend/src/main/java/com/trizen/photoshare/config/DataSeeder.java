package com.trizen.photoshare.config;

import com.trizen.photoshare.entity.Event;
import com.trizen.photoshare.entity.EventMember;
import com.trizen.photoshare.entity.Role;
import com.trizen.photoshare.entity.User;
import com.trizen.photoshare.repository.EventMemberRepository;
import com.trizen.photoshare.repository.EventRepository;
import com.trizen.photoshare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Creates the demo accounts the reviewer needs on a fresh database.
 * Turn it off with app.demo.seed=false.
 */
@Component
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public DataSeeder(UserRepository userRepository,
                      EventRepository eventRepository,
                      EventMemberRepository eventMemberRepository,
                      PasswordEncoder passwordEncoder,
                      AppProperties properties) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        AppProperties.Demo demo = properties.getDemo();
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(new User("Priya Nair", demo.getAdminEmail(),
                passwordEncoder.encode(demo.getAdminPassword()), Role.ADMIN));
        User member = userRepository.save(new User("Arjun Rao", demo.getMemberEmail(),
                passwordEncoder.encode(demo.getMemberPassword()), Role.TEAM_MEMBER));

        Event event = eventRepository.save(new Event("Arjun & Priya Wedding",
                "Two day celebration at Bengaluru Palace Grounds.",
                LocalDate.now().minusDays(7), admin));
        eventMemberRepository.save(new EventMember(event, member));

        log.info("Seeded demo data: admin {} / team member {}",
                demo.getAdminEmail(), demo.getMemberEmail());
    }
}
