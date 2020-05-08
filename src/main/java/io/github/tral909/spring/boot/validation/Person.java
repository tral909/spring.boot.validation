package io.github.tral909.spring.boot.validation;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static javax.persistence.GenerationType.IDENTITY;

@Getter
@Setter
@Entity
public class Person {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	Long id;

	Integer age;

	String name;

	String inn;
}
