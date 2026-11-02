package com.example.project;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.repository.PlaceRepository;
import com.example.project.place.service.GooglePlacesClient;
import com.example.project.route.dto.RouteCreateRequestDto;
import com.example.project.route.repository.RouteRepository;
import com.example.project.route.repository.RoutePlaceRepository;
import com.example.project.route.service.RouteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test")
class TransactionHoldBenchmarkTests {
 @Autowired RouteService service;
 @Autowired TravelUserRepository users;
 @Autowired RouteRepository routes;
 @Autowired RoutePlaceRepository links;
 @Autowired PlaceRepository places;
 @MockBean GooglePlacesClient google;

 @BeforeEach void login() {
   var user=users.save(TravelUser.builder().email("transaction@example.invalid").password("fixture").build());
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(),"fixture",List.of()));
 }
 @AfterEach void cleanup() { SecurityContextHolder.clearContext();links.deleteAll();routes.deleteAll();places.deleteAll();users.deleteAll(); }
 RouteCreateRequestDto request(String... ids) {
   var dto=new RouteCreateRequestDto();dto.setTitle("fixture");
   dto.setPlaces(List.of(java.util.Arrays.stream(ids).map(id->{var p=new RouteCreateRequestDto.SimplePlaceDto();p.setPlaceId(id);return p;}).toList()));return dto;
 }
 PlaceRequestDto detail(String id) {
   var dto=new PlaceRequestDto();dto.setGooglePlaceId(id);dto.setName(id);dto.setLat(37);dto.setLng(127);return dto;
 }
 @Test void measureProviderTimeInsideTransaction() {
   java.util.List<Double> total=new java.util.ArrayList<>(),held=new java.util.ArrayList<>();
   when(google.fetchPlaceDetails(anyString())).thenAnswer(invocation->{
     boolean inTransaction=TransactionSynchronizationManager.isActualTransactionActive();
     long start=System.nanoTime();Thread.sleep(20);
     held.add(inTransaction ? (System.nanoTime()-start)/1_000_000.0 : 0.0);
     return detail(invocation.getArgument(0));
   });
   for(int i=0;i<7;i++) {
     long start=System.nanoTime();
     long id=service.createRoute(request("bench-"+i));
     assertThat(routes.existsById(id)).isTrue();
     total.add((System.nanoTime()-start)/1_000_000.0);
   }
   assertThat(links.count()).isEqualTo(7);
   System.out.println("TX_TOTAL_MS="+total.subList(2,7));
   System.out.println("TX_PROVIDER_HELD_MS="+held.subList(2,7));
 }
}
