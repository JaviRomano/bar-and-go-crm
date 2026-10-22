package com.barandgo.crm.exception;

/**
 * La operación choca con el estado actual de los datos (duplicado, registro en uso...). Se traduce a 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
