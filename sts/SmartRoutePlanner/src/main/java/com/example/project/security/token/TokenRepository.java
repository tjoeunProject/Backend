package com.example.project.security.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRepository extends JpaRepository<Token, Integer> {

  @Query(value = """
      select t from Token t inner join TravelUser u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false and t.revoked = false)\s
      """)
  List<Token> findAllValidTokenByUser(@Param("id") Integer id);
  
  Optional<Token> findByToken(String token);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Token t where t.token = :token")
  Optional<Token> findForUpdateByToken(@Param("token") String token);
}
