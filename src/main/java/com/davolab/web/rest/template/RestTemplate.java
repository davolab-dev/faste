package com.davolab.web.rest.template;

import com.davolab.web.rest.template.dto.ApiResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

public interface RestTemplate<MD, ME, RD, ID extends Serializable> {

    @PostMapping
    ResponseEntity<ApiResponseDto> create(@RequestBody MD mainDto);

    @GetMapping("/all")
    ResponseEntity<ApiResponseDto> findAll();

    @GetMapping("/{id}")
    ResponseEntity<ApiResponseDto> findById(@PathVariable("id") ID pk);

    @GetMapping("/page")
    ResponseEntity<ApiResponseDto> paginate(@RequestParam(value = "page", defaultValue = "0") ID page, @RequestParam(value = "size", defaultValue = "0") ID size);

    @PutMapping
    ResponseEntity<ApiResponseDto> update(@RequestBody MD mainDto);

    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponseDto> delete(@PathVariable("id") ID pk);
}
