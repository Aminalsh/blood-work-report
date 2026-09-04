package io.everyonecodes.spring_module.global_exception_handling;

public class ResourceNotFoundException extends RuntimeException{


    public ResourceNotFoundException(String message) {
        super(message);
    }


}
