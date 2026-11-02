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
class RouteTransactionTests {
 @Autowired RouteService service;
 @Autowired TravelUserRepository users;
 @Autowired RouteRepository routes;
 @Autowired RoutePlaceRepository links;
 @Autowired PlaceRepository places;
 @MockBean GooglePlacesClient google;
 @org.springframework.boot.test.mock.mockito.SpyBean com.example.project.place.service.PlaceService placeService;
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
 @Test void providerIsCalledOutsideDatabaseTransaction() {
   when(google.fetchPlaceDetails("a")).thenAnswer(invocation->{
     assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
     assertThat(routes.count()).isZero();return detail("a");
   });
   long id=service.createRoute(request("a","a"));
   assertThat(routes.existsById(id)).isTrue();assertThat(links.count()).isEqualTo(2);
   verify(google,times(1)).fetchPlaceDetails("a");
 }
 @Test void providerFailureLeavesCreateUnchanged() {
   when(google.fetchPlaceDetails("a")).thenReturn(detail("a"));
   when(google.fetchPlaceDetails("b")).thenThrow(new IllegalStateException("fixture provider failure"));
   assertThatThrownBy(()->service.createRoute(request("a","b"))).isInstanceOf(IllegalStateException.class);
   assertThat(routes.count()).isZero();assertThat(places.count()).isZero();assertThat(links.count()).isZero();
 }
 @Test void providerFailureLeavesExistingRouteUnchanged() {
   when(google.fetchPlaceDetails("a")).thenReturn(detail("a"));
   long id=service.createRoute(request("a"));
   when(google.fetchPlaceDetails("b")).thenThrow(new IllegalStateException("fixture provider failure"));
   var change=request("b");change.setTitle("changed");
   assertThatThrownBy(()->service.updateRoute(id,change)).isInstanceOf(IllegalStateException.class);
   assertThat(routes.findById(id).orElseThrow().getTitle()).isEqualTo("fixture");assertThat(links.count()).isEqualTo(1);
 }
 @Test void databaseFailureRollsBackAllPreparedPlacesAndRoute() {
   when(google.fetchPlaceDetails("a")).thenReturn(detail("a"));
   when(google.fetchPlaceDetails("b")).thenReturn(detail("b"));
   doThrow(new IllegalStateException("fixture persistence failure")).when(placeService).savePreparedPlace(eq("b"),any());
   assertThatThrownBy(()->service.createRoute(request("a","b"))).isInstanceOf(IllegalStateException.class);
   assertThat(routes.count()).isZero();assertThat(places.count()).isZero();assertThat(links.count()).isZero();
 }
}
