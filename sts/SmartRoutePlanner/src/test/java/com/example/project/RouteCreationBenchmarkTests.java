package com.example.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RouteCreationBenchmarkTests {
    @Autowired MockMvc mvc;
    @Autowired TravelUserRepository users;
    @MockBean PlaceService placeService;

    @Test
    void measureValidOwnerCreation() throws Exception {
        var owner=users.saveAndFlush(TravelUser.builder().email("bench@example.invalid")
                .nickname("bench").password("fixture-only").build());
        double[] samples=new double[7];
        for (int batch=0;batch<10;batch++) {
            long start=System.nanoTime();
            for (int i=0;i<20;i++) {
                var response=mvc.perform(post("/api/route").with(user(owner.getEmail()))
                        .contentType("application/json")
                        .content("{\"memberId\":"+owner.getId()+",\"title\":\"bench\",\"places\":[[]]}"))
                        .andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(Long.parseLong(response.getContentAsString())).isPositive();
            }
            if(batch>=3) samples[batch-3]=(System.nanoTime()-start)/1_000_000.0/20;
        }
        org.mockito.Mockito.verifyNoInteractions(placeService);
        System.out.println("ROUTEPICK_CREATE_MS="+java.util.Arrays.toString(samples));
    }
}
