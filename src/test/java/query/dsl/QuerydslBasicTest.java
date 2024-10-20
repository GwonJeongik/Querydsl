package query.dsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.hibernate.dialect.TiDBDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import query.dsl.entity.Member;
import query.dsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static query.dsl.entity.QMember.member;

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
        //when
        Member findMember = query
                .selectFrom(member)
                .where(member.name.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    @DisplayName(value = "Querydsl 검색 조건 설정!")
    void search() {
        //when
        Member findMember = query
                .selectFrom(member)
                .where(
                        member.name.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void fetchResult() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> fetchResults = query
                .selectFrom(member)
                .fetchResults();

        long total = fetchResults.getTotal();

        List<Member> content = fetchResults.getResults();

        long totalCount = query
                .selectFrom(member)
                .fetchCount();
    }

}
