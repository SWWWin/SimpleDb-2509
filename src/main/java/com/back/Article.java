package com.back;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class Article {
    private Long Id;
    private String Title;
    private String Body;
    private LocalDateTime CreatedDate;
    private LocalDateTime ModifiedDate;
    private boolean isBlind;

    public Article() {}

}
