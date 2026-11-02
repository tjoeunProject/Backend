package com.example.project;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.domain.Place;
import com.example.project.place.repository.PlaceRepository;
import com.example.project.route.domain.Route;
import com.example.project.route.domain.RoutePlace;
import com.example.project.route.repository.RouteRepository;
import com.example.project.route.repository.RoutePlaceRepository;
import com.example.project.route.service.RouteService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class RouteListQueryTests {
    @Autowired TravelUserRepository users;
    @Autowired RouteRepository routes;
    @Autowired RoutePlaceRepository links;
    @Autowired PlaceRepository places;
    @Autowired RouteService service;
    @Autowired EntityManager em;

    @Test
    void listingTwelveRoutesDoesNotQueryEachFirstPlaceSeparately() {
        var user=users.save(TravelUser.builder().email("query@example.invalid").password("fixture").build());
        for(int i=0;i<12;i++) {
            Route route=new Route();route.setUser(user);route.setTitle("route-"+i);route.setTotalDays(1);routes.save(route);
            Place place=new Place();place.setName("first-"+i);place.setPhotoReferences(List.of("photo-"+i));places.save(place);
            RoutePlace link=new RoutePlace();link.setRoute(route);link.setPlace(place);link.setDayIndex(1);link.setOrderIndex(1);links.save(link);
        }
        em.flush();em.clear();
        var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();stats.clear();
        long start=System.nanoTime();
        var result=service.getRoutesByMember(user.getId().longValue());
        double elapsed=(System.nanoTime()-start)/1_000_000.0;
        long queries=stats.getPrepareStatementCount();
        System.out.println("ROUTEPICK_LIST_QUERIES="+queries+";MS="+elapsed);
        assertThat(result).hasSize(12);
        assertThat(result).allSatisfy(row -> {
            assertThat(row.getMainPlaceName()).startsWith("first-");
            assertThat(row.getPhotoUrl()).startsWith("photo-");
        });
        assertThat(queries).isLessThanOrEqualTo(2);
    }
}
