package com.example.project.global.hashid;

import org.hashids.Hashids;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HashidToLongConverter implements Converter<String, Long> {

    private final Hashids hashids;

    public HashidToLongConverter(Hashids hashids) {
        this.hashids = hashids;
    }

    @Override
    public Long convert(String source) {
        try {
            long[] decoded = hashids.decode(source);
            if (decoded.length > 0) {
                System.out.println("✅ 복호화 성공: " + source + " -> " + decoded[0]);
                return decoded[0];
            } else {
                System.out.println("❌ 복호화 실패(빈 배열): " + source);
            }
        } catch (Exception e) {
            System.out.println("❌ 복호화 중 에러: " + e.getMessage());
        }
        return null; // 여기서 null을 주면 컨트롤러가 Bad Request를 냅니다.
    }
}
