package com.roberthj.project.healthcare.framework.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtils {

    public static final ObjectMapper COMMON_MAPPER = new ObjectMapper();

}
