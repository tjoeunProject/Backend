package com.example.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.service.PlaceService;
import com.example.project.route.domain.Route;
import com.example.project.route.repository.RouteRepository;
import org.hashids.Hashids;
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
class OwnershipApiTests {
    @Autowired MockMvc mvc;
    @Autowired TravelUserRepository users;
    @Autowired RouteRepository routes;
    @Autowired Hashids hashids;
    @Autowired com.example.project.member.repository.MemberLikeRouteRepository likes;
    @MockBean PlaceService placeService;

    TravelUser member(String name) {
        return users.saveAndFlush(TravelUser.builder().email(name+"@example.invalid")
                .nickname(name).password("fixture-only").build());
    }

    Route route(TravelUser owner) {
        Route route = new Route();
        route.setUser(owner); route.setTitle("original"); route.setTotalDays(1);
        return routes.saveAndFlush(route);
    }

    @Test
    void anotherUserCannotDeleteARoute() throws Exception {
        TravelUser owner=member("owner"), other=member("other");
        Route saved=route(owner);
        mvc.perform(delete("/api/route/"+hashids.encode(saved.getId())).with(user(other.getEmail())))
                .andExpect(status().isForbidden());
        assertThat(routes.existsById(saved.getId())).isTrue();
    }

    @Test
    void anotherUserCannotCreateAnOwnersRoute() throws Exception {
        TravelUser owner=member("owner"), other=member("other");
        mvc.perform(post("/api/route").with(user(other.getEmail())).contentType("application/json")
                .content("{\"memberId\":"+owner.getId()+",\"title\":\"forged\",\"places\":[[]]}"))
                .andExpect(status().isForbidden());
        assertThat(routes.count()).isZero();
        org.mockito.Mockito.verifyNoInteractions(placeService);
    }

    @Test
    void anotherUserCannotUpdateAnOwnersRoute() throws Exception {
        TravelUser owner=member("owner"), other=member("other");
        Route saved=route(owner);
        mvc.perform(put("/api/route/"+hashids.encode(saved.getId())).with(user(other.getEmail()))
                .contentType("application/json").content("{\"title\":\"forged\",\"places\":[]}"))
                .andExpect(status().isForbidden());
        assertThat(routes.findById(saved.getId()).orElseThrow().getTitle()).isEqualTo("original");
        org.mockito.Mockito.verifyNoInteractions(placeService);
    }

    @Test
    void likesCannotBeReadAsAnotherUser() throws Exception {
        TravelUser owner=member("owner"), other=member("other");
        mvc.perform(get("/api/v1/users/"+owner.getId()+"/likes").with(user(other.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileResponseDoesNotExposeCredentials() throws Exception {
        TravelUser owner=member("owner");
        mvc.perform(put("/api/user/profile").with(user(owner.getEmail())).contentType("application/json")
                .content("{\"nickname\":\"updated\",\"age\":30,\"gender\":\"M\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("updated"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.tokens").doesNotExist())
                .andExpect(jsonPath("$.routes").doesNotExist());
    }

    @Test
    void ownerCanCreateAndDeleteTheirRoute() throws Exception {
        TravelUser owner=member("owner");
        var response=mvc.perform(post("/api/route").with(user(owner.getEmail())).contentType("application/json")
                .content("{\"memberId\":"+owner.getId()+",\"title\":\"mine\",\"places\":[[]]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long id=Long.valueOf(response);
        assertThat(routes.findById(id).orElseThrow().getUser().getId()).isEqualTo(owner.getId());
        mvc.perform(delete("/api/route/"+hashids.encode(id)).with(user(owner.getEmail())))
                .andExpect(status().isOk());
        assertThat(routes.existsById(id)).isFalse();
    }

    @Test
    void likeMutationsAndResponsesRespectOwnership() throws Exception {
        TravelUser owner=member("owner"), other=member("other"); Route route=route(owner);
        String path="/api/v1/users/"+owner.getId()+"/routes/"+hashids.encode(route.getId())+"/likes";
        mvc.perform(post(path).with(user(other.getEmail()))).andExpect(status().isForbidden());
        assertThat(likes.count()).isZero();
        mvc.perform(post(path).with(user(owner.getEmail()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(route.getId()))
                .andExpect(jsonPath("$.user").doesNotExist()).andExpect(jsonPath("$.route").doesNotExist());
        mvc.perform(delete(path).with(user(other.getEmail()))).andExpect(status().isForbidden());
        assertThat(likes.count()).isEqualTo(1);
        mvc.perform(get("/api/user/likes").with(user(owner.getEmail()))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(route.getId()))
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }
}
