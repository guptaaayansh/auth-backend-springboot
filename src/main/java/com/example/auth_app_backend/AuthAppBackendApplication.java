package com.example.auth_app_backend;

import com.example.auth_app_backend.config.AppConstants;
import com.example.auth_app_backend.entities.Role;
import com.example.auth_app_backend.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class AuthAppBackendApplication implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;

	public static void main(String[] args) {
		SpringApplication.run(AuthAppBackendApplication.class, args);
	}


	@Override
	public void run(String... args) throws Exception {

		//we will create some default user role
		//ADMIN
		//GUEST

		roleRepository.findByName("ROLE_"+ AppConstants.ADMIN_ROLE).ifPresentOrElse(role->{
			System.out.println("Admin Role Already Exists: "+role.getName());
		},()->{

			Role role=new Role();
			role.setName("ROLE_"+AppConstants.ADMIN_ROLE);
			role.setId(UUID.randomUUID());
			roleRepository.save(role);

		});

		roleRepository.findByName("ROLE_"+AppConstants.GUEST_ROLE).ifPresentOrElse(role->{
			System.out.println("Guest Role Already Exists: "+role.getName());
		},()->{

			Role role=new Role();
			role.setName("ROLE_"+AppConstants.GUEST_ROLE);
			role.setId(UUID.randomUUID());
			roleRepository.save(role);

		});



	}

}
