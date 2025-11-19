package com.todolab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ToDoALabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToDoALabApplication.class, args);
        log.info("==================================" +
                 "  To Do Lab Application           " +
                 "==================================");
    }
}
