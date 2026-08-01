package com.impress.server.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // 성공 시 데이터를 담아 반환하는 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공적으로 처리되었습니다.", data);
    }

    // 성공했지만 전달할 데이터가 없는 경우 (예: 방 나가기 DELETE 요청 등)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "성공적으로 처리되었습니다.", null);
    }

    // 실패 시 에러 메시지를 반환하는 정적 팩토리 메서드
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}