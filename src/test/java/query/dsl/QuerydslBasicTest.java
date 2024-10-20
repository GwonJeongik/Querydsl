package query.dsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import query.dsl.entity.Member;
import query.dsl.entity.QMember;
import query.dsl.entity.Team;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    public void before() {
        query = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName(value = "JPQL로 특정 회원을 찾아라!")
    void startJpql() {
        //when
        Member findMember = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    @DisplayName(value = "Querydsl로 특정 회원을 찾아라!")
    void startQuerydsl() {
        //given
        QMember m = QMember.member;

        //when
        Member findMember = query.selectFrom(m)
                .where(m.name.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
    }
}
