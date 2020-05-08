package io.github.tral909.spring.boot.validation;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;

public interface PersonDao extends CrudRepository<Person, Long> {
	@Nullable
	Person findByInn(String inn);
}
