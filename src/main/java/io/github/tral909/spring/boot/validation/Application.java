package io.github.tral909.spring.boot.validation;

import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// тестирование через CURL
// curl -s -X POST localhost:8888/hello -H 'content-type: application/json;charset=utf-8' -d '{"age": 11, "name": "rrrr", "inn": "12345"}' | json_pp
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

@Value
class PersonDto {
	@Max(value = 10, message = "Значение поля 'age' должно быть меньше или равно 10")
	Integer age;
	String name;
	@UniqueInn
	String inn;
}

@RestController
class HelloController {

	@PostMapping("hello")
	public PersonDto hello(@RequestBody @Valid PersonDto personDto) {
		return personDto;
	}

}

@Value
class CountDto {
	Integer number;

	@ConstructorProperties({ "number" }) //или над конструктором @JsonCreator и в параметре @JsonProperty("number")
	public CountDto(Integer number) {
		this.number = number;
	}
}

@RestController
class CountController {

	@PostMapping("count") // @Valid отсутствует, чтобы проверить BPP
	public CountDto count(@RequestBody CountDto countDto) { return countDto; }

}

@Value
class ErrorResponse {
	int code;
	String desc;
	String message;
}

// Нам нужен свой интерфейс для валидаторов с методом, который будет возвращаеть целевой класс для валидации - Class<?> getRequestBodyClass()
// Это необходимо, чтобы правильно зарегистрировать класс DTO в реестре реализаций валидаторов
interface MyValidator extends Validator {
	Class<?> getRequestBodyClass();

	@Override
	default boolean supports(Class<?> clazz) {
		Class<?> bodyClass = getRequestBodyClass();
		Assert.notNull(bodyClass, "bodyClass is null");

		return bodyClass.isAssignableFrom(clazz);
	}
}

// При валидации через Validator нужен компонент, который реализует интерфейс Validator
@Component
class PersonDtoValidator implements MyValidator {
	@Override
	public Class<?> getRequestBodyClass() {
		return PersonDto.class;
	}

	@Override
	public void validate(Object o, Errors errors) {
		PersonDto dto = (PersonDto) o;

		if (dto.getName() != null && dto.getName().length() > 3) {
			errors.reject("Длина поля 'name' не может быть больше 3 символов");
		}
	}
}

@Component
class CountDtoValidator implements MyValidator {
	@Override
	public Class<?> getRequestBodyClass() {
		return CountDto.class;
	}

	@Override
	public void validate(Object o, Errors errors) {
		CountDto dto = (CountDto) o;

		if (dto.getNumber() != null && dto.getNumber() > 2) {
			errors.reject("Поле 'number' не может быть больше 2");
		}
	}
}

@RestControllerAdvice
class CommonControllerAdvice {

	// Реестр реализаций валидаторов - валидируемый класс против интерфейса валидатора
	// Нужны именно конкретные валидаторы (реестр через Class). Через инжекцию List<MyValidator> не работает
	private Map<Class, MyValidator> validatorsRegistry;

	@Autowired
	public CommonControllerAdvice(List<MyValidator> validators) {
		validatorsRegistry = validators.stream().collect(Collectors.toMap(k -> k.getRequestBodyClass(), Function.identity()));
	}

	@InitBinder // нужен для валидации через Validator
	public void initBinder(WebDataBinder binder) {
		Class<?> targetClass = Optional.ofNullable(binder.getTarget()).map(Object::getClass).orElse(null);
		if (targetClass == null) { // the target object to bind onto is 'null' if the binder is just used to convert a plain parameter value
			return;
		}
		System.out.println("In binder! validators: " + validatorsRegistry);

		MyValidator myValidator = validatorsRegistry.get(targetClass);
		Assert.notNull(myValidator, String.format("No validator configured for [%s]", targetClass.getName()));

		// если использовать setValidator(..) то дефолтный hibernate-validator затрется реализацией Validator'a
		binder.addValidators(myValidator);
	}

	// используется для перехвата ошибок и возвращения кастомной модели ошибки с понятными сообщениями
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ErrorResponse MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getAllErrors().stream()
				.map(e -> Optional.ofNullable(e.getDefaultMessage()) //message в javax.validation.constraints.* аннотациях это defaultMessage
						.orElse(e.getCode())) //code передается при одном параметре в errors.reject(Str) (defaultMessage = null)
				.collect(Collectors.joining(";"));
		return new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message);
	}
}
