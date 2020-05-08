package io.github.tral909.spring.boot.validation;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueInnConstraintValidator implements ConstraintValidator<UniqueInn, String> {
	@Autowired
	private PersonDao personDao;

	@Override
	public void initialize(UniqueInn constraintAnnotation) {

	}

	@Override
	public boolean isValid(String inn, ConstraintValidatorContext context) {
		if (personDao.findByInn(inn) != null) {
			context.disableDefaultConstraintViolation();
			// здесь можно указать шаблон для интернационализации
			// https://urvanov.ru/2017/11/01/spring-validation-custom-constraint-validator/
			context.buildConstraintViolationWithTemplate("Такой ИНН уже существует").addConstraintViolation();
			return false;
		}
		return true;
	}
}
