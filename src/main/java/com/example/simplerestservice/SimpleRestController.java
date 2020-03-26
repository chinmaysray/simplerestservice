package com.example.simplerestservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class SimpleRestController {

    @PostMapping("/add")
    public ResponseEntity<String> add(@Valid @RequestParam String x, @Valid @RequestParam String y) {
        String result =  String.valueOf(Integer.parseInt(x) + Integer.parseInt(y));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/diff")
    public ResponseEntity<String> diff(@Valid @RequestParam String x, @Valid @RequestParam String y) {
        String result =  String.valueOf( Integer.parseInt(x) - Integer.parseInt(y));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
