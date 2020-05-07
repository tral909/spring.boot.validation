package io.github.tral909.spring.boot.validation;

import lombok.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
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

import javax.validation.Valid;
import javax.validation.constraints.Max;
import java.util.Optional;
import java.util.stream.Collectors;

// тестирование через CURL
// curl -s -X POST localhost:8888/hello -H 'content-type: application/json;charset=utf-8' -d '{"number": 11, "data": "rrrr"}' | json_pp
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

@Value
class InputData {
	@Max(value = 10, message = "Значение поля 'number' должно быть меньше или равно 10")
	Integer number;
	String data;
}

@RestController
class HelloController {

	@InitBinder // нужен для валидации через Validator
	protected void initBinder(WebDataBinder binder) {
		// если использовать setValidator(..) то дефолтный hibernate-validator затрется реализацией Validator'a
		binder.addValidators(new InputDataValidator());
	}

	@PostMapping("hello")
	public InputData hello(@RequestBody @Valid InputData inputData) {
		return inputData;
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

@Value
class ErrorResponse {
	int code;
	String desc;
	String message;
}

// При валидации через Validator нужен кастомный компонент, который реализует интерфейс Validator
@Component
class InputDataValidator implements Validator {
	@Override
	public boolean supports(Class<?> aClass) {
		return aClass.isAssignableFrom(InputData.class);
	}

	@Override
	public void validate(@NonNull Object o, Errors errors) {
		InputData dto = (InputData) o;

		if (dto.getData() != null && dto.getData().length() > 3) {
			errors.reject("Длина поля 'data' не может быть больше 3 символов");
		}
	}
}
